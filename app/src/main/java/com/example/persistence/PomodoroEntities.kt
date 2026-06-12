package com.example.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_daily_summary")
data class PomodoroDailySummary(
    @PrimaryKey val date: String, // Format: "yyyy-MM-dd"
    val focusTimeMs: Long = 0L,
    val breakTimeMs: Long = 0L,
    val focusSessionsCompleted: Int = 0,
    val breakSessionsCompleted: Int = 0,
    val manualBreakCount: Int = 0,
    val totalFocusHours: Double = 0.0
)

@Entity(tableName = "pomodoro_session_log")
data class PomodoroSessionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String, // Format: "yyyy-MM-dd"
    val startTime: Long, // Wall clock timestamp
    val endTime: Long, // Wall clock timestamp
    val durationMs: Long,
    val sessionType: String // "FOCUS", "BREAK", "MANUAL_BREAK"
)
