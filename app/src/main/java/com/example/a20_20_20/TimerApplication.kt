package com.example.a20_20_20

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.a20_20_20.domain.TimerSettings
import com.example.a20_20_20.domain.TimerState
import com.example.a20_20_20.service.TimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerApplication : Application() {
    
    private var timerService: TimerService? = null
    private var isBound = false
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.TimerServiceBinder
            timerService = binder.getService()
            isBound = true
            
            // サービスの状態をアプリケーション全体で共有
            applicationScope.launch {
                timerService?.getTimerState()?.collect { serviceState ->
                    _timerState.value = serviceState
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            timerService = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        bindToTimerService()
    }

    private fun bindToTimerService() {
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
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

    fun getService(): TimerService? = timerService

    companion object {
        @Volatile
        private var instance: TimerApplication? = null

        fun getInstance(): TimerApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}