package com.example.a20_20_20.domain

import org.junit.Test
import org.junit.Assert.*

class TimerStateTest {

    @Test
    fun `初期状態は停止状態でワークフェーズ`() {
        val state = TimerState()
        
        assertEquals(TimerPhase.WORK, state.currentPhase)
        assertEquals(TimerStatus.STOPPED, state.status)
        assertEquals(0, state.completedCycles)
    }

    @Test
    fun `残り時間がワーク時間と一致する`() {
        val settings = TimerSettings(workDurationMillis = 1200000L) // 20分
        val state = TimerState(settings = settings)
        
        assertEquals(1200000L, state.remainingTimeMillis)
    }

    @Test
    fun `タイマー開始時にステータスが実行中になる`() {
        val state = TimerState()
        val startedState = state.start()
        
        assertEquals(TimerStatus.RUNNING, startedState.status)
    }

    @Test
    fun `タイマー一時停止時にステータスが一時停止になる`() {
        val state = TimerState().start()
        val pausedState = state.pause()
        
        assertEquals(TimerStatus.PAUSED, pausedState.status)
    }

    @Test
    fun `タイマー停止時にステータスが停止になり初期状態にリセット`() {
        val settings = TimerSettings(workDurationMillis = 1200000L)
        val state = TimerState(settings = settings)
            .start()
            .copy(remainingTimeMillis = 600000L) // 半分経過
        
        val stoppedState = state.stop()
        
        assertEquals(TimerStatus.STOPPED, stoppedState.status)
        assertEquals(TimerPhase.WORK, stoppedState.currentPhase)
        assertEquals(1200000L, stoppedState.remainingTimeMillis)
        assertEquals(0, stoppedState.completedCycles)
    }

    @Test
    fun `ワークフェーズからブレイクフェーズに移行`() {
        val settings = TimerSettings(breakDurationMillis = 20000L)
        val state = TimerState(
            currentPhase = TimerPhase.WORK,
            settings = settings
        )
        
        val nextState = state.nextPhase()
        
        assertEquals(TimerPhase.BREAK, nextState.currentPhase)
        assertEquals(20000L, nextState.remainingTimeMillis)
    }

    @Test
    fun `ブレイクフェーズからワークフェーズに移行してサイクル完了数が増加`() {
        val settings = TimerSettings(workDurationMillis = 1200000L)
        val state = TimerState(
            currentPhase = TimerPhase.BREAK,
            completedCycles = 0,
            settings = settings
        )
        
        val nextState = state.nextPhase()
        
        assertEquals(TimerPhase.WORK, nextState.currentPhase)
        assertEquals(1200000L, nextState.remainingTimeMillis)
        assertEquals(1, nextState.completedCycles)
    }

    @Test
    fun `最大サイクル数に達した場合にタイマーが完了状態になる`() {
        val settings = TimerSettings(repeatCount = 2)
        val state = TimerState(
            currentPhase = TimerPhase.BREAK,
            completedCycles = 2,
            settings = settings
        )
        
        assertTrue(state.isCompleted())
    }

    @Test
    fun `無制限リピートの場合は完了状態にならない`() {
        val settings = TimerSettings() // デフォルトは無制限
        val state = TimerState(
            currentPhase = TimerPhase.BREAK,
            completedCycles = 100,
            settings = settings
        )
        
        assertFalse(state.isCompleted())
    }
}