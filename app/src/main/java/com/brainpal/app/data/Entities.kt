package com.brainpal.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_events")
data class WatchEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String,
    val watchedAtEpochMillis: Long,
    val localDate: String,
    val contentKey: String? = null,
    val wasAd: Boolean = false
)

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey val id: Int = 1,
    val reelsTarget: Int,
    val updatedAtEpochMillis: Long
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val remindersEnabled: Boolean = true,
    val reminderInterval: Int = 5
)

@Entity(tableName = "count_corrections")
data class CountCorrectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localDate: String,
    val delta: Int,
    val note: String = "",
    val createdAtEpochMillis: Long
)

@Entity(tableName = "detection_diagnostics")
data class DetectionDiagnosticEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtEpochMillis: Long,
    val platform: String,
    val message: String
)
