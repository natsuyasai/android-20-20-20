package com.example.a20_20_20.service

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
    
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    fun start() {
        val currentState = _timerState.value
        if (currentState.status == TimerStatus.STOPPED || currentState.status == TimerStatus.PAUSED) {
            _timerState.value = currentState.start()
            startCountdown()
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
        timerJob?.cancel()
        val currentState = _timerState.value
        _timerState.value = currentState.stop()
    }

    fun updateSettings(settings: TimerSettings) {
        stop()
        _timerState.value = TimerState(settings = settings)
    }

    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_timerState.value.status == TimerStatus.RUNNING) {
                delay(1000L) // 1秒間隔で更新
                
                val currentState = _timerState.value
                if (currentState.status != TimerStatus.RUNNING) break
                
                val newRemainingTime = currentState.remainingTimeMillis - 1000L
                
                if (newRemainingTime <= 0) {
                    // フェーズ完了
                    handlePhaseCompletion(currentState)
                } else {
                    // 時間更新
                    _timerState.value = currentState.copy(remainingTimeMillis = newRemainingTime)
                }
            }
        }
    }

    private fun handlePhaseCompletion(currentState: TimerState) {
        val nextState = currentState.nextPhase()
        
        if (nextState.isCompleted()) {
            // 全サイクル完了
            _timerState.value = nextState.stop()
        } else {
            // 次のフェーズに移行
            _timerState.value = nextState
        }
    }
}