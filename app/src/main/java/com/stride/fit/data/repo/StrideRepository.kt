package com.stride.fit.data.repo

import android.content.Context
import com.stride.fit.data.db.BodyLog
import com.stride.fit.data.db.DayStat
import com.stride.fit.data.db.MinuteBucket
import com.stride.fit.data.db.RoutineLog
import com.stride.fit.data.db.StrideDatabase
import com.stride.fit.health.HealthMath
import com.stride.fit.sensors.MotionClass
import com.stride.fit.util.Time
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single funnel for everything that reads or writes user data. Step writes arrive from a
 * background service and UI reads happen on the main thread, so all read-modify-write
 * day totals are serialised behind a mutex.
 */
class StrideRepository private constructor(context: Context) {

    private val app = context.applicationContext
    private val db = StrideDatabase.get(app)
    val settings = SettingsStore(app)

    private val dayMutex = Mutex()

    val profile: Flow<Profile> = settings.profile

    val dayStatDao get() = db.dayStatDao()
    val minuteDao get() = db.minuteBucketDao()
    val runDao get() = db.runDao()
    val workoutDao get() = db.workoutDao()
    val bodyDao get() = db.bodyDao()
    val routineDao get() = db.routineDao()
    val mapRegionDao get() = db.mapRegionDao()


    /** Distance and energy come from this batch's cadence, not from a daily average. */
    suspend fun recordSteps(steps: Int, atMs: Long, motion: MotionClass, cadenceSpm: Double) {
        if (steps <= 0) return
        val p = profile.first()
        val cadence = cadenceSpm.takeIf { it > 20 } ?: 100.0
        val running = motion == MotionClass.RUNNING

        val distance = HealthMath.distanceFromStepsM(
            steps = steps,
            heightCm = p.heightCm,
            male = p.male,
            cadenceSpm = cadence,
            running = running
        )
        val kcal = HealthMath.kcalFromSteps(steps, cadence, p.weightKg, p.heightCm, p.male)

        dayMutex.withLock {
            val minute = Time.epochMinuteOf(atMs)
            val existing = minuteDao.get(minute)
            minuteDao.upsert(
                MinuteBucket(
                    epochMinute = minute,
                    steps = (existing?.steps ?: 0) + steps,
                    cadenceSpm = maxOf(existing?.cadenceSpm ?: 0.0, cadence),
                    motion = motion.name
                )
            )

            val day = Time.epochDayOf(atMs)
            val stat = dayStatDao.get(day) ?: DayStat(epochDay = day)
            dayStatDao.upsert(
                stat.copy(
                    steps = stat.steps + steps,
                    distanceM = stat.distanceM + distance,
                    kcal = stat.kcal + kcal
                )
            )
        }
    }

    /** Record steps the gate refused, so the rejection is visible rather than silent. */
    suspend fun recordRejected(steps: Int, riding: Boolean, atMs: Long) {
        if (steps <= 0) return
        dayMutex.withLock {
            val day = Time.epochDayOf(atMs)
            val stat = dayStatDao.get(day) ?: DayStat(epochDay = day)
            dayStatDao.upsert(
                if (riding) stat.copy(rejectedRiding = stat.rejectedRiding + steps)
                else stat.copy(rejectedUnclear = stat.rejectedUnclear + steps)
            )
        }
    }

    /** A minute counts as active at 60+ steps, the usual threshold for purposeful walking. */
    suspend fun refreshActiveMinutes(day: Long = Time.today()) {
        val range = Time.minuteRangeForDay(day)
        val active = minuteDao.activeMinutes(range.first, range.last)
        dayMutex.withLock {
            val stat = dayStatDao.get(day) ?: DayStat(epochDay = day)
            if (stat.activeMinutes != active) {
                dayStatDao.upsert(stat.copy(activeMinutes = active))
            }
        }
    }

    suspend fun addWorkoutEnergy(day: Long, strength: Double = 0.0, mobility: Double = 0.0, run: Double = 0.0) {
        dayMutex.withLock {
            val stat = dayStatDao.get(day) ?: DayStat(epochDay = day)
            dayStatDao.upsert(
                stat.copy(
                    strengthKcal = stat.strengthKcal + strength,
                    mobilityKcal = stat.mobilityKcal + mobility,
                    runKcal = stat.runKcal + run
                )
            )
        }
    }

    fun observeDay(day: Long): Flow<DayStat> =
        dayStatDao.observe(day).map { it ?: DayStat(epochDay = day) }

    fun observeWeek(endDay: Long = Time.today()): Flow<List<DayStat>> =
        dayStatDao.observeRange(endDay - 6, endDay).map { rows ->
            val byDay = rows.associateBy { it.epochDay }
            ((endDay - 6)..endDay).map { byDay[it] ?: DayStat(epochDay = it) }
        }

    fun observeHourlySteps(day: Long): Flow<List<Int>> {
        val range = Time.minuteRangeForDay(day)
        return minuteDao.observeRange(range.first, range.last).map { buckets ->
            val hours = IntArray(24)
            buckets.forEach { hours[Time.hourOfMinute(it.epochMinute).coerceIn(0, 23)] += it.steps }
            hours.toList()
        }
    }

