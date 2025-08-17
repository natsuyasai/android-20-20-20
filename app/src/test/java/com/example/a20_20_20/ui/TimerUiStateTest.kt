package com.example.a20_20_20.ui

import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerSettings
import com.example.a20_20_20.domain.TimerState
import org.junit.Test
import org.junit.Assert.*

class TimerUiStateTest {

    @Test
    fun `ワークフェーズ時のフォーマット済み時間が正しく表示される`() {
        val timerState = TimerState(
            currentPhase = TimerPhase.WORK,
            remainingTimeMillis = 75000L, // 1分15秒
            settings = TimerSettings()
        )
        val uiState = TimerUiState(timerState = timerState)
        
        assertEquals("01:15", uiState.formattedTime)
    }

    @Test
    fun `ブレイクフェーズ時のフォーマット済み時間が正しく表示される`() {
        val timerState = TimerState(
            currentPhase = TimerPhase.BREAK,
            remainingTimeMillis = 5000L, // 5秒
            settings = TimerSettings()
        )
        val uiState = TimerUiState(timerState = timerState)
        
        assertEquals("00:05", uiState.formattedTime)
    }

    @Test
    fun `0秒時のフォーマット済み時間が正しく表示される`() {
        val timerState = TimerState(
            remainingTimeMillis = 0L,
            settings = TimerSettings()
        )
        val uiState = TimerUiState(timerState = timerState)
        
        assertEquals("00:00", uiState.formattedTime)
    }

    @Test
    fun `1時間以上の時間が正しくフォーマットされる`() {
        val timerState = TimerState(
            remainingTimeMillis = 3725000L, // 1時間2分5秒
            settings = TimerSettings()
        )
        val uiState = TimerUiState(timerState = timerState)
        
        assertEquals("62:05", uiState.formattedTime) // 分として表示
    }

    @Test
    fun `ワークフェーズラベルが正しく表示される`() {
        val timerState = TimerState(currentPhase = TimerPhase.WORK)
        val uiState = TimerUiState(timerState = timerState)
        
        assertEquals("ワーク", uiState.phaseLabel)
    }

    @Test
    fun `ブレイクフェーズラベルが正しく表示される`() {
        val timerState = TimerState(currentPhase = TimerPhase.BREAK)
        val uiState = TimerUiState(timerState = timerState)
        
        assertEquals("ブレイク", uiState.phaseLabel)
    }

    @Test
    fun `完了サイクル数が正しく表示される`() {
        val timerState = TimerState(completedCycles = 3)
        val uiState = TimerUiState(timerState = timerState)
        
        assertEquals("3サイクル完了", uiState.cycleInfo)
    }

    @Test
    fun `0サイクル時の表示が正しい`() {
        val timerState = TimerState(completedCycles = 0)
        val uiState = TimerUiState(timerState = timerState)
        
        assertEquals("0サイクル完了", uiState.cycleInfo)
    }
}