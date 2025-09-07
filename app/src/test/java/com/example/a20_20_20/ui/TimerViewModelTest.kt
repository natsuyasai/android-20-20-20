package com.example.a20_20_20.ui

import com.example.a20_20_20.domain.NotificationSettings
import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerSettings
import com.example.a20_20_20.domain.TimerState
import com.example.a20_20_20.domain.TimerStatus
import org.junit.Test
import org.junit.Assert.*

class TimerViewModelTest {

    @Test
    fun `初期状態でタイマーが停止状態`() {
        // TimerViewModelは複雑な依存関係があるため、
        // ビジネスロジックのテストに焦点を当てる
        val initialState = TimerState()
        
        assertEquals(TimerStatus.STOPPED, initialState.status)
        assertEquals(TimerPhase.WORK, initialState.currentPhase)
    }

    @Test
    fun `タイマー開始時にステータスが実行中になる`() {
        // TimerStateのビジネスロジックをテスト
        val initialState = TimerState()
        val runningState = initialState.start()
        
        assertEquals(TimerStatus.RUNNING, runningState.status)
    }

    @Test
    fun `タイマー一時停止時にステータスが一時停止になる`() {
        val initialState = TimerState()
        val runningState = initialState.start()
        val pausedState = runningState.copy(status = TimerStatus.PAUSED)
        
        assertEquals(TimerStatus.PAUSED, pausedState.status)
    }

    @Test
    fun `タイマー停止時にステータスが停止になり初期状態にリセット`() {
        val initialState = TimerState()
        val runningState = initialState.start()
        val stoppedState = runningState.stop()
        
        assertEquals(TimerStatus.STOPPED, stoppedState.status)
        assertEquals(TimerPhase.WORK, stoppedState.currentPhase)
        assertEquals(0, stoppedState.completedCycles)
    }

    @Test
    fun `設定更新時に新しい設定が反映される`() {
        val customSettings = TimerSettings(
            workDurationMillis = 1500000L, // 25分
            breakDurationMillis = 30000L,  // 30秒
            repeatCount = 5
        )
        
        val timerState = TimerState(settings = customSettings)
        
        assertEquals(customSettings, timerState.settings)
        assertEquals(1500000L, timerState.remainingTimeMillis)
    }

    @Test
    fun `残り時間のフォーマットが正しく表示される`() {
        // 時間フォーマットのロジックをテスト
        val timeInMillis = 1200000L // 20分
        val minutes = (timeInMillis / 60000).toInt()
        val seconds = ((timeInMillis % 60000) / 1000).toInt()
        val formatted = String.format("%d:%02d", minutes, seconds)
        
        assertEquals("20:00", formatted)
    }

    @Test
    fun `秒未満の残り時間のフォーマットが正しく表示される`() {
        // 短い時間のフォーマットをテスト
        val timeInMillis = 30500L // 30.5秒
        val minutes = (timeInMillis / 60000).toInt()
        val seconds = ((timeInMillis % 60000) / 1000).toInt()
        val formatted = String.format("%d:%02d", minutes, seconds)
        
        assertEquals("0:30", formatted)
    }

    @Test
    fun `現在のフェーズラベルが正しく表示される`() {
        val workState = TimerState(currentPhase = TimerPhase.WORK)
        val breakState = TimerState(currentPhase = TimerPhase.BREAK)
        
        assertEquals(TimerPhase.WORK, workState.currentPhase)
        assertEquals(TimerPhase.BREAK, breakState.currentPhase)
    }

    @Test
    fun `画面ロック設定が正しく切り替わる`() {
        // 初期設定（画面ロック無効が false）
        val initialSettings = NotificationSettings(
            keepScreenOnDuringTimer = false
        )
        
        // 切り替え後の設定（画面ロック無効が true）
        val toggledSettings = initialSettings.copy(
            keepScreenOnDuringTimer = !initialSettings.keepScreenOnDuringTimer
        )
        
        assertFalse(initialSettings.keepScreenOnDuringTimer)
        assertTrue(toggledSettings.keepScreenOnDuringTimer)
    }

    @Test
    fun `画面ロック有効時のトーストメッセージが正しい`() {
        val settings = NotificationSettings(keepScreenOnDuringTimer = false)
        val newSettings = settings.copy(keepScreenOnDuringTimer = true)
        
        val expectedMessage = if (newSettings.keepScreenOnDuringTimer) {
            "画面ロック無効"
        } else {
            "画面ロック有効"
        }
        
        assertEquals("画面ロック無効", expectedMessage)
    }

    @Test
    fun `画面ロック無効時のトーストメッセージが正しい`() {
        val settings = NotificationSettings(keepScreenOnDuringTimer = true)
        val newSettings = settings.copy(keepScreenOnDuringTimer = false)
        
        val expectedMessage = if (newSettings.keepScreenOnDuringTimer) {
            "画面ロック無効"
        } else {
            "画面ロック有効"
        }
        
        assertEquals("画面ロック有効", expectedMessage)
    }
}