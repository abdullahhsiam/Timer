package com.example.persistence

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroDao {
    @Query("SELECT * FROM pomodoro_daily_summary ORDER BY date DESC")
    fun getAllDailySummaries(): Flow<List<PomodoroDailySummary>>

    @Query("SELECT * FROM pomodoro_daily_summary WHERE date = :date LIMIT 1")
    suspend fun getDailySummaryByDate(date: String): PomodoroDailySummary?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailySummary(summary: PomodoroDailySummary)

    @Query("SELECT * FROM pomodoro_session_log ORDER BY date DESC, startTime ASC")
    fun getAllSessionLogs(): Flow<List<PomodoroSessionLog>>

    @Query("SELECT * FROM pomodoro_session_log WHERE date = :date ORDER BY startTime ASC")
    fun getSessionLogsForDate(date: String): Flow<List<PomodoroSessionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionLog(log: PomodoroSessionLog)

    @Query("DELETE FROM pomodoro_daily_summary WHERE date < :cutoffDate")
    suspend fun pruneDailySummaries(cutoffDate: String)

    @Query("DELETE FROM pomodoro_session_log WHERE date < :cutoffDate")
    suspend fun pruneSessionLogs(cutoffDate: String)
}
