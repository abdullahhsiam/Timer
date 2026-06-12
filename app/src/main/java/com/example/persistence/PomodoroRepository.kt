package com.example.persistence

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class PomodoroRepository(private val pomodoroDao: PomodoroDao) {
    val allDailySummaries: Flow<List<PomodoroDailySummary>> = pomodoroDao.getAllDailySummaries()
    val allSessionLogs: Flow<List<PomodoroSessionLog>> = pomodoroDao.getAllSessionLogs()

    fun getSessionLogsForDate(date: String): Flow<List<PomodoroSessionLog>> =
        pomodoroDao.getSessionLogsForDate(date)

    suspend fun getDailySummaryByDate(date: String): PomodoroDailySummary? =
        pomodoroDao.getDailySummaryByDate(date)

    suspend fun insertOrUpdateDailySummary(summary: PomodoroDailySummary) =
        pomodoroDao.insertOrUpdateDailySummary(summary)

    suspend fun insertSessionLog(log: PomodoroSessionLog) =
        pomodoroDao.insertSessionLog(log)

    suspend fun pruneOlderThan30Days() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val cutoffDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        pomodoroDao.pruneDailySummaries(cutoffDate)
        pomodoroDao.pruneSessionLogs(cutoffDate)
    }

    suspend fun recordSession(
        type: String,
        startTime: Long,
        endTime: Long,
        durationMs: Long
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(endTime))
        
        // 1. Insert session log
        val log = PomodoroSessionLog(
            date = today,
            startTime = startTime,
            endTime = endTime,
            durationMs = durationMs,
            sessionType = type
        )
        pomodoroDao.insertSessionLog(log)

        // 2. Load and increment today's daily summary (as fallback metric persistence)
        val existing = pomodoroDao.getDailySummaryByDate(today) ?: PomodoroDailySummary(date = today)
        
        val isFocus = type == "FOCUS"
        val isBreak = type == "BREAK"
        val isManualBreak = type == "MANUAL_BREAK"

        val updatedFocusTime = existing.focusTimeMs + (if (isFocus) durationMs else 0L)
        val updatedBreakTime = existing.breakTimeMs + (if (isBreak || isManualBreak) durationMs else 0L)
        
        val updatedFocusSessions = existing.focusSessionsCompleted + (if (isFocus) 1 else 0)
        val updatedBreakSessions = existing.breakSessionsCompleted + (if (isBreak) 1 else 0)
        val updatedManualBreaks = existing.manualBreakCount + (if (isManualBreak) 1 else 0)
        val updatedTotalHours = updatedFocusTime / 3600000.0

        val newSummary = PomodoroDailySummary(
            date = today,
            focusTimeMs = updatedFocusTime,
            breakTimeMs = updatedBreakTime,
            focusSessionsCompleted = updatedFocusSessions,
            breakSessionsCompleted = updatedBreakSessions,
            manualBreakCount = updatedManualBreaks,
            totalFocusHours = updatedTotalHours
        )
        pomodoroDao.insertOrUpdateDailySummary(newSummary)

        // 3. Prune old records to preserve up to 30 days of history
        pruneOlderThan30Days()
    }
}
