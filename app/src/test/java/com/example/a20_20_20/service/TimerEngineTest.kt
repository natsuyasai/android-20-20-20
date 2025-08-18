package com.example.a20_20_20.service

import android.content.Context
import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerSettings
import com.example.a20_20_20.domain.TimerState
import com.example.a20_20_20.domain.TimerStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockContext = mock<Context>()

    @Test
    fun `初期状態でタイマーが停止状態`() = runTest(testDispatcher) {
        val engine = TimerEngine(mockContext, testDispatcher)
        val state = engine.timerState.value
        
        assertEquals(TimerStatus.STOPPED, state.status)
        assertEquals(TimerPhase.WORK, state.currentPhase)
    }

    @Test
    fun `タイマー開始時に実行中状態になる`() = runTest(testDispatcher) {
        val engine = TimerEngine(mockContext, testDispatcher)
        
        engine.start()
        val state = engine.timerState.value
        
        assertEquals(TimerStatus.RUNNING, state.status)
    }

    @Test
    fun `タイマー停止時に初期状態にリセット`() = runTest(testDispatcher) {
        val settings = TimerSettings(workDurationMillis = 60000L)
        val engine = TimerEngine(mockContext, testDispatcher)
        
        engine.updateSettings(settings)
        engine.start()
        engine.stop()
        
        val state = engine.timerState.value
        assertEquals(TimerStatus.STOPPED, state.status)
        assertEquals(TimerPhase.WORK, state.currentPhase)
        assertEquals(60000L, state.remainingTimeMillis)
        assertEquals(0, state.completedCycles)
    }

    @Test
    fun `タイマー一時停止時に一時停止状態になる`() = runTest(testDispatcher) {
        val engine = TimerEngine(mockContext, testDispatcher)
        
        engine.start()
        engine.pause()
        
        val state = engine.timerState.value
        assertEquals(TimerStatus.PAUSED, state.status)
    }

    @Test
    fun `設定更新時に新しい設定が反映される`() = runTest(testDispatcher) {
        val customSettings = TimerSettings(
            workDurationMillis = 1500000L, // 25分
            breakDurationMillis = 30000L,  // 30秒
            repeatCount = 5
        )
        val engine = TimerEngine(mockContext, testDispatcher)
        
        engine.updateSettings(customSettings)
        val state = engine.timerState.value
        
        assertEquals(customSettings, state.settings)
        assertEquals(1500000L, state.remainingTimeMillis)
    }

    @Test
    fun `時間経過時に残り時間が減少する`() = runTest(testDispatcher) {
        val settings = TimerSettings(workDurationMillis = 5000L) // 5秒
        val engine = TimerEngine(mockContext, testDispatcher)
        
        engine.updateSettings(settings)
        engine.start()
        
        // 1秒進める
        testDispatcher.scheduler.advanceTimeBy(1000L)
        testDispatcher.scheduler.runCurrent()
        
        val state = engine.timerState.value
        assertTrue("残り時間が減少している", state.remainingTimeMillis < 5000L)
    }

    @Test
    fun `ワーク時間終了時にブレイクフェーズに移行`() = runTest(testDispatcher) {
        val settings = TimerSettings(
            workDurationMillis = 1000L, // 1秒
            breakDurationMillis = 2000L  // 2秒
        )
        val engine = TimerEngine(mockContext, testDispatcher)
        
        engine.updateSettings(settings)
        engine.start()
        
        // ワーク時間を完全に経過
        testDispatcher.scheduler.advanceTimeBy(1100L)
        testDispatcher.scheduler.runCurrent()
        
        val state = engine.timerState.value
        assertEquals(TimerPhase.BREAK, state.currentPhase)
        assertEquals(2000L, state.remainingTimeMillis)
    }

    @Test
    fun `ブレイク時間終了時にワークフェーズに移行しサイクルが増加`() = runTest(testDispatcher) {
        val settings = TimerSettings(
            workDurationMillis = 1000L, // 1秒  
            breakDurationMillis = 1000L  // 1秒
        )
        val engine = TimerEngine(mockContext, testDispatcher)
        
        engine.updateSettings(settings)
        engine.start()
        
        // ワーク時間とブレイク時間を完全に経過
        testDispatcher.scheduler.advanceTimeBy(2100L)
        testDispatcher.scheduler.runCurrent()
        
        val state = engine.timerState.value
        assertEquals(TimerPhase.WORK, state.currentPhase)
        assertEquals(1000L, state.remainingTimeMillis)
        assertEquals(1, state.completedCycles)
    }

    @Test
    fun `最大サイクル数に達した場合にタイマーが停止`() = runTest(testDispatcher) {
        val settings = TimerSettings(
            workDurationMillis = 1000L, // 1秒
            breakDurationMillis = 1000L, // 1秒
            repeatCount = 1 // 1サイクルのみ
        )
        val engine = TimerEngine(mockContext, testDispatcher)
        
        engine.updateSettings(settings)
        engine.start()
        
        // 1サイクル完了まで時間を進める
        testDispatcher.scheduler.advanceTimeBy(2100L)
        testDispatcher.scheduler.runCurrent()
        
        val state = engine.timerState.value
        assertEquals(TimerStatus.STOPPED, state.status)
        assertTrue("タイマーが完了状態", state.isCompleted())
    }

    @Test
    fun `AlarmManagerベースでのフェーズ完了処理が正しく動作する`() = runTest(testDispatcher) {
        val engine = TimerEngine(mockContext, testDispatcher)
        
        // ワークフェーズで開始
        engine.start()
        val initialState = engine.timerState.value
        assertEquals(TimerPhase.WORK, initialState.currentPhase)
        assertEquals(TimerStatus.RUNNING, initialState.status)
        
        // フェーズ完了を手動でトリガー
        engine.handlePhaseCompletion()
        
        val stateAfterCompletion = engine.timerState.value
        assertEquals(TimerPhase.BREAK, stateAfterCompletion.currentPhase)
    }

    @Test
    fun `手動停止フラグが正しく設定される`() = runTest(testDispatcher) {
        val engine = TimerEngine(mockContext, testDispatcher)
        
        engine.start()
        assertFalse("開始時は手動停止フラグがfalse", engine.isManuallyStoppedRecently())
        
        engine.stop()
        assertTrue("停止時は手動停止フラグがtrue", engine.isManuallyStoppedRecently())
    }

    @Test
    fun `一時停止と再開が正しく動作する`() = runTest(testDispatcher) {
        val engine = TimerEngine(mockContext, testDispatcher)
        
        engine.start()
        assertEquals(TimerStatus.RUNNING, engine.timerState.value.status)
        
        engine.pause()
        assertEquals(TimerStatus.PAUSED, engine.timerState.value.status)
        
        engine.start() // 再開
        assertEquals(TimerStatus.RUNNING, engine.timerState.value.status)
    }
}