package com.example.a20_20_20.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a20_20_20.domain.TimerSettings
import com.example.a20_20_20.service.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel(private val context: Context) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()
    
    private var timerService: TimerService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.TimerServiceBinder
            timerService = binder.getService()
            isBound = true
            
            // サービスからタイマー状態を監視
            viewModelScope.launch {
                timerService?.getTimerState()?.collect { timerState ->
                    _uiState.value = TimerUiState(timerState = timerState)
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            timerService = null
        }
    }

    init {
        bindService()
    }

    private fun bindService() {
        Intent(context, TimerService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun startTimer() {
        timerService?.startTimerEngine()
    }

    fun pauseTimer() {
        timerService?.pauseTimerEngine()
    }

    fun stopTimer() {
        timerService?.stopTimerEngine()
    }

    fun updateSettings(settings: TimerSettings) {
        timerService?.updateTimerSettings(settings)
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }
}