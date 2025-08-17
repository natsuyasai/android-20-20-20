package com.example.a20_20_20.service

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
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(dispatcher)
    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0L // タイマー開始時のシステム時刻
    private var pausedTimeMillis: Long = 0L // 一時停止した時間の累計
    private var notificationSettings = NotificationSettings.DEFAULT
    private var isManualStop = false // 手動停止フラグ
    
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    fun start() {
        isManualStop = false // タイマー開始時は手動停止フラグをクリア
        val currentState = _timerState.value
        when (currentState.status) {
            TimerStatus.STOPPED -> {
                // 新規開始
                startTimeMillis = System.currentTimeMillis()
                pausedTimeMillis = 0L
                _timerState.value = currentState.start()
                startCountdown()
            }
            TimerStatus.PAUSED -> {
                // 再開
                val pauseStartTime = startTimeMillis + (currentState.settings.workDurationMillis - currentState.remainingTimeMillis) + pausedTimeMillis
                pausedTimeMillis += System.currentTimeMillis() - pauseStartTime
                _timerState.value = currentState.start()
                startCountdown()
            }
            TimerStatus.RUNNING -> {
                // 既に実行中の場合は何もしない（2重起動防止）
                // ログ出力やデバッグ用（プロダクションでは削除可能）
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        val currentState = _timerState.value
        if (currentState.status == TimerStatus.RUNNING) {
            _timerState.value = currentState.pause()
        }
    }

    fun stop() {
        isManualStop = true // 手動停止フラグを設定
        timerJob?.cancel()
        startTimeMillis = 0L
        pausedTimeMillis = 0L
        val currentState = _timerState.value
        _timerState.value = currentState.stop()
    }

    fun updateSettings(settings: TimerSettings) {
        stop()
        _timerState.value = TimerState(settings = settings)
    }
    
    fun updateNotificationSettings(settings: NotificationSettings) {
        notificationSettings = settings
        // 実行中の場合は更新間隔を反映するためにカウントダウンを再開
        if (_timerState.value.status == TimerStatus.RUNNING) {
            startCountdown()
        }
    }
    
    fun isManuallyStoppedRecently(): Boolean {
        return isManualStop
    }

    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = scope.launch {
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
                    // フェーズ完了 - 即座に処理して次のフェーズに移行
                    handlePhaseCompletion(currentState)
                    // フェーズが切り替わった場合は短時間後に再度更新（遅延なしで次のフェーズを開始）
                    delay(50L) // 50ms後に次のフェーズの更新を開始
                } else {
                    // 時間更新
                    _timerState.value = currentState.copy(remainingTimeMillis = newRemainingTime)
                    
                    // 次の更新間隔を計算（残り時間が短い場合は短い間隔で更新）
                    val baseUpdateInterval = calculateOptimalUpdateInterval(currentState)
                    val nextUpdateInterval = if (newRemainingTime <= 3000) {
                        // 残り3秒以下の場合は500ms間隔で精密に更新
                        500L
                    } else if (newRemainingTime <= 10000) {
                        // 残り10秒以下の場合は1000ms間隔で更新
                        1000L
                    } else {
                        // それ以外は設定された間隔
                        baseUpdateInterval
                    }
                    
                    delay(nextUpdateInterval)
                }
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

    private fun handlePhaseCompletion(currentState: TimerState) {
        val nextState = currentState.nextPhase()
        
        if (nextState.isCompleted()) {
            // 全サイクル完了
            startTimeMillis = 0L
            pausedTimeMillis = 0L
            _timerState.value = nextState.stop()
        } else {
            // 次のフェーズに移行
            // システム時刻の調整は不要（calculatePhaseStartTimeで適切に計算される）
            _timerState.value = nextState
        }
    }
}