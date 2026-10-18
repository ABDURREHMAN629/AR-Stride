package com.stride.fit.sensors

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** What the body is doing, judged from raw accelerometer motion. */
enum class MotionClass {
    STILL,
    WALKING,
    RUNNING,
    CYCLING,
    VEHICLE,
    UNKNOWN;

    /** Only these two ever earn a step. */
    val isPedestrian: Boolean get() = this == WALKING || this == RUNNING

    val label: String
        get() = when (this) {
            STILL -> "Still"
            WALKING -> "Walking"
            RUNNING -> "Running"
            CYCLING -> "Cycling"
            VEHICLE -> "In vehicle"
            UNKNOWN -> "Unclear"
        }
}

/** How eager the gate is to credit a step. */
enum class Sensitivity(val label: String, val blurb: String) {
    LENIENT("Lenient", "Counts more steps. Some cycling may slip through."),
    BALANCED("Balanced", "Recommended. Rejects riding, keeps normal walking."),
    STRICT("Strict", "Rejects anything ambiguous. May miss slow shuffling.")
}

/** Features of one analysis window. Shown in the app so decisions can be inspected. */
data class MotionFeatures(
    val sd: Double,             // std-dev of gravity-removed magnitude (m/s^2)
    val p2p: Double,            // robust peak-to-peak (m/s^2)
    val kurtosis: Double,       // impulsiveness: heel strikes spike, pedalling does not
    val dominantHz: Double,     // strongest repetition rate
    val periodicity: Double,    // 0..1 autocorrelation strength at that rate
    val sampleHz: Double,
    val samples: Int
) {
    companion object {
        val EMPTY = MotionFeatures(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0)
    }

    val cadenceSpm: Double get() = dominantHz * 60.0
}

/**
 * Sliding-window motion classifier: feed it accelerometer magnitudes, ask once a
 * second. Four independent signals decide, and cycling defeats any one of them but
 * not all four: amplitude (sd, p2p), rhythm (dominantHz), regularity (periodicity)
 * and impact shape (kurtosis).
 */
