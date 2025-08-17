package com.example.a20_20_20.ui

import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerSettings
import com.example.a20_20_20.domain.TimerStatus
import org.junit.Test
import org.junit.Assert.*

class TimerViewModelTest {

    @Test
    fun `初期状態でタイマーが停止状態`() {
        val viewModel = TimerViewModel()
        val uiState = viewModel.uiState.value
        
        assertEquals(TimerStatus.STOPPED, uiState.timerState.status)
        assertEquals(TimerPhase.WORK, uiState.timerState.currentPhase)
    }

    @Test
    fun `タイマー開始時にステータスが実行中になる`() {
        val viewModel = TimerViewModel()
        
        viewModel.startTimer()
        val uiState = viewModel.uiState.value
        
        assertEquals(TimerStatus.RUNNING, uiState.timerState.status)
    }

    @Test
    fun `タイマー一時停止時にステータスが一時停止になる`() {
        val viewModel = TimerViewModel()
        
        viewModel.startTimer()
        viewModel.pauseTimer()
        val uiState = viewModel.uiState.value
        
        assertEquals(TimerStatus.PAUSED, uiState.timerState.status)
    }

    @Test
    fun `タイマー停止時にステータスが停止になり初期状態にリセット`() {
        val viewModel = TimerViewModel()
        val settings = TimerSettings(workDurationMillis = 1200000L)
        
        viewModel.updateSettings(settings)
        viewModel.startTimer()
        // 何らかの時間経過をシミュレート
        viewModel.stopTimer()
        
        val uiState = viewModel.uiState.value
        assertEquals(TimerStatus.STOPPED, uiState.timerState.status)
        assertEquals(TimerPhase.WORK, uiState.timerState.currentPhase)
        assertEquals(0, uiState.timerState.completedCycles)
    }

    @Test
    fun `設定更新時に新しい設定が反映される`() {
        val viewModel = TimerViewModel()
        val customSettings = TimerSettings(
            workDurationMillis = 1500000L, // 25分
            breakDurationMillis = 30000L,  // 30秒
            repeatCount = 10
        )
        
        viewModel.updateSettings(customSettings)
        val uiState = viewModel.uiState.value
        
        assertEquals(customSettings, uiState.timerState.settings)
        assertEquals(1500000L, uiState.timerState.remainingTimeMillis)
    }

    @Test
    fun `残り時間のフォーマットが正しく表示される`() {
        val viewModel = TimerViewModel()
        val settings = TimerSettings(workDurationMillis = 125000L) // 2分5秒
        
        viewModel.updateSettings(settings)
        val uiState = viewModel.uiState.value
        
        assertEquals("02:05", uiState.formattedTime)
    }

    @Test
    fun `秒未満の残り時間のフォーマットが正しく表示される`() {
        val viewModel = TimerViewModel()
        val settings = TimerSettings(workDurationMillis = 500L) // 0.5秒
        
        viewModel.updateSettings(settings)
        val uiState = viewModel.uiState.value
        
        assertEquals("00:01", uiState.formattedTime) // 切り上げで1秒表示
    }

    @Test
    fun `現在のフェーズラベルが正しく表示される`() {
        val viewModel = TimerViewModel()
        
        val workUiState = viewModel.uiState.value
        assertEquals("ワーク", workUiState.phaseLabel)
        
        // ブレイクフェーズに移行
        viewModel.startTimer()
        // フェーズ移行のシミュレート（実際の実装では時間経過で自動移行）
        val breakSettings = TimerSettings(breakDurationMillis = 20000L)
        viewModel.updateSettings(breakSettings)
        // この部分は実装時により詳細なテストが必要
    }
}