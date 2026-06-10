package com.example

enum class TimerStatus {
    IDLE, RUNNING, PAUSED, FINISHED
}

enum class StopwatchStatus {
    IDLE, RUNNING, PAUSED
}

data class LapRecord(
    val index: Int,
    val lapTimeMs: Long,
    val totalTimeMs: Long
)
