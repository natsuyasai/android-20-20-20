package com.example.a20_20_20.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.service.notification.StatusBarNotification
import com.example.a20_20_20.domain.NotificationSettings
import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerSettings
import com.example.a20_20_20.domain.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerService : Service() {
    
    private val binder = TimerServiceBinder()
    private lateinit var timerEngine: TimerEngine
    private lateinit var notificationManager: TimerNotificationManager
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null
    private var notificationCheckJob: kotlinx.coroutines.Job? = null
    private var lastRestoreTime = 0L
    
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
                
                // タイマーが動作中の場合は通知監視を開始
                if (state.status != com.example.a20_20_20.domain.TimerStatus.STOPPED) {
                    startNotificationMonitoring()
                } else {
                    stopNotificationMonitoring()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNotificationMonitoring()
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
            // フォアグラウンド通知として更新（削除されても復旧）
            startForeground(TimerNotificationManager.NOTIFICATION_ID, notification)
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
    
    fun restoreNotification() {
        val currentState = timerEngine.timerState.value
        android.util.Log.d("TimerService", "Restoring notification for state: ${currentState.status}")
        
        // 停止状態以外の場合は通知を復旧
        if (currentState.status != com.example.a20_20_20.domain.TimerStatus.STOPPED) {
            try {
                // 通知チャンネルが正しく設定されていることを確認
                val notification = notificationManager.createTimerNotification(currentState)
                
                // フォアグラウンドサービスとして通知を表示
                startForeground(TimerNotificationManager.NOTIFICATION_ID, notification)
                
                android.util.Log.d("TimerService", "Notification restored successfully with ID: ${TimerNotificationManager.NOTIFICATION_ID}")
            } catch (e: Exception) {
                android.util.Log.e("TimerService", "Failed to restore notification", e)
                
                // 復元に失敗した場合は、サービスの状態を正常化
                try {
                    // 通知設定をリセットして再試行
                    notificationManager.updateSettings(notificationManager.getCurrentNotificationSettings())
                    val notification = notificationManager.createTimerNotification(currentState)
                    startForeground(TimerNotificationManager.NOTIFICATION_ID, notification)
                    android.util.Log.d("TimerService", "Notification restored after reset")
                } catch (retryException: Exception) {
                    android.util.Log.e("TimerService", "Failed to restore notification even after reset", retryException)
                }
            }
        } else {
            android.util.Log.d("TimerService", "Timer is stopped, no notification to restore")
        }
    }
    
    private fun startNotificationMonitoring() {
        // 既に監視中の場合は重複起動を防ぐ
        if (notificationCheckJob?.isActive == true) {
            return
        }
        
        android.util.Log.d("TimerService", "Starting notification monitoring")
        notificationCheckJob = serviceScope.launch {
            while (true) {
                val currentState = timerEngine.timerState.value
                if (currentState.status == com.example.a20_20_20.domain.TimerStatus.STOPPED) {
                    android.util.Log.d("TimerService", "Timer stopped, ending notification monitoring")
                    break
                }
                
                // タイマーの更新間隔と同期して通知存在をチェック
                val notificationSettings = notificationManager.getCurrentNotificationSettings()
                val checkInterval = calculateNotificationCheckInterval(notificationSettings.updateInterval.intervalMillis, currentState.remainingTimeMillis)
                
                android.util.Log.d("TimerService", "Checking notification with interval: ${checkInterval}ms")
                delay(checkInterval)
                
                // 通知が削除されているかチェック
                if (!isNotificationVisible()) {
                    val currentTime = System.currentTimeMillis()
                    // 最後の復元から更新間隔の2倍以上経過している場合のみ復元（連続復元を防ぐ）
                    val cooldownPeriod = notificationSettings.updateInterval.intervalMillis * 2
                    if (currentTime - lastRestoreTime > cooldownPeriod) {
                        android.util.Log.w("TimerService", "Notification was dismissed by user, restoring...")
                        restoreNotification()
                        lastRestoreTime = currentTime
                    } else {
                        android.util.Log.d("TimerService", "Notification restore skipped due to rate limiting (cooldown: ${cooldownPeriod}ms)")
                    }
                }
            }
        }
    }
    
    private fun stopNotificationMonitoring() {
        notificationCheckJob?.cancel()
        notificationCheckJob = null
        android.util.Log.d("TimerService", "Stopped notification monitoring")
    }
    
    private fun calculateNotificationCheckInterval(baseInterval: Long, remainingTimeMillis: Long): Long {
        return when {
            // 残り時間が3秒以下の場合は500ms間隔で高頻度チェック
            remainingTimeMillis <= 3000 -> 500L
            // 残り時間が10秒以下の場合は1秒間隔でチェック
            remainingTimeMillis <= 10000 -> 1000L
            // それ以外はタイマーの更新間隔と同じ頻度でチェック
            else -> baseInterval
        }
    }
    
    private fun isNotificationVisible(): Boolean {
        return try {
            // NotificationManagerから自アプリの通知を確認
            val systemNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            // Android 6.0以降では、アクティブな通知を確認できる
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val activeNotifications: Array<StatusBarNotification> = systemNotificationManager.activeNotifications
                val hasTimerNotification = activeNotifications.any { notification ->
                    notification.id == TimerNotificationManager.NOTIFICATION_ID
                }
                
                android.util.Log.d("TimerService", "Checking notification visibility: found=${hasTimerNotification}, total active=${activeNotifications.size}")
                return hasTimerNotification
            } else {
                // Android 6.0未満では直接確認できないため、状態ベースで判断
                val currentState = timerEngine.timerState.value
                val shouldBeVisible = currentState.status != com.example.a20_20_20.domain.TimerStatus.STOPPED
                android.util.Log.d("TimerService", "Legacy notification check: should be visible=${shouldBeVisible}")
                return shouldBeVisible
            }
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Error checking notification visibility", e)
            // エラーの場合は安全側に倒して復元を試行
            return false
        }
    }
}