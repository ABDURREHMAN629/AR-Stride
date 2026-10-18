package com.stride.fit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [
        DayStat::class,
        MinuteBucket::class,
        RunSession::class,
        RunPoint::class,
        RunSplit::class,
        WorkoutSession::class,
        SetLog::class,
        BodyLog::class,
        RoutineLog::class,
        MapRegion::class
    ],
    version = 2,
    exportSchema = true
)
abstract class StrideDatabase : RoomDatabase() {

    abstract fun dayStatDao(): DayStatDao
    abstract fun minuteBucketDao(): MinuteBucketDao
    abstract fun runDao(): RunDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyDao(): BodyDao
    abstract fun routineDao(): RoutineDao
    abstract fun mapRegionDao(): MapRegionDao

    companion object {
        @Volatile private var instance: StrideDatabase? = null

        /**
         * Offline map regions arrived in version 2. A real migration rather than a
         * destructive one, because by the time maps existed there was already step
         * history worth keeping.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `map_region` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`centerLat` REAL NOT NULL, " +
                        "`centerLon` REAL NOT NULL, " +
                        "`minLat` REAL NOT NULL, " +
                        "`maxLat` REAL NOT NULL, " +
                        "`minLon` REAL NOT NULL, " +
                        "`maxLon` REAL NOT NULL, " +
                        "`minZoom` INTEGER NOT NULL, " +
                        "`maxZoom` INTEGER NOT NULL, " +
                        "`radiusKm` REAL NOT NULL, " +
                        "`tileCount` INTEGER NOT NULL, " +
                        "`bytes` INTEGER NOT NULL, " +
                        "`downloadedAtMs` INTEGER NOT NULL)"
                )
            }
        }

        fun get(context: Context): StrideDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StrideDatabase::class.java,
                    "stride.db"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
