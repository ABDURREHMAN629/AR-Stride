package com.stride.fit.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DayStatDao {

    @Upsert
    suspend fun upsert(stat: DayStat)

    @Query("SELECT * FROM day_stat WHERE epochDay = :day")
    suspend fun get(day: Long): DayStat?

    @Query("SELECT * FROM day_stat WHERE epochDay = :day")
    fun observe(day: Long): Flow<DayStat?>

    @Query("SELECT * FROM day_stat WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    fun observeRange(from: Long, to: Long): Flow<List<DayStat>>

    @Query("SELECT * FROM day_stat WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    suspend fun range(from: Long, to: Long): List<DayStat>

    @Query("SELECT COALESCE(SUM(steps), 0) FROM day_stat WHERE epochDay BETWEEN :from AND :to")
    fun observeStepSum(from: Long, to: Long): Flow<Int>

    @Query("SELECT steps FROM day_stat WHERE epochDay BETWEEN :from AND :to ORDER BY steps")
    suspend fun stepsSorted(from: Long, to: Long): List<Int>
}

@Dao
interface MinuteBucketDao {

    @Upsert
    suspend fun upsert(bucket: MinuteBucket)

    @Query("SELECT * FROM minute_bucket WHERE epochMinute = :minute")
    suspend fun get(minute: Long): MinuteBucket?

    @Query("SELECT * FROM minute_bucket WHERE epochMinute BETWEEN :from AND :to ORDER BY epochMinute")
    fun observeRange(from: Long, to: Long): Flow<List<MinuteBucket>>

    @Query("SELECT COUNT(*) FROM minute_bucket WHERE epochMinute BETWEEN :from AND :to AND steps >= 60")
    suspend fun activeMinutes(from: Long, to: Long): Int

    @Query("DELETE FROM minute_bucket WHERE epochMinute < :before")
    suspend fun prune(before: Long)
}

@Dao
interface RunDao {

    @Insert
    suspend fun insertSession(session: RunSession): Long

    @Update
    suspend fun updateSession(session: RunSession)

    @Query("SELECT * FROM run_session WHERE id = :id")
    suspend fun session(id: Long): RunSession?

    @Query("SELECT * FROM run_session WHERE id = :id")
    fun observeSession(id: Long): Flow<RunSession?>

    @Query("SELECT * FROM run_session WHERE finished = 1 ORDER BY startMs DESC")
    fun observeAll(): Flow<List<RunSession>>

    @Query("SELECT * FROM run_session WHERE finished = 1 ORDER BY startMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RunSession>>

    @Query("SELECT * FROM run_session WHERE finished = 0 ORDER BY startMs DESC LIMIT 1")
    suspend fun unfinished(): RunSession?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPoint(point: RunPoint): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPoints(points: List<RunPoint>)

    @Query("SELECT * FROM run_point WHERE runId = :runId ORDER BY ts")
    suspend fun points(runId: Long): List<RunPoint>

    @Query("SELECT * FROM run_point WHERE runId = :runId ORDER BY ts")
    fun observePoints(runId: Long): Flow<List<RunPoint>>

    @Insert
    suspend fun insertSplit(split: RunSplit)

    @Query("SELECT * FROM run_split WHERE runId = :runId ORDER BY `index`")
    suspend fun splits(runId: Long): List<RunSplit>

    @Query("DELETE FROM run_session WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM run_point WHERE runId = :id")
    suspend fun deletePoints(id: Long)

    @Query("DELETE FROM run_split WHERE runId = :id")
    suspend fun deleteSplits(id: Long)

    @Query("SELECT COALESCE(SUM(distanceM), 0) FROM run_session WHERE finished = 1 AND startMs >= :since")
    fun observeDistanceSince(since: Long): Flow<Double>

    @Query("SELECT COALESCE(MAX(distanceM), 0) FROM run_session WHERE finished = 1")
    suspend fun longestRunM(): Double

    @Query("SELECT COALESCE(MIN(NULLIF(avgPaceSecPerKm, 0)), 0) FROM run_session WHERE finished = 1 AND distanceM >= 1000")
    suspend fun bestPaceSecPerKm(): Int
}

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun session(id: Long): WorkoutSession?

    @Query("SELECT * FROM workout_session WHERE finished = 0 ORDER BY startMs DESC LIMIT 1")
    suspend fun unfinished(): WorkoutSession?

    @Query("SELECT * FROM workout_session WHERE finished = 1 ORDER BY startMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_session WHERE finished = 1 ORDER BY startMs DESC")
    fun observeAll(): Flow<List<WorkoutSession>>

    @Query("SELECT COUNT(*) FROM workout_session WHERE finished = 1 AND epochDay BETWEEN :from AND :to")
    fun observeCount(from: Long, to: Long): Flow<Int>

    @Upsert
    suspend fun upsertSet(set: SetLog): Long

    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId ORDER BY id")
    fun observeSets(sessionId: Long): Flow<List<SetLog>>

    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId ORDER BY id")
    suspend fun sets(sessionId: Long): List<SetLog>

    @Query("DELETE FROM set_log WHERE id = :id")
    suspend fun deleteSet(id: Long)

    /** Heaviest completed set for an exercise: the basis for progressive overload. */
    @Query(
        "SELECT * FROM set_log WHERE exerciseId = :exerciseId AND done = 1 AND reps > 0 " +
            "ORDER BY weightKg DESC, reps DESC LIMIT 1"
    )
    suspend fun bestSet(exerciseId: String): SetLog?

    @Query(
        "SELECT * FROM set_log WHERE exerciseId = :exerciseId AND done = 1 " +
            "ORDER BY ts DESC LIMIT :limit"
    )
    suspend fun recentSets(exerciseId: String, limit: Int): List<SetLog>

    @Query("SELECT DISTINCT exerciseId FROM set_log WHERE done = 1")
    suspend fun trainedExercises(): List<String>

    /** Which days of the 30-day plan have been completed. */
    @Query("SELECT DISTINCT programDayId FROM workout_session WHERE finished = 1 AND programDayId LIKE 'plan\\_%' ESCAPE '\\'")
    fun observeCompletedPlanDays(): Flow<List<String>>

    @Query("SELECT * FROM workout_session WHERE programDayId = :dayId AND finished = 1 ORDER BY startMs")
    suspend fun sessionsForPlanDay(dayId: String): List<WorkoutSession>
}

@Dao
interface BodyDao {

    @Upsert
    suspend fun upsert(log: BodyLog)

    @Query("SELECT * FROM body_log ORDER BY epochDay DESC LIMIT 1")
    fun observeLatest(): Flow<BodyLog?>

    @Query("SELECT * FROM body_log ORDER BY epochDay DESC LIMIT 1")
    suspend fun latest(): BodyLog?

    @Query("SELECT * FROM body_log ORDER BY epochDay")
    fun observeAll(): Flow<List<BodyLog>>

    @Query("SELECT * FROM body_log WHERE epochDay >= :from ORDER BY epochDay")
    fun observeSince(from: Long): Flow<List<BodyLog>>

    @Query("DELETE FROM body_log WHERE epochDay = :day")
    suspend fun delete(day: Long)
}

@Dao
interface RoutineDao {

    @Insert
    suspend fun insert(log: RoutineLog)

    @Query("SELECT * FROM routine_log WHERE epochDay = :day ORDER BY ts DESC")
    fun observeDay(day: Long): Flow<List<RoutineLog>>

    @Query("SELECT * FROM routine_log ORDER BY ts DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RoutineLog>>

    @Query("SELECT DISTINCT epochDay FROM routine_log ORDER BY epochDay DESC LIMIT :limit")
    suspend fun activeDays(limit: Int): List<Long>

    @Query("SELECT COALESCE(SUM(seconds), 0) FROM routine_log WHERE epochDay BETWEEN :from AND :to")
    fun observeSeconds(from: Long, to: Long): Flow<Int>
}

@Dao
interface MapRegionDao {

    @Insert
    suspend fun insert(region: MapRegion): Long

    @Query("SELECT * FROM map_region ORDER BY downloadedAtMs DESC")
    fun observeAll(): Flow<List<MapRegion>>

    @Query("SELECT * FROM map_region ORDER BY downloadedAtMs DESC")
    suspend fun all(): List<MapRegion>

    @Query("DELETE FROM map_region WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM map_region WHERE id = :id")
    suspend fun get(id: Long): MapRegion?

    /** Whether any saved region already covers a point, so we do not offer a duplicate. */
    @Query(
        "SELECT COUNT(*) FROM map_region WHERE :lat BETWEEN minLat AND maxLat " +
            "AND :lon BETWEEN minLon AND maxLon"
    )
    suspend fun countCovering(lat: Double, lon: Double): Int
}
