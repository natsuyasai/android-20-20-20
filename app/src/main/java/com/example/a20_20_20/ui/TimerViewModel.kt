package com.example.a20_20_20.ui

import androidx.lifecycle.ViewModel
import com.example.a20_20_20.domain.TimerSettings
import com.example.a20_20_20.domain.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    fun startTimer() {
        _uiState.value = _uiState.value.copy(
            timerState = _uiState.value.timerState.start()
        )
    }

    fun pauseTimer() {
        _uiState.value = _uiState.value.copy(
            timerState = _uiState.value.timerState.pause()
        )
    }

    fun stopTimer() {
        _uiState.value = _uiState.value.copy(
            timerState = _uiState.value.timerState.stop()
        )
    }

    fun updateSettings(settings: TimerSettings) {
        val newTimerState = TimerState(settings = settings)
        _uiState.value = _uiState.value.copy(
            timerState = newTimerState
        )
    }
}