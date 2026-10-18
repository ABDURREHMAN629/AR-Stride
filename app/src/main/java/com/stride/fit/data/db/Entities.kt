package com.stride.fit.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One calendar day of ambulatory activity. Keyed by epoch day so it is timezone-stable. */
@Entity(tableName = "day_stat")
data class DayStat(
    @PrimaryKey val epochDay: Long,
    val steps: Int = 0,
    val rejectedRiding: Int = 0,
    val rejectedUnclear: Int = 0,
    val distanceM: Double = 0.0,
    val kcal: Double = 0.0,
    val activeMinutes: Int = 0,
    val runKcal: Double = 0.0,
    val strengthKcal: Double = 0.0,
    val mobilityKcal: Double = 0.0
)

/**
 * Minute-resolution step buckets. These drive the hourly chart and, more importantly,
 * let energy expenditure be computed from real cadence instead of a daily average.
 */
@Entity(tableName = "minute_bucket")
data class MinuteBucket(
    @PrimaryKey val epochMinute: Long,
    val steps: Int,
    val cadenceSpm: Double,
    val motion: String
)

@Entity(tableName = "run_session")
data class RunSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMs: Long,
    val endMs: Long = 0,
    val distanceM: Double = 0.0,
    val movingMs: Long = 0,
    val elapsedMs: Long = 0,
    val kcal: Double = 0.0,
    val elevGainM: Double = 0.0,
    val elevLossM: Double = 0.0,
    val avgPaceSecPerKm: Int = 0,
    val bestKmPaceSec: Int = 0,
    val steps: Int = 0,
    val avgCadenceSpm: Int = 0,
    val activity: String = "RUN",
    val finished: Boolean = false,
    val note: String = ""
)

@Entity(
    tableName = "run_point",
    indices = [Index("runId")]
)
data class RunPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val ts: Long,
    val lat: Double,
    val lon: Double,
    val altitude: Double,
    val accuracyM: Float,
    val speedMps: Float,
    val cumulativeM: Double
)

@Entity(
    tableName = "run_split",
    indices = [Index("runId")]
)
data class RunSplit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val index: Int,
    val meters: Double,
    val durationMs: Long,
    val paceSecPerKm: Int,
    val elevDeltaM: Double
)

@Entity(tableName = "workout_session")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val programDayId: String,
    val name: String,
    val startMs: Long,
    val endMs: Long = 0,
    val volumeKg: Double = 0.0,
    val totalSets: Int = 0,
    val totalReps: Int = 0,
    val kcal: Double = 0.0,
    val finished: Boolean = false
)

@Entity(
    tableName = "set_log",
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val setIndex: Int,
    val targetReps: String,
    val reps: Int,
    val weightKg: Double,
    val rpe: Int,
    val done: Boolean,
    val ts: Long
)

@Entity(tableName = "body_log")
data class BodyLog(
    @PrimaryKey val epochDay: Long,
    val weightKg: Double,
    val waistCm: Double = 0.0,
    val note: String = ""
)

@Entity(
    tableName = "routine_log",
    indices = [Index("epochDay")]
)
data class RoutineLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val routineId: String,
    val name: String,
    val seconds: Int,
    val kcal: Double,
    val ts: Long
)

/** Aggregate row used by the weekly charts. */
data class DailyTotal(val epochDay: Long, val value: Double)

/**
 * An area of map downloaded for offline use. Stores the bounding box and zoom range
 * rather than a tile list, since the tiles are recomputed from those on demand.
 */
@Entity(tableName = "map_region")
data class MapRegion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val centerLat: Double,
    val centerLon: Double,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val minZoom: Int,
    val maxZoom: Int,
    val radiusKm: Double,
    val tileCount: Int,
    val bytes: Long,
    val downloadedAtMs: Long
)