    /** Fourteen days split into this week and last: better or worse than before? */
    fun observeStepComparison(endDay: Long = Time.today()): Flow<StepComparison> =
        dayStatDao.observeRange(endDay - 13, endDay).map { rows ->
            val byDay = rows.associateBy { it.epochDay }
            val fortnight = ((endDay - 13)..endDay).map { byDay[it] ?: DayStat(epochDay = it) }
            val thisWeek = fortnight.takeLast(7)
            val lastWeek = fortnight.take(7)

            val best = thisWeek.maxByOrNull { it.steps }
            StepComparison(
                today = byDay[endDay]?.steps ?: 0,
                yesterday = byDay[endDay - 1]?.steps ?: 0,
                thisWeekTotal = thisWeek.sumOf { it.steps },
                lastWeekTotal = lastWeek.sumOf { it.steps },
                thisWeekAvg = thisWeek.sumOf { it.steps } / 7,
                lastWeekAvg = lastWeek.sumOf { it.steps } / 7,
                thisWeekDistanceM = thisWeek.sumOf { it.distanceM },
                lastWeekDistanceM = lastWeek.sumOf { it.distanceM },
                bestDaySteps = best?.steps ?: 0,
                bestDayLabel = best?.let { Time.shortWeekday(it.epochDay) } ?: "",
                thisWeek = thisWeek,
                lastWeek = lastWeek
            )
        }

    /** Median of the last seven days, used to move the step goal at a sane pace. */
    suspend fun sevenDayMedianSteps(endDay: Long = Time.today()): Int {
        val sorted = dayStatDao.stepsSorted(endDay - 6, endDay)
        if (sorted.isEmpty()) return 0
        return sorted[sorted.size / 2]
    }

    /** Once a day at most, on real evidence: a goal that moves every session means nothing. */
    suspend fun reviewStepGoal() {
        val p = profile.first()
        val today = Time.today()
        if (p.lastGoalReviewDay >= today) return
        val median = sevenDayMedianSteps(today - 1)
        if (median == 0) {
            settings.update { it.copy(lastGoalReviewDay = today) }
            return
        }
        val next = HealthMath.nextStepGoal(p.stepGoal, median)
        settings.update { it.copy(stepGoal = next, lastGoalReviewDay = today) }
    }


    suspend fun logWeight(kg: Double, day: Long = Time.today(), waistCm: Double = 0.0, note: String = "") {
        bodyDao.upsert(BodyLog(epochDay = day, weightKg = kg, waistCm = waistCm, note = note))
        settings.update { it.copy(weightKg = kg) }
    }

    fun observeWeightHistory(days: Int = 180): Flow<List<BodyLog>> =
        bodyDao.observeSince(Time.today() - days)


    suspend fun logRoutine(routineId: String, name: String, seconds: Int) {
        val p = profile.first()
        val kcal = HealthMath.kcalFromMobility(seconds / 60.0, p.weightKg)
        val day = Time.today()
        routineDao.insert(
            RoutineLog(
                epochDay = day,
                routineId = routineId,
                name = name,
                seconds = seconds,
                kcal = kcal,
                ts = System.currentTimeMillis()
            )
        )
        addWorkoutEnergy(day, mobility = kcal)
    }

    /** Consecutive days ending today (or yesterday) with at least one routine logged. */
    suspend fun postureStreak(): Int {
        val days = routineDao.activeDays(400).toSortedSet(reverseOrder())
        if (days.isEmpty()) return 0
        val today = Time.today()
        var cursor = if (days.contains(today)) today else today - 1
        if (!days.contains(cursor)) return 0
        var streak = 0
        while (days.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }


    /** Everything the Today screen needs, in one flow. */
    fun observeTodaySummary(): Flow<TodaySummary> =
        combine(observeDay(Time.today()), profile) { stat, p ->
            val activityKcal = stat.kcal
            val exerciseKcal = stat.runKcal + stat.strengthKcal + stat.mobilityKcal
            TodaySummary(
                stat = stat,
                profile = p,
                totalBurnKcal = activityKcal + exerciseKcal,
                tdeeKcal = HealthMath.tdee(p.bmr, activityKcal, exerciseKcal),
                stepProgress = if (p.stepGoal > 0) (stat.steps.toDouble() / p.stepGoal).coerceIn(0.0, 1.0) else 0.0,
                burnProgress = if (p.calorieBurnGoal > 0)
                    ((activityKcal + exerciseKcal) / p.calorieBurnGoal).coerceIn(0.0, 1.0) else 0.0
            )
        }

    companion object {
        @Volatile private var instance: StrideRepository? = null

        fun get(context: Context): StrideRepository =
            instance ?: synchronized(this) {
                instance ?: StrideRepository(context).also { instance = it }
            }
    }
}

/** Today against yesterday, and this week against last. */
data class StepComparison(
    val today: Int = 0,
    val yesterday: Int = 0,
    val thisWeekTotal: Int = 0,
    val lastWeekTotal: Int = 0,
    val thisWeekAvg: Int = 0,
    val lastWeekAvg: Int = 0,
    val thisWeekDistanceM: Double = 0.0,
    val lastWeekDistanceM: Double = 0.0,
    val bestDaySteps: Int = 0,
    val bestDayLabel: String = "",
    val thisWeek: List<DayStat> = emptyList(),
    val lastWeek: List<DayStat> = emptyList()
) {
    val dayDelta: Int get() = today - yesterday
    val weekDelta: Int get() = thisWeekTotal - lastWeekTotal

    /** Percent change week on week, or null when there is no baseline to compare to. */
    val weekChangePercent: Int?
        get() = if (lastWeekTotal <= 0) null
        else ((thisWeekTotal - lastWeekTotal) * 100.0 / lastWeekTotal).toInt()

    val hasLastWeek: Boolean get() = lastWeekTotal > 0
}

data class TodaySummary(
    val stat: DayStat,
    val profile: Profile,
    val totalBurnKcal: Double,
    val tdeeKcal: Double,
    val stepProgress: Double,
    val burnProgress: Double
)
