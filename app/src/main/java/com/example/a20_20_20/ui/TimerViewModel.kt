package com.example.a20_20_20.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a20_20_20.TimerApplication
import com.example.a20_20_20.domain.NotificationSettings
import com.example.a20_20_20.domain.TimerSettings
import com.example.a20_20_20.service.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {
    
    private val application = TimerApplication.getInstance()
    
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()
    
    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()
    
    val notificationSettings: StateFlow<NotificationSettings> = application.notificationSettings
    val timerSettings: StateFlow<TimerSettings> = application.timerSettings

    init {
        // アプリケーションからタイマー状態を監視
        viewModelScope.launch {
            application.timerState.collect { timerState ->
                _uiState.value = TimerUiState(timerState = timerState)
            }
        }
        
        // サービスの状態を定期的に更新
        viewModelScope.launch {
            application.getService()?.getTimerState()?.collect { timerState ->
                _uiState.value = TimerUiState(timerState = timerState)
            }
        }
    }

    fun startTimer() {
        // サービスを明示的に開始してフォアグラウンドにする
        val intent = Intent(application, TimerService::class.java)
        intent.action = TimerService.ACTION_START_TIMER
        application.startForegroundService(intent)
    }

    fun pauseTimer() {
        application.pauseTimer()
    }

    fun stopTimer() {
        application.stopTimer()
    }

    fun updateSettings(settings: TimerSettings) {
        application.updateSettings(settings)
    }
    
    fun updateNotificationSettings(settings: NotificationSettings) {
        application.updateNotificationSettings(settings)
    }

    fun navigateToSettings() {
        _showSettings.value = true
    }

    fun navigateBack() {
        _showSettings.value = false
    }
}