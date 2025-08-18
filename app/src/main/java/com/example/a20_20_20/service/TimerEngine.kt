package com.example.a20_20_20.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.a20_20_20.domain.NotificationSettings
import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerSettings
import com.example.a20_20_20.domain.TimerState
import com.example.a20_20_20.domain.TimerStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerEngine(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(dispatcher)
    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0L // タイマー開始時のシステム時刻
    private var notificationSettings = NotificationSettings.DEFAULT
    private var isManualStop = false // 手動停止フラグ
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private var currentAlarmIntent: PendingIntent? = null
    private var updateTimerJob: Job? = null // UI更新用のCoroutine
    
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    companion object {
        const val ACTION_TIMER_TICK = "com.example.a20_20_20.TIMER_TICK"
        const val ACTION_PHASE_COMPLETE = "com.example.a20_20_20.PHASE_COMPLETE"
        private const val REQUEST_CODE_TIMER_TICK = 1001
        private const val REQUEST_CODE_PHASE_COMPLETE = 1002
    }

    fun start() {
        isManualStop = false // タイマー開始時は手動停止フラグをクリア
        val currentState = _timerState.value
        when (currentState.status) {
            TimerStatus.STOPPED -> {
                // 新規開始
                startTimeMillis = System.currentTimeMillis()
                _timerState.value = currentState.start()
                startAlarmBasedTimer()
            }
            TimerStatus.PAUSED -> {
                // 再開：一時停止からの再開では、残り時間をそのまま維持
                // 一時停止した時点の状態を復元するだけで十分
                _timerState.value = currentState.start()
                startAlarmBasedTimer()
            }
            TimerStatus.RUNNING -> {
                // 既に実行中の場合は何もしない（2重起動防止）
                android.util.Log.d("TimerEngine", "Timer already running, ignoring start request")
            }
        }
    }

    fun pause() {
        cancelAlarms()
        updateTimerJob?.cancel()
        val currentState = _timerState.value
        if (currentState.status == TimerStatus.RUNNING) {
            // AlarmManagerベースでは、一時停止時は現在の残り時間をそのまま保持
            // AlarmManagerが設定した正確な残り時間から独自計算は不要
            _timerState.value = currentState.copy(
                status = TimerStatus.PAUSED
            )
            android.util.Log.d("TimerEngine", "Timer paused with remaining: ${currentState.remainingTimeMillis}ms")
        }
    }

    fun stop() {
        isManualStop = true // 手動停止フラグを設定
        cancelAlarms()
        updateTimerJob?.cancel()
        startTimeMillis = 0L
        val currentState = _timerState.value
        _timerState.value = currentState.stop()
        android.util.Log.d("TimerEngine", "Timer stopped")
    }

    fun updateSettings(settings: TimerSettings) {
        stop()
        _timerState.value = TimerState(settings = settings)
    }
    
    fun updateNotificationSettings(settings: NotificationSettings) {
        notificationSettings = settings
        // 実行中の場合は更新間隔を反映するためにUI更新を再開
        if (_timerState.value.status == TimerStatus.RUNNING) {
            startUIUpdateTimer()
        }
    }
    
    fun isManuallyStoppedRecently(): Boolean {
        return isManualStop
    }

    private fun startAlarmBasedTimer() {
        cancelAlarms()
        
        val currentState = _timerState.value
        val phaseDuration = when (currentState.currentPhase) {
            TimerPhase.WORK -> currentState.settings.workDurationMillis
            TimerPhase.BREAK -> currentState.settings.breakDurationMillis
        }
        
        // フェーズ完了のアラームを設定
        val phaseCompleteTime = System.currentTimeMillis() + currentState.remainingTimeMillis
        schedulePhaseCompleteAlarm(phaseCompleteTime)
        
        // UI更新用のタイマーを開始
        startUIUpdateTimer()
        
        android.util.Log.d("TimerEngine", "AlarmManager-based timer started, phase will complete in ${currentState.remainingTimeMillis}ms")
    }
    
    private fun startUIUpdateTimer() {
        updateTimerJob?.cancel()
        updateTimerJob = scope.launch {
            while (_timerState.value.status == TimerStatus.RUNNING) {
                val currentState = _timerState.value
                if (currentState.status != TimerStatus.RUNNING) break
                
                // AlarmManagerベースでは、UI更新のみを行い、
                // フェーズ完了の判定はAlarmManagerに委ねる
                updateRemainingTimeForDisplay(currentState)
                
                val nextUpdateInterval = calculateOptimalUpdateInterval(currentState)
                delay(nextUpdateInterval)
            }
        }
    }
    
    private fun updateRemainingTimeForDisplay(currentState: TimerState) {
        // AlarmManagerベースでは、正確な残り時間はフェーズ完了時まで維持される
        // UI更新は主に通知やUI表示の同期のためのものであり、時間計算は行わない
        // 実際の残り時間の更新はAlarmManagerからのフェーズ完了時のみ実行される
        
        // 必要に応じて将来のUI表示用の軽微な更新ロジックをここに追加可能
        // 現在は残り時間の独自計算は実行せず、AlarmManagerに委ねる
    }
    
    private fun calculateOptimalUpdateInterval(state: TimerState): Long {
        val baseInterval = notificationSettings.updateInterval.intervalMillis
        
        // ブレイクフェーズの場合のみ特別処理
        if (state.currentPhase == TimerPhase.BREAK) {
            val breakDuration = state.settings.breakDurationMillis
            
            // 更新間隔がブレイク時間より長い場合はブレイク時間を使用
            if (baseInterval > breakDuration) {
                return breakDuration
            }
        }
        
        return baseInterval
    }
    

    private fun schedulePhaseCompleteAlarm(triggerTime: Long) {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = ACTION_PHASE_COMPLETE
        }
        
        currentAlarmIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PHASE_COMPLETE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            // Android 12以降では権限チェックが必要
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    android.util.Log.w("TimerEngine", "Cannot schedule exact alarms, falling back to inexact alarm")
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, currentAlarmIntent!!)
                    return
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0以降では正確なアラームのためsetExactAndAllowWhileIdleを使用
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    currentAlarmIntent!!
                )
            } else {
                // Android 6.0未満ではsetExactを使用
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    currentAlarmIntent!!
                )
            }
            android.util.Log.d("TimerEngine", "Phase complete alarm scheduled for: $triggerTime")
        } catch (e: SecurityException) {
            android.util.Log.e("TimerEngine", "Failed to set exact alarm, falling back to inexact alarm", e)
            // 正確なアラームに失敗した場合は通常のアラームを使用
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, currentAlarmIntent!!)
        }
    }
    
    private fun cancelAlarms() {
        currentAlarmIntent?.let { pendingIntent ->
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            android.util.Log.d("TimerEngine", "Alarms cancelled")
        }
        currentAlarmIntent = null
    }
    
    fun handlePhaseCompletion() {
        val currentState = _timerState.value
        android.util.Log.d("TimerEngine", "Handling phase completion for: ${currentState.currentPhase}")
        
        val nextState = currentState.nextPhase()
        
        if (nextState.isCompleted()) {
            // 全サイクル完了
            stop()
        } else {
            // 次のフェーズに移行
            _timerState.value = nextState
            
            // 新しいフェーズのアラームを設定
            if (nextState.status == TimerStatus.RUNNING) {
                startAlarmBasedTimer()
            }
        }
    }
}