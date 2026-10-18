package com.stride.fit.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Synthetic motion, fed through the real classifier.
 *
 * The signals model acceleration *magnitude*, which is what the classifier sees, so
 * phone orientation drops out. Each one is built from the physical features that
 * matter: heel-strike impulses at the step rate, the body's bounce, the left/right
 * asymmetry a pocketed phone picks up from riding on one thigh, a pedal stroke's
 * smooth wave, a car's low-level jitter.
 */
class MotionClassifierTest {

    private val sampleHz = 50.0

    /** Run [seconds] of a signal through the classifier and return the final verdict. */
    private fun classify(
        seconds: Double = 6.0,
        sensitivity: Sensitivity = Sensitivity.BALANCED,
        signal: (t: Double) -> Double
    ): Pair<MotionClass, MotionFeatures> {
        val c = MotionClassifier(sensitivity = sensitivity)
        val n = (seconds * sampleHz).toInt()
        for (i in 0 until n) {
            val t = i / sampleHz
            val z = (9.81 + signal(t)).toFloat()
            c.addSample(0f, 0f, z, (t * 1000).toLong() + 1_000L)
        }
        return c.classify()
    }

    /** A heel strike: a short sharp impulse centred on [at]. */
    private fun impulse(t: Double, at: Double, amp: Double, widthS: Double = 0.035): Double {
        val d = (t - at) / widthS
        return amp * exp(-0.5 * d * d)
    }

    /** Sum of impulses at every step, alternating between two strengths. */
    private fun steps(
        t: Double,
        stepHz: Double,
        strongAmp: Double,
        weakAmp: Double
    ): Double {
        val period = 1.0 / stepHz
        val k = (t / period).toInt()
        var sum = 0.0
        for (j in (k - 1)..(k + 1)) {
            if (j < 0) continue
            val amp = if (j % 2 == 0) strongAmp else weakAmp
            sum += impulse(t, j * period, amp)
        }
        return sum
    }

    private fun noise(rng: Random, sd: Double): Double = (rng.nextDouble() * 2 - 1) * sd * 1.73


    @Test
    fun `pocketed walk with left-right asymmetry is walking, at step rate not stride rate`() {
        val rng = Random(1)
        val (cls, f) = classify { t ->
            steps(t, stepHz = 1.8, strongAmp = 7.0, weakAmp = 4.2) +
                1.5 * sin(2 * PI * 1.8 * t) +          // body bounce, once per step
                0.6 * sin(2 * PI * 0.9 * t) +          // thigh swing, once per stride
                noise(rng, 0.25)
        }
        assertEquals("features=$f", MotionClass.WALKING, cls)
        // The fix itself: cadence must be read at the step, ~1.8 Hz, not the ~0.9 Hz stride.
        assertTrue("cadence read as ${f.dominantHz} Hz", f.dominantHz in 1.5..2.1)
    }

    @Test
    fun `slow pocketed walk is walking`() {
        val rng = Random(2)
        val (cls, f) = classify { t ->
            steps(t, stepHz = 1.5, strongAmp = 6.0, weakAmp = 3.5) +
                1.2 * sin(2 * PI * 1.5 * t) +
                0.7 * sin(2 * PI * 0.75 * t) +
                noise(rng, 0.25)
        }
        assertEquals("features=$f", MotionClass.WALKING, cls)
    }

    @Test
    fun `walking with the phone held in the hand is walking, not cycling`() {
        val rng = Random(3)
        val (cls, f) = classify { t ->
            steps(t, stepHz = 1.8, strongAmp = 3.4, weakAmp = 3.0) +
                0.8 * sin(2 * PI * 1.8 * t) +
                noise(rng, 0.2)
        }
        assertEquals("features=$f", MotionClass.WALKING, cls)
    }

    @Test
    fun `pocketed run is running`() {
        val rng = Random(4)
        val (cls, f) = classify { t ->
            steps(t, stepHz = 2.8, strongAmp = 16.0, weakAmp = 10.0) +
                3.0 * sin(2 * PI * 2.8 * t) +
                1.0 * sin(2 * PI * 1.4 * t) +
                noise(rng, 0.4)
        }
        assertEquals("features=$f", MotionClass.RUNNING, cls)
        assertTrue("cadence read as ${f.dominantHz} Hz", f.dominantHz in 2.4..3.2)
    }


    @Test
    fun `smooth seated pedalling is cycling and never counted`() {
        val rng = Random(5)
        val (cls, f) = classify { t ->
            1.1 * sin(2 * PI * 1.2 * t) + noise(rng, 0.15)
        }
        assertEquals("features=$f", MotionClass.CYCLING, cls)
    }

    @Test
    fun `pedalling with a push on each downstroke is still not counted`() {
        // Both legs push once per revolution, adding a component at twice the cadence.
        val rng = Random(6)
        val (cls, f) = classify { t ->
            1.1 * sin(2 * PI * 1.2 * t) + 0.5 * sin(2 * PI * 2.4 * t) + noise(rng, 0.15)
        }
        assertFalse("features=$f", cls.isPedestrian)
    }

    @Test
    fun `car ride jitter is a vehicle`() {
        val rng = Random(7)
        val (cls, f) = classify { _ -> noise(rng, 0.3) }
        assertEquals("features=$f", MotionClass.VEHICLE, cls)
    }

    @Test
    fun `irregular jolting on a rough road is never counted`() {
        val rng = Random(8)
        // Jolts at random moments and random strengths - no gait rhythm underneath.
        val jolts = List(30) { rng.nextDouble() * 6.0 to (3.0 + rng.nextDouble() * 5.0) }
        val (cls, f) = classify { t ->
            jolts.sumOf { (at, amp) -> impulse(t, at, amp, widthS = 0.06) } + noise(rng, 0.4)
        }
        assertFalse("features=$f", cls.isPedestrian)
    }

    @Test
    fun `phone lying on a desk is still`() {
        val rng = Random(9)
        val (cls, f) = classify { _ -> noise(rng, 0.02) }
        assertEquals("features=$f", MotionClass.STILL, cls)
    }


    @Test
    fun `a confident walk is not vetoed by a stray riding reading`() {
        val gate = PedestrianGate()
        val walk = MotionFeatures(
            sd = 2.2, p2p = 9.0, kurtosis = 3.4, dominantHz = 1.8,
            periodicity = 0.62, sampleHz = 50.0, samples = 200
        )
        var t = 10_000L
        gate.onRecognisedActivity(RecognisedActivity.IN_VEHICLE, confidence = 80, atMs = t)
        repeat(5) {
            t += 1000
            gate.onMotion(MotionClass.WALKING, walk, t)
        }
        assertEquals(Verdict.CREDIT, gate.verdictAt(t))
    }

    @Test
    fun `an ambiguous window during a riding reading stays rejected`() {
        val gate = PedestrianGate()
        val weak = MotionFeatures(
            sd = 1.0, p2p = 4.2, kurtosis = 2.2, dominantHz = 1.6,
            periodicity = 0.28, sampleHz = 50.0, samples = 200
        )
        var t = 10_000L
        gate.onRecognisedActivity(RecognisedActivity.ON_BICYCLE, confidence = 80, atMs = t)
        repeat(5) {
            t += 1000
            gate.onMotion(MotionClass.WALKING, weak, t)
        }
        assertEquals(Verdict.REJECT_RIDING, gate.verdictAt(t))
    }
}
