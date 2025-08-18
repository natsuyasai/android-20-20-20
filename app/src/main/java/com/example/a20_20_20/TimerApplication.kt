package com.example.a20_20_20

import android.app.AlarmManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import com.example.a20_20_20.data.SettingsRepository
import com.example.a20_20_20.domain.NotificationSettings
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
    private lateinit var settingsRepository: SettingsRepository
    
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    private val _notificationSettings = MutableStateFlow(NotificationSettings.DEFAULT)
    val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings.asStateFlow()
    
    private val _timerSettings = MutableStateFlow<TimerSettings>(TimerSettings.DEFAULT)
    val timerSettings: StateFlow<TimerSettings> = _timerSettings.asStateFlow()
    
    private val _exactAlarmPermissionGranted = MutableStateFlow(false)
    val exactAlarmPermissionGranted: StateFlow<Boolean> = _exactAlarmPermissionGranted.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.TimerServiceBinder
            timerService = binder.getService()
            isBound = true
            
            // 保存された設定をサービスに反映
            timerService?.updateTimerSettings(_timerSettings.value)
            timerService?.updateNotificationSettings(_notificationSettings.value)
            
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
        
        // 設定リポジトリの初期化
        settingsRepository = SettingsRepository(this)
        
        // 保存された設定を読み込み
        loadSettings()
        
        // 権限状態をチェック
        checkExactAlarmPermission()
        
        bindToTimerService()
    }
    
    private fun loadSettings() {
        val savedTimerSettings = settingsRepository.loadTimerSettings()
        val savedNotificationSettings = settingsRepository.loadNotificationSettings()
        
        _timerSettings.value = savedTimerSettings
        _notificationSettings.value = savedNotificationSettings
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
        _timerSettings.value = settings
        settingsRepository.saveTimerSettings(settings)
        timerService?.updateTimerSettings(settings)
    }
    
    fun updateNotificationSettings(settings: NotificationSettings) {
        _notificationSettings.value = settings
        settingsRepository.saveNotificationSettings(settings)
        timerService?.updateNotificationSettings(settings)
    }

    fun getService(): TimerService? = timerService
    
    fun restoreNotificationIfNeeded() {
        // サービスが接続されている場合は直接復元
        timerService?.restoreNotification()
            ?: run {
                // サービスが接続されていない場合は再接続を試行
                android.util.Log.d("TimerApplication", "Service not connected, attempting to reconnect")
                bindToTimerService()
            }
    }

    companion object {
        @Volatile
        private var instance: TimerApplication? = null

        fun getInstance(): TimerApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
    
    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            _exactAlarmPermissionGranted.value = alarmManager.canScheduleExactAlarms()
        } else {
            // Android 11以前では権限不要
            _exactAlarmPermissionGranted.value = true
        }
    }
    
    fun updateExactAlarmPermissionState() {
        checkExactAlarmPermission()
    }
}