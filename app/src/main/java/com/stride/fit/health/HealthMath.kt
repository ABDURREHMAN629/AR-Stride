package com.stride.fit.health

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * All physiological maths. Defaults are seeded for the owner of this build (male,
 * 82.0 kg, 162.6 cm); every function takes explicit parameters so the profile stays editable.
 */
object HealthMath {

    const val DEFAULT_WEIGHT_KG = 82.0
    const val DEFAULT_HEIGHT_CM = 162.6   // 5 ft 4 in
    const val DEFAULT_AGE = 25
    const val DEFAULT_SEX_MALE = true


    fun bmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0) return 0.0
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }

    fun bmiCategory(bmi: Double): String = when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Healthy"
        bmi < 30.0 -> "Overweight"
        bmi < 35.0 -> "Obese I"
        bmi < 40.0 -> "Obese II"
        else -> "Obese III"
    }

    /** Weight span that puts BMI between 18.5 and 24.9 for this height. */
    fun healthyWeightRangeKg(heightCm: Double): ClosedFloatingPointRange<Double> {
        val m = heightCm / 100.0
        return (18.5 * m * m)..(24.9 * m * m)
    }

    /** Mifflin-St Jeor: the best-validated BMR estimate for the general population. */
    fun bmr(weightKg: Double, heightCm: Double, age: Int, male: Boolean): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        return if (male) base + 5.0 else base - 161.0
    }

    /** TDEE from the measured step count rather than a coarse lifestyle multiplier. */
    fun tdee(bmr: Double, activityKcal: Double, exerciseKcal: Double): Double {
        // 10 % thermic effect of food on top of resting + movement
        val gross = bmr + activityKcal + exerciseKcal
        return gross * 1.10
    }

    /** Lean body mass estimate (Boer). Useful for setting a protein target. */
    fun leanBodyMassKg(weightKg: Double, heightCm: Double, male: Boolean): Double =
        if (male) 0.407 * weightKg + 0.267 * heightCm - 19.2
        else 0.252 * weightKg + 0.473 * heightCm - 48.3

    fun proteinTargetG(weightKg: Double, heightCm: Double, male: Boolean): Int {
        // ~2 g per kg of lean mass: sensible for a fat-loss phase with lifting
        val lbm = leanBodyMassKg(weightKg, heightCm, male)
        return (lbm * 2.0).roundToInt()
    }

    /** Water in millilitres: 35 ml per kg, plus 500 ml per hour of training. */
    fun waterTargetMl(weightKg: Double, trainingMinutes: Int): Int =
        (weightKg * 35.0 + trainingMinutes / 60.0 * 500.0).roundToInt()


    /** Wilkie ratio: 0.415 x height for men, 0.413 for women, stretched by cadence. */
    fun walkingStrideM(heightCm: Double, male: Boolean, cadenceSpm: Double = 110.0): Double {
        val base = heightCm / 100.0 * (if (male) 0.415 else 0.413)
        val factor = when {
            cadenceSpm < 80 -> 0.86
            cadenceSpm < 95 -> 0.93
            cadenceSpm < 110 -> 1.0
            cadenceSpm < 125 -> 1.08
            else -> 1.16
        }
        return base * factor
    }

    /** Running stride is roughly half your height and grows with cadence. */
    fun runningStrideM(heightCm: Double, cadenceSpm: Double = 165.0): Double {
        val base = heightCm / 100.0 * 0.50
        val factor = (0.85 + (cadenceSpm - 150.0) / 150.0).coerceIn(0.80, 1.35)
        return base * factor
    }

    fun distanceFromStepsM(
        steps: Int,
        heightCm: Double,
        male: Boolean,
        cadenceSpm: Double,
        running: Boolean
    ): Double {
        val stride = if (running) runningStrideM(heightCm, cadenceSpm)
        else walkingStrideM(heightCm, male, cadenceSpm)
        return steps * stride
    }


    /** Compendium of Physical Activities METs for level walking, interpolated. */
    fun walkingMet(speedKmh: Double): Double {
        val anchors = listOf(
            2.0 to 2.0, 3.2 to 2.8, 4.0 to 3.0, 4.8 to 3.5,
            5.6 to 4.3, 6.4 to 5.0, 7.2 to 6.3
        )
        return interpolate(anchors, speedKmh).coerceIn(1.5, 8.0)
    }

    /** ACSM: VO2 = 0.2*S + 0.9*S*grade + 3.5, S in m/min. Continuous, unlike MET tables. */
    fun runningMet(speedKmh: Double, gradeFraction: Double = 0.0): Double {
        val mPerMin = speedKmh * 1000.0 / 60.0
        val vo2 = 0.2 * mPerMin + 0.9 * mPerMin * gradeFraction + 3.5
        return (vo2 / 3.5).coerceIn(2.0, 23.0)
    }

    /** Below ~7 km/h people walk, above it they run. Pick the right equation. */
    fun ambulatoryMet(speedKmh: Double, gradeFraction: Double = 0.0): Double =
        if (speedKmh < 7.0) walkingMet(speedKmh) + 15.0 * gradeFraction
        else runningMet(speedKmh, gradeFraction)

    /** kcal = MET x 3.5 x kg / 200 per minute, less the resting cost: the extra burn. */
    fun kcal(met: Double, weightKg: Double, minutes: Double, netOfResting: Boolean = true): Double {
        val gross = met * 3.5 * weightKg / 200.0 * minutes
        if (!netOfResting) return gross
        val resting = 1.0 * 3.5 * weightKg / 200.0 * minutes
        return max(0.0, gross - resting)
    }

    /** Energy for a block of steps, using cadence to infer speed. */
    fun kcalFromSteps(
        steps: Int,
        cadenceSpm: Double,
        weightKg: Double,
        heightCm: Double,
        male: Boolean
    ): Double {
        if (steps <= 0 || cadenceSpm <= 0) return 0.0
        val running = cadenceSpm >= 140
        val stride = if (running) runningStrideM(heightCm, cadenceSpm)
        else walkingStrideM(heightCm, male, cadenceSpm)
        val speedKmh = cadenceSpm * stride * 60.0 / 1000.0
        val minutes = steps / cadenceSpm
        return kcal(ambulatoryMet(speedKmh), weightKg, minutes)
    }

    /** Resistance training burn. Vigorous lifting is ~6 MET, moderate ~3.5. */
    fun kcalFromStrength(minutes: Double, weightKg: Double, vigorous: Boolean): Double =
        kcal(if (vigorous) 6.0 else 3.5, weightKg, minutes)

    fun kcalFromMobility(minutes: Double, weightKg: Double): Double =
        kcal(2.5, weightKg, minutes)


    /** Tanaka: 208 - 0.7 x age. Beats the old 220 - age on every validation set. */
    fun maxHr(age: Int): Int = (208.0 - 0.7 * age).roundToInt()

    data class Zone(val name: String, val lowBpm: Int, val highBpm: Int, val purpose: String)

    fun zones(age: Int, restingHr: Int = 70): List<Zone> {
        val hrMax = maxHr(age)
        val reserve = hrMax - restingHr
        fun k(p: Double) = (restingHr + reserve * p).roundToInt()   // Karvonen
        return listOf(
            Zone("Z1 Recovery", k(0.50), k(0.60), "Warm-up and easy walking"),
            Zone("Z2 Aerobic", k(0.60), k(0.70), "Fat oxidation, base building"),
            Zone("Z3 Tempo", k(0.70), k(0.80), "Aerobic power"),
            Zone("Z4 Threshold", k(0.80), k(0.90), "Lactate threshold"),
            Zone("Z5 Max", k(0.90), hrMax, "Short intervals only")
        )
    }


    /** A safe deficit is 0.5 to 0.75 % of bodyweight per week. */
    fun recommendedDailyDeficitKcal(weightKg: Double): Int =
        (weightKg * 0.0065 * 7700.0 / 7.0).roundToInt().coerceIn(300, 750)

    fun weeksToTarget(currentKg: Double, targetKg: Double, weeklyLossKg: Double = 0.5): Int {
        if (targetKg >= currentKg || weeklyLossKg <= 0) return 0
        return ((currentKg - targetKg) / weeklyLossKg).roundToInt()
    }

    /** Progressive step goal: nudge by 500 once the 7-day median clears the current goal. */
    fun nextStepGoal(currentGoal: Int, sevenDayMedian: Int): Int {
        if (sevenDayMedian >= currentGoal) return min(currentGoal + 500, 15000)
        if (sevenDayMedian < currentGoal * 0.7) return max(currentGoal - 500, 4000)
        return currentGoal
    }

    /** Epley one-rep max, capped where the formula stops behaving. */
    fun oneRepMax(weightKg: Double, reps: Int): Double {
        if (reps <= 0) return 0.0
        if (reps == 1) return weightKg
        if (reps > 12) return weightKg * (1 + 12 / 30.0)
        return weightKg * (1 + reps / 30.0)
    }

    fun estimateWeightForReps(oneRm: Double, reps: Int): Double =
        if (reps <= 1) oneRm else oneRm / (1 + reps / 30.0)


    private fun interpolate(anchors: List<Pair<Double, Double>>, x: Double): Double {
        if (anchors.isEmpty()) return 0.0
        if (x <= anchors.first().first) return anchors.first().second
        if (x >= anchors.last().first) {
            val (x1, y1) = anchors[anchors.size - 2]
            val (x2, y2) = anchors.last()
            return y2 + (x - x2) * (y2 - y1) / (x2 - x1)
        }
        for (i in 0 until anchors.size - 1) {
            val (x1, y1) = anchors[i]
            val (x2, y2) = anchors[i + 1]
            if (x in x1..x2) return y1 + (x - x1) * (y2 - y1) / (x2 - x1)
        }
        return anchors.last().second
    }

    fun paceSecPerKm(distanceM: Double, durationMs: Long): Int {
        if (distanceM < 1.0 || durationMs <= 0) return 0
        return (durationMs / 1000.0 / (distanceM / 1000.0)).roundToInt()
    }

    fun speedKmh(distanceM: Double, durationMs: Long): Double {
        if (durationMs <= 0) return 0.0
        return distanceM / 1000.0 / (durationMs / 3_600_000.0)
    }

    /** VO2max estimate from a recent best 5 k, via the Daniels-Gilbert VDOT model. */
    fun vo2maxFrom5k(seconds: Int): Double {
        if (seconds <= 0) return 0.0
        val minutes = seconds / 60.0
        val velocity = 5000.0 / minutes                     // m/min
        val percentMax = 0.8 + 0.1894393 * exp(-0.012778 * minutes) +
                0.2989558 * exp(-0.1932605 * minutes)
        val vo2 = -4.60 + 0.182258 * velocity + 0.000104 * velocity.pow(2)
        return vo2 / percentMax
    }
}