class MotionClassifier(
    private val windowSeconds: Double = 4.0,
    var sensitivity: Sensitivity = Sensitivity.BALANCED
) {

    private val capacity = (TARGET_HZ * 6).toInt()   // 6 s of headroom for a 4 s window
    private val mag = DoubleArray(capacity)
    private val ts = LongArray(capacity)
    private var head = 0
    private var count = 0
    private var lastAcceptedMs = 0L

    fun reset() {
        head = 0
        count = 0
        lastAcceptedMs = 0L
    }

    fun addSample(x: Float, y: Float, z: Float, timestampMs: Long) {
        // Human gait tops out near 4 Hz, so 55 Hz is still heavily oversampled.
        if (timestampMs - lastAcceptedMs < MIN_SAMPLE_GAP_MS) return
        lastAcceptedMs = timestampMs

        val m = sqrt((x * x + y * y + z * z).toDouble())
        mag[head] = m
        ts[head] = timestampMs
        head = (head + 1) % capacity
        if (count < capacity) count++
        trimToWindow(timestampMs)
    }

    /** Drop samples older than the window so a stalled sensor cannot poison the buffer. */
    private fun trimToWindow(nowMs: Long) {
        val cutoff = nowMs - (windowSeconds * 1000).toLong()
        while (count > 0) {
            val oldestIdx = (head - count + capacity) % capacity
            if (ts[oldestIdx] < cutoff) count-- else break
        }
    }

    /** Ordered oldest-to-newest copy of the live window. */
    private fun snapshot(): DoubleArray {
        val out = DoubleArray(count)
        val start = (head - count + capacity) % capacity
        for (i in 0 until count) out[i] = mag[(start + i) % capacity]
        return out
    }

    private fun spanMs(): Long {
        if (count < 2) return 0
        val start = (head - count + capacity) % capacity
        val end = (head - 1 + capacity) % capacity
        return ts[end] - ts[start]
    }

    fun features(): MotionFeatures {
        if (count < 24) return MotionFeatures.EMPTY
        val span = spanMs()
        if (span < 1500) return MotionFeatures.EMPTY

        val raw = snapshot()
        val n = raw.size
        val sampleHz = (n - 1) * 1000.0 / span

        // Subtracting the window mean removes gravity: a cheap high-pass filter.
        var mean = 0.0
        for (v in raw) mean += v
        mean /= n
        val dyn = DoubleArray(n) { raw[it] - mean }

        var m2 = 0.0
        var m4 = 0.0
        for (v in dyn) {
            val s = v * v
            m2 += s
            m4 += s * s
        }
        m2 /= n
        m4 /= n
        val sd = sqrt(m2)
        val kurtosis = if (m2 > 1e-9) m4 / (m2 * m2) else 0.0

        val sorted = dyn.clone().also { it.sort() }
        val lo = sorted[(n * 0.025).toInt().coerceIn(0, n - 1)]
        val hi = sorted[(n * 0.975).toInt().coerceIn(0, n - 1)]
        val p2p = hi - lo

        // Skipped when motionless: correlating sensor noise returns a fictional rhythm.
        var bestLag = 0
        var bestScore = 0.0
        val minLag = (sampleHz / 4.5).roundToInt().coerceAtLeast(2)     // up to 4.5 Hz
        val maxLag = (sampleHz / 0.7).roundToInt().coerceAtMost(n - 8)  // down to 0.7 Hz
        var scores = DoubleArray(0)
        if (sd >= STILL_SD && m2 > 1e-9 && maxLag > minLag) {
            scores = DoubleArray(maxLag + 1)
            for (lag in minLag..maxLag) {
                var acc = 0.0
                val len = n - lag
                for (i in 0 until len) acc += dyn[i] * dyn[i + lag]
                val score = acc / (len * m2)
                scores[lag] = score
                if (score > bestScore) {
                    bestScore = score
                    bestLag = lag
                }
            }
        }

        // A pocketed phone rides on one thigh, so the strongest repeat is usually the
        // whole stride - two steps - and walking read at stride rate falls below the
        // gait band and gets discarded as riding. A strong repeat at half that lag is
        // the real step. A pedalling wave correlates negatively at half its period, so
        // this never promotes cycling.
        var stepLag = bestLag
        if (bestLag > 0) {
            val half = bestLag / 2.0
            val lagFrom = floor(half - 2).toInt().coerceAtLeast(minLag)
            val lagTo = ceil(half + 2).toInt().coerceAtMost(maxLag)
            var halfScore = Double.NEGATIVE_INFINITY
            var halfLag = 0
            for (lag in lagFrom..lagTo) {
                if (scores[lag] > halfScore) {
                    halfScore = scores[lag]
                    halfLag = lag
                }
            }
            if (halfLag in minLag until bestLag &&
                halfScore >= OCTAVE_MIN_SCORE &&
                halfScore >= bestScore * OCTAVE_RATIO
            ) {
                stepLag = halfLag
            }
        }
        val dominantHz = if (stepLag > 0) sampleHz / stepLag else 0.0
        // Regularity stays on the strongest repeat: the truest measure of metronomic.
        val periodicity = bestScore.coerceIn(0.0, 1.0)

        return MotionFeatures(sd, p2p, kurtosis, dominantHz, periodicity, sampleHz, n)
    }

    fun classify(): Pair<MotionClass, MotionFeatures> {
        val f = features()
        if (f.samples == 0) return MotionClass.UNKNOWN to f
        return decide(f, sensitivity) to f
    }

    companion object {

        /**
         * Phones ignore the requested period - a Redmi 10A gives ~102 Hz when asked
         * for 50 - so samples are decimated to keep the window a real four seconds.
         */
        const val TARGET_HZ = 55.0
        val MIN_SAMPLE_GAP_MS = (1000.0 / TARGET_HZ).toLong()   // 18 ms

        /** Below this deviation the phone is not meaningfully moving. */
        const val STILL_SD = 0.10

        /** How strong the half-lag repeat must be, absolutely and against the stride. */
        const val OCTAVE_MIN_SCORE = 0.20
        const val OCTAVE_RATIO = 0.55

        /**
         * Thresholds scale with sensitivity. A pocketed phone sees 6-15 m/s^2 peak to
         * peak while walking, and 1-4 m/s^2 on a bicycle seat.
         */
        fun decide(f: MotionFeatures, sensitivity: Sensitivity): MotionClass {
            if (f.samples < 24 || f.sampleHz < 8.0) return MotionClass.UNKNOWN

            val walkP2p: Double
            val walkSd: Double
            val walkPeriodicity: Double
            val walkKurtosis: Double
            when (sensitivity) {
                Sensitivity.LENIENT -> {
                    walkP2p = 3.2; walkSd = 0.70; walkPeriodicity = 0.20; walkKurtosis = 1.85
                }
                Sensitivity.BALANCED -> {
                    // 4.0, not 4.5: a hand-carried phone swings less, and rhythm and
                    // impact shape now carry more of the decision.
                    walkP2p = 4.0; walkSd = 0.95; walkPeriodicity = 0.26; walkKurtosis = 2.15
                }
                Sensitivity.STRICT -> {
                    walkP2p = 6.0; walkSd = 1.40; walkPeriodicity = 0.34; walkKurtosis = 2.45
                }
            }

            if (f.sd < STILL_SD && f.p2p < 0.45) return MotionClass.STILL

            val runsFast = f.dominantHz in 2.1..4.2
            if (runsFast && f.p2p >= walkP2p * 2.2 && f.sd >= walkSd * 2.6 && f.periodicity >= 0.18) {
                return MotionClass.RUNNING
            }

            val walksInRhythm = f.dominantHz in 1.20..2.85
            if (walksInRhythm &&
                f.p2p >= walkP2p &&
                f.sd >= walkSd &&
                f.periodicity >= walkPeriodicity &&
                f.kurtosis >= walkKurtosis
            ) {
                return MotionClass.WALKING
            }

            // Cycling: rhythmic but gentle, smooth and slow, all three. The old 2.1 Hz
            // bound swallowed hand-carried walking; 0.15 regularity let car jitter pass.
            val pedalRhythm = f.dominantHz in 0.75..1.45
            if (pedalRhythm &&
                f.periodicity >= 0.30 &&
                f.p2p < walkP2p &&
                f.sd < walkSd * 1.6 &&
                f.kurtosis < walkKurtosis
            ) {
                return MotionClass.CYCLING
            }

            // Vehicle: low-level jitter or engine buzz with no gait cycle under it. Kept
            // to low-energy movement; stronger irregular motion is usually rough walking.
            val noGaitCycle = f.periodicity < 0.20 || f.dominantHz > 4.2 || f.dominantHz < 0.75
            if (noGaitCycle && f.sd < walkSd * 1.6) return MotionClass.VEHICLE

            // Strong but *irregular* jolting outside a gait: a rough road on two wheels.
            // Strong and regular is a mis-read gait - that is what discarded pocketed walking.
            if (f.p2p >= walkP2p && !walksInRhythm && !runsFast && f.periodicity < 0.35) {
                return MotionClass.VEHICLE
            }

            return MotionClass.UNKNOWN
        }
    }
}

