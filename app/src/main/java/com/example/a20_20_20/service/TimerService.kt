package com.example.a20_20_20.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import com.example.a20_20_20.domain.NotificationSettings
import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerSettings
import com.example.a20_20_20.domain.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerService : Service() {
    
    private val binder = TimerServiceBinder()
    private lateinit var timerEngine: TimerEngine
    private lateinit var notificationManager: TimerNotificationManager
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        const val ACTION_START_TIMER = "com.example.a20_20_20.START_TIMER"
        const val ACTION_PAUSE_TIMER = "com.example.a20_20_20.PAUSE_TIMER"
        const val ACTION_STOP_TIMER = "com.example.a20_20_20.STOP_TIMER"
    }

    override fun onCreate() {
        super.onCreate()
        timerEngine = TimerEngine()
        notificationManager = TimerNotificationManager(this)
        
        // ウェイクロックを取得
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TimerApp::TimerWakeLock"
        )
        
        // タイマー状態の変化を監視して通知を更新
        serviceScope.launch {
            timerEngine.timerState.collect { state ->
                updateNotification(state)
                handlePhaseCompletion(state)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        notificationManager.cleanup()
        // ウェイクロックを解除
        wakeLock?.let { wl ->
            if (wl.isHeld) {
                wl.release()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> startTimer()
            ACTION_PAUSE_TIMER -> pauseTimer()
            ACTION_STOP_TIMER -> stopTimer()
        }
        return START_STICKY // サービスが強制終了されても再起動
    }

    private fun startTimer() {
        // ウェイクロックを取得してタイマーが画面オフでも動作するようにする
        wakeLock?.let { wl ->
            if (!wl.isHeld) {
                wl.acquire(10*60*1000L /*10 minutes*/)
            }
        }
        
        timerEngine.start()
        val notification = notificationManager.createTimerNotification(timerEngine.timerState.value)
        startForeground(TimerNotificationManager.NOTIFICATION_ID, notification)
    }

    private fun pauseTimer() {
        timerEngine.pause()
    }

    private fun stopTimer() {
        timerEngine.stop()
        
        // ウェイクロックを解除
        wakeLock?.let { wl ->
            if (wl.isHeld) {
                wl.release()
            }
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(state: TimerState) {
        if (state.status != com.example.a20_20_20.domain.TimerStatus.STOPPED) {
            val notification = notificationManager.createTimerNotification(state)
            notificationManager.notify(TimerNotificationManager.NOTIFICATION_ID, notification)
        }
    }

    private var lastCompletedPhase: TimerPhase? = null
    private var lastRemainingTime: Long = Long.MAX_VALUE

    private fun handlePhaseCompletion(state: TimerState) {
        // フェーズが変わった場合のみ通知音を鳴らす
        if (state.remainingTimeMillis > lastRemainingTime) {
            // 残り時間が増加した = フェーズが変わった
            lastCompletedPhase?.let { completedPhase ->
                notificationManager.showPhaseCompletionNotification(completedPhase)
            }
            lastCompletedPhase = state.currentPhase
        }
        lastRemainingTime = state.remainingTimeMillis
    }

    inner class TimerServiceBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    // ViewModelから呼び出すためのメソッド
    fun getTimerState(): StateFlow<TimerState> = timerEngine.timerState
    
    fun updateTimerSettings(settings: TimerSettings) {
        timerEngine.updateSettings(settings)
    }
    
    fun updateNotificationSettings(settings: NotificationSettings) {
        notificationManager.updateSettings(settings)
    }
    
    fun startTimerEngine() {
        startTimer()
    }
    
    fun pauseTimerEngine() {
        pauseTimer()
    }
    
    fun stopTimerEngine() {
        stopTimer()
    }
}