package com.example.a20_20_20.ui

import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerState
import kotlin.math.ceil

data class TimerUiState(
    val timerState: TimerState = TimerState()
) {
    val formattedTime: String
        get() {
            val totalSeconds = ceil(timerState.remainingTimeMillis / 1000.0).toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val phaseLabel: String
        get() = when (timerState.currentPhase) {
            TimerPhase.WORK -> "ワーク"
            TimerPhase.BREAK -> "ブレイク"
        }

    val cycleInfo: String
        get() = "${timerState.completedCycles}サイクル完了"
}