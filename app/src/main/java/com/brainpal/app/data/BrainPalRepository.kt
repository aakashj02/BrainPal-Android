package com.brainpal.app.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class HomeSnapshot(
    val todayCount: Int = 0,
    val yesterdayCount: Int = 0,
    val goal: Int? = null,
    val streak: Int = 0
) {
    val progress: Float
        get() = if (goal == null || goal <= 0) 0f else (todayCount.toFloat() / goal).coerceIn(0f, 1f)
}

class BrainPalRepository(private val dao: BrainPalDao) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    private fun dateString(date: LocalDate = LocalDate.now()): String = date.format(formatter)

    fun observeHome(): Flow<HomeSnapshot> {
        val today = dateString()
        val yesterday = dateString(LocalDate.now().minusDays(1))
        return combine(
            dao.observeCountForDate(today),
            dao.observeCountForDate(yesterday),
            dao.observeCorrectionForDate(today),
            dao.observeCorrectionForDate(yesterday),
            dao.observeDailyGoal(),
            dao.observeDailyCounts()
        ) { values ->
            val todayEvents = values[0] as Int
            val yesterdayEvents = values[1] as Int
            val todayCorrection = values[2] as Int
            val yesterdayCorrection = values[3] as Int
            val goalEntity = values[4] as DailyGoalEntity?
            @Suppress("UNCHECKED_CAST")
            val dailyRows = values[5] as List<DailyCountRow>
            val goal = goalEntity?.reelsTarget
            val counts = dailyRows.associate { it.localDate to it.total }
            var streak = 0
            if (goal != null && goal > 0) {
                var day = LocalDate.now()
                // A streak is measured through today if today's goal is met; otherwise start yesterday.
                if ((counts[dateString(day)] ?: 0) + todayCorrection < goal) day = day.minusDays(1)
                while (((counts[dateString(day)] ?: 0) +
                        if (day == LocalDate.now()) todayCorrection else if (day == LocalDate.now().minusDays(1)) yesterdayCorrection else 0) >= goal) {
                    streak++
                    day = day.minusDays(1)
                    if (streak > 3660) break
                }
            }
            HomeSnapshot(
                todayCount = (todayEvents + todayCorrection).coerceAtLeast(0),
                yesterdayCount = (yesterdayEvents + yesterdayCorrection).coerceAtLeast(0),
                goal = goal,
                streak = streak
            )
        }
    }

    suspend fun setDailyGoal(target: Int) {
        require(target in 1..100_000) { "Goal must be between 1 and 100000." }
        dao.saveDailyGoal(DailyGoalEntity(reelsTarget = target, updatedAtEpochMillis = System.currentTimeMillis()))
        dao.insertDefaultSettings(AppSettingsEntity())
    }

    fun observeRecentEvents(): Flow<List<WatchEventEntity>> = dao.observeRecentEvents()

    suspend fun allEvents(): List<WatchEventEntity> = dao.getAllWatchEvents()
}
