package com.brainpal.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WatchEventEntity::class,
        DailyGoalEntity::class,
        AppSettingsEntity::class,
        CountCorrectionEntity::class,
        DetectionDiagnosticEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BrainPalDatabase : RoomDatabase() {
    abstract fun brainPalDao(): BrainPalDao

    companion object {
        @Volatile private var INSTANCE: BrainPalDatabase? = null

        fun getInstance(context: Context): BrainPalDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BrainPalDatabase::class.java,
                    "brain_pal_local.db"
                ).build().also { INSTANCE = it }
            }
    }
}
