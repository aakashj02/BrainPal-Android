package com.brainpal.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BrainPalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDailyGoal(goal: DailyGoalEntity)

    @Query("SELECT * FROM daily_goals WHERE id = 1 LIMIT 1")
    fun observeDailyGoal(): Flow<DailyGoalEntity?>

    @Query("SELECT * FROM daily_goals WHERE id = 1 LIMIT 1")
    suspend fun getDailyGoal(): DailyGoalEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultSettings(settings: AppSettingsEntity)

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Insert
    suspend fun insertWatchEvent(event: WatchEventEntity): Long

    @Query("SELECT COUNT(*) FROM watch_events WHERE localDate = :date AND wasAd = 0")
    fun observeCountForDate(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM watch_events WHERE localDate = :date AND wasAd = 0")
    suspend fun countForDate(date: String): Int

    @Query("SELECT localDate, COUNT(*) AS total FROM watch_events WHERE wasAd = 0 GROUP BY localDate")
    fun observeDailyCounts(): Flow<List<DailyCountRow>>

    @Query("SELECT * FROM watch_events ORDER BY watchedAtEpochMillis DESC LIMIT 500")
    fun observeRecentEvents(): Flow<List<WatchEventEntity>>

    @Query("SELECT platform, COUNT(*) AS total FROM watch_events WHERE localDate = :date AND wasAd = 0 GROUP BY platform")
    fun observePlatformCountsForDate(date: String): Flow<List<PlatformCountRow>>

    @Query("SELECT platform, COUNT(*) AS total FROM watch_events WHERE wasAd = 0 GROUP BY platform")
    fun observePlatformLifetimeCounts(): Flow<List<PlatformCountRow>>

    @Query("SELECT * FROM watch_events ORDER BY watchedAtEpochMillis DESC")
    suspend fun getAllWatchEvents(): List<WatchEventEntity>

    @Insert
    suspend fun insertCorrection(correction: CountCorrectionEntity)

    @Query("SELECT COALESCE(SUM(delta), 0) FROM count_corrections WHERE localDate = :date")
    fun observeCorrectionForDate(date: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(delta), 0) FROM count_corrections WHERE localDate = :date")
    suspend fun correctionForDate(date: String): Int
    @Insert
    suspend fun insertDiagnostic(entity: DetectionDiagnosticEntity): Long
}

data class PlatformCountRow(
    val platform: String,
    val total: Int
)

data class DailyCountRow(
    val localDate: String,
    val total: Int
)