/**
 * Adaptive peak counter, used only on devices with no hardware step sensor.
 *
 * Counts heel strikes as upward crossings of a slowly-adapting threshold, with a
 * refractory period so a single strike cannot register twice.
 */
class FallbackStepDetector {

    private var slowMean = 9.81
    private var slowDev = 0.6
    private var armed = false
    private var lastStepMs = 0L

    /** Returns the number of steps detected by this sample (0 or 1). */
    fun onSample(x: Float, y: Float, z: Float, timestampMs: Long): Int {
        val m = sqrt((x * x + y * y + z * z).toDouble())

        // Exponential moving average and deviation track the DC level and noise floor.
        slowMean += (m - slowMean) * 0.08
        slowDev += (abs(m - slowMean) - slowDev) * 0.08

        val threshold = slowMean + (slowDev * 1.2).coerceIn(0.55, 3.0)
        val release = slowMean + (slowDev * 0.4).coerceIn(0.15, 1.2)

        if (!armed && m > threshold) {
            armed = true
            // 260 ms refractory: nobody sustains more than ~230 steps per minute.
            if (timestampMs - lastStepMs > 260) {
                lastStepMs = timestampMs
                return 1
            }
        } else if (armed && m < release) {
            armed = false
        }
        return 0
    }

    fun reset() {
        slowMean = 9.81
        slowDev = 0.6
        armed = false
        lastStepMs = 0L
    }
}
