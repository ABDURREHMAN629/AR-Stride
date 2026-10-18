package com.stride.fit.sensors

/** Coarse activity as reported by Google Play services, mapped off the GMS constants. */
enum class RecognisedActivity {
    ON_FOOT, WALKING, RUNNING, ON_BICYCLE, IN_VEHICLE, STILL, TILTING, UNKNOWN
}

/** Why a batch of raw pedometer steps was kept or thrown away. */
enum class Verdict {
    CREDIT,
    REJECT_RIDING,      // bicycle or motor vehicle
    REJECT_UNCLEAR,     // motion did not look like a gait
    REJECT_STILL
}

data class GateState(
    val motion: MotionClass = MotionClass.UNKNOWN,
    val features: MotionFeatures = MotionFeatures.EMPTY,
    val recognised: RecognisedActivity = RecognisedActivity.UNKNOWN,
    val ridingLatched: Boolean = false,
    val open: Boolean = false,
    val creditedToday: Int = 0,
    val rejectedRiding: Int = 0,
    val rejectedUnclear: Int = 0
)

/**
 * Decides, second by second, whether hardware pedometer steps are real. The
 * accelerometer [MotionClassifier] is the primary judge; Play services activity
 * recognition is a veto that latches shut for 90 s on a confident riding reading.
 * Hysteresis - two pedestrian windows to open, three to close - stops flickering.
 */
class PedestrianGate {

    private companion object {
        const val OPEN_STREAK = 2
        const val CLOSE_STREAK = 3
        const val RIDING_LATCH_MS = 90_000L
        const val HISTORY = 1200            // 20 minutes at one verdict per second
        const val ALIGN_TOLERANCE_MS = 45_000L
    }

    // Lets a late-delivered batched pedometer event be matched to what the body was
    // doing when those steps actually happened.
    private val histTs = LongArray(HISTORY)
    private val histOpen = BooleanArray(HISTORY)
    private val histClass = arrayOfNulls<MotionClass>(HISTORY)
    private val histCadence = DoubleArray(HISTORY)
    private var histHead = 0
    private var histCount = 0

    private var openStreak = 0
    private var closeStreak = 0
    private var gateOpen = false

    private var ridingUntilMs = 0L
    private var recognised = RecognisedActivity.UNKNOWN
    private var lastMotion = MotionClass.UNKNOWN
    private var lastFeatures = MotionFeatures.EMPTY

    var creditedToday: Int = 0; private set
    var rejectedRiding: Int = 0; private set
    var rejectedUnclear: Int = 0; private set

    fun resetDailyCounters() {
        creditedToday = 0
        rejectedRiding = 0
        rejectedUnclear = 0
    }

    fun snapshot(): GateState = GateState(
        motion = lastMotion,
        features = lastFeatures,
        recognised = recognised,
        ridingLatched = ridingUntilMs > System.currentTimeMillis(),
        open = gateOpen,
        creditedToday = creditedToday,
        rejectedRiding = rejectedRiding,
        rejectedUnclear = rejectedUnclear
    )

    /** Feed one classifier verdict. Call roughly once per second. */
    fun onMotion(motion: MotionClass, features: MotionFeatures, atMs: Long) {
        lastMotion = motion
        lastFeatures = features

        val riding = atMs < ridingUntilMs
        // An unmistakable gait cannot happen in a vehicle, so it overrides a riding
        // reading - sandboxed Play services misreports and would throw walking away.
        // Ambiguous windows are still vetoed: the motorbike-on-a-rough-road case.
        val vetoed = riding && !(motion.isPedestrian && isConfidentGait(features))
        val pedestrian = motion.isPedestrian && !vetoed

        if (pedestrian) {
            openStreak++
            closeStreak = 0
            if (openStreak >= OPEN_STREAK) gateOpen = true
        } else {
            closeStreak++
            openStreak = 0
            if (closeStreak >= CLOSE_STREAK) gateOpen = false
        }

        histTs[histHead] = atMs
        histOpen[histHead] = gateOpen
        histClass[histHead] = if (vetoed) MotionClass.VEHICLE else motion
        histCadence[histHead] = features.cadenceSpm
        histHead = (histHead + 1) % HISTORY
        if (histCount < HISTORY) histCount++
    }

    /** Strong enough on every axis that no vehicle produces it. */
    private fun isConfidentGait(f: MotionFeatures): Boolean =
        f.periodicity >= 0.45 &&
            f.kurtosis >= 2.3 &&
            f.p2p >= 5.0 &&
            f.dominantHz in 1.3..3.8

    /** Confident riding latches the gate shut; confident walking releases it early. */
    fun onRecognisedActivity(activity: RecognisedActivity, confidence: Int, atMs: Long) {
        recognised = activity
        when (activity) {
            RecognisedActivity.ON_BICYCLE, RecognisedActivity.IN_VEHICLE ->
                if (confidence >= 55) {
                    ridingUntilMs = atMs + RIDING_LATCH_MS
                    gateOpen = false
                    openStreak = 0
                }
            RecognisedActivity.WALKING, RecognisedActivity.RUNNING, RecognisedActivity.ON_FOOT ->
                if (confidence >= 70) ridingUntilMs = 0L
            else -> Unit
        }
    }

    /** Verdict plus the context needed to price the steps: what motion, at what cadence. */
    data class Resolution(
        val verdict: Verdict,
        val motion: MotionClass,
        val cadenceSpm: Double
    )

    /** What was the body doing at [atMs]? Used to judge late-delivered step batches. */
    fun resolve(atMs: Long): Resolution {
        val idx = nearestHistoryIndex(atMs)
        if (idx < 0) {
            // No history that close: fall back to the live state, and stay conservative.
            return Resolution(
                verdict = if (gateOpen) Verdict.CREDIT else Verdict.REJECT_UNCLEAR,
                motion = lastMotion,
                cadenceSpm = lastFeatures.cadenceSpm
            )
        }
        val motion = histClass[idx] ?: MotionClass.UNKNOWN
        val cadence = histCadence[idx]
        val verdict = when {
            histOpen[idx] -> Verdict.CREDIT
            motion == MotionClass.CYCLING || motion == MotionClass.VEHICLE -> Verdict.REJECT_RIDING
            motion == MotionClass.STILL -> Verdict.REJECT_STILL
            else -> Verdict.REJECT_UNCLEAR
        }
        return Resolution(verdict, motion, cadence)
    }

    fun verdictAt(atMs: Long): Verdict = resolve(atMs).verdict

    private fun nearestHistoryIndex(atMs: Long): Int {
        if (histCount == 0) return -1
        var best = -1
        var bestDelta = Long.MAX_VALUE
        val start = (histHead - histCount + HISTORY) % HISTORY
        for (i in 0 until histCount) {
            val idx = (start + i) % HISTORY
            val d = kotlin.math.abs(histTs[idx] - atMs)
            if (d < bestDelta) {
                bestDelta = d
                best = idx
            }
        }
        return if (bestDelta <= ALIGN_TOLERANCE_MS) best else -1
    }

    /** Apply a verdict to a batch of raw steps, returning how many were credited. */
    fun account(rawSteps: Int, verdict: Verdict): Int {
        if (rawSteps <= 0) return 0
        return when (verdict) {
            Verdict.CREDIT -> {
                creditedToday += rawSteps
                rawSteps
            }
            Verdict.REJECT_RIDING -> {
                rejectedRiding += rawSteps
                0
            }
            Verdict.REJECT_UNCLEAR, Verdict.REJECT_STILL -> {
                rejectedUnclear += rawSteps
                0
            }
        }
    }
}
