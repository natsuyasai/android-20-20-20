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
    private var pausedTimeMillis: Long = 0L // 一時停止した時間の累計
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
                pausedTimeMillis = 0L
                _timerState.value = currentState.start()
                startAlarmBasedTimer()
            }
            TimerStatus.PAUSED -> {
                // 再開
                val pauseStartTime = startTimeMillis + (currentState.settings.workDurationMillis - currentState.remainingTimeMillis) + pausedTimeMillis
                pausedTimeMillis += System.currentTimeMillis() - pauseStartTime
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
            _timerState.value = currentState.pause()
            android.util.Log.d("TimerEngine", "Timer paused")
        }
    }

    fun stop() {
        isManualStop = true // 手動停止フラグを設定
        cancelAlarms()
        updateTimerJob?.cancel()
        startTimeMillis = 0L
        pausedTimeMillis = 0L
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
                
                // システム時刻ベースで経過時間を計算
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - startTimeMillis - pausedTimeMillis
                
                // 現在のフェーズの開始時間を計算
                val phaseStartTime = calculatePhaseStartTime(currentState)
                val phaseElapsedTime = elapsedTime - phaseStartTime
                
                val phaseDuration = when (currentState.currentPhase) {
                    TimerPhase.WORK -> currentState.settings.workDurationMillis
                    TimerPhase.BREAK -> currentState.settings.breakDurationMillis
                }
                
                val newRemainingTime = phaseDuration - phaseElapsedTime
                
                if (newRemainingTime <= 0) {
                    // フェーズ完了はAlarmManagerで処理されるため、ここでは状態を0に設定
                    _timerState.value = currentState.copy(remainingTimeMillis = 0L)
                    break
                } else {
                    // 時間更新
                    _timerState.value = currentState.copy(remainingTimeMillis = newRemainingTime)
                }
                
                // AlarmManagerが正確なフェーズ完了を管理するため、
                // UI更新は一定間隔で十分
                val nextUpdateInterval = calculateOptimalUpdateInterval(currentState)
                
                delay(nextUpdateInterval)
            }
        }
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
    
    private fun calculatePhaseStartTime(state: TimerState): Long {
        // 現在のサイクルまでの累積時間を計算
        val completedCyclesTime = state.completedCycles * (state.settings.workDurationMillis + state.settings.breakDurationMillis)
        
        // 現在のサイクル内でのフェーズ開始時間
        val currentCyclePhaseTime = when (state.currentPhase) {
            TimerPhase.WORK -> 0L
            TimerPhase.BREAK -> state.settings.workDurationMillis
        }
        
        return completedCyclesTime + currentCyclePhaseTime
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