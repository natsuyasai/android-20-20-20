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
        
        // ウェイクロックを取得（画面OFFを許容するPARTIAL_WAKE_LOCK）
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
        // 既に実行中の場合は2重起動を防ぐ
        val currentState = timerEngine.timerState.value
        if (currentState.status == com.example.a20_20_20.domain.TimerStatus.RUNNING) {
            // 既に実行中の場合は通知のみ更新
            val notification = notificationManager.createTimerNotification(currentState)
            startForeground(TimerNotificationManager.NOTIFICATION_ID, notification)
            return
        }
        
        // ウェイクロックを取得してタイマーが画面オフでも動作するようにする
        wakeLock?.let { wl ->
            if (!wl.isHeld) {
                // タイマーの残り時間に基づいて適切な時間でウェイクロックを取得
                val totalRemainingTime = calculateTotalRemainingTime(currentState)
                // 最低10分、最大2時間の制限を設ける
                val wakeLockDuration = totalRemainingTime.coerceIn(10*60*1000L, 2*60*60*1000L)
                wl.acquire(wakeLockDuration)
            }
        }
        
        timerEngine.start()
        val notification = notificationManager.createTimerNotification(timerEngine.timerState.value)
        startForeground(TimerNotificationManager.NOTIFICATION_ID, notification)
    }
    
    private fun calculateTotalRemainingTime(state: TimerState): Long {
        // 現在のフェーズの残り時間 + 残りサイクル数に基づく概算時間
        val currentPhaseRemaining = state.remainingTimeMillis
        val remainingCycles = if (state.settings.isUnlimitedRepeat()) {
            // 無制限の場合は1時間分として計算
            3 // 約1時間分のサイクル
        } else {
            // 現在のサイクル番号 = completedCycles + 1（ワークフェーズ）またはcompletedCycles（ブレイクフェーズ）
            val currentCycle = state.completedCycles + (if (state.currentPhase == TimerPhase.WORK) 1 else 0)
            state.settings.repeatCount - currentCycle
        }
        
        val cycleTime = state.settings.workDurationMillis + state.settings.breakDurationMillis
        return currentPhaseRemaining + (remainingCycles * cycleTime)
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

    private var lastObservedPhase: TimerPhase? = null
    private var lastObservedCycle: Int = 0
    private var lastRemainingTime: Long = Long.MAX_VALUE

    private fun handlePhaseCompletion(state: TimerState) {
        // 手動停止の場合は通知しない
        if (timerEngine.isManuallyStoppedRecently()) {
            return
        }
        
        // フェーズまたはサイクルが変わった場合に通知音を鳴らす
        val currentPhase = state.currentPhase
        val currentCycle = state.completedCycles
        
        if (lastObservedPhase != null && 
            (lastObservedPhase != currentPhase || lastObservedCycle != currentCycle)) {
            // フェーズまたはサイクルが変わった = 前のフェーズが完了した
            notificationManager.showPhaseCompletionNotification(lastObservedPhase!!)
        }
        
        lastObservedPhase = currentPhase
        lastObservedCycle = currentCycle
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
        timerEngine.updateNotificationSettings(settings)
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