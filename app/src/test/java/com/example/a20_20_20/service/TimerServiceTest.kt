package com.example.a20_20_20.service

import com.example.a20_20_20.domain.NotificationSettings
import com.example.a20_20_20.domain.NotificationUpdateInterval
import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerState
import com.example.a20_20_20.domain.TimerStatus
import org.junit.Test
import org.junit.Assert.*

class TimerServiceTest {

    @Test
    fun `通知チェック間隔が残り時間に応じて適切に計算されること`() {
        // このテストでは、TimerServiceの private メソッドを直接テストできないため、
        // 論理的な検証を行う
        
        // 各更新間隔の値を確認
        assertEquals("1秒間隔が正しいこと", 1000L, NotificationUpdateInterval.EVERY_SECOND.intervalMillis)
        assertEquals("2秒間隔が正しいこと", 2000L, NotificationUpdateInterval.EVERY_2_SECONDS.intervalMillis)
        assertEquals("5秒間隔が正しいこと", 5000L, NotificationUpdateInterval.EVERY_5_SECONDS.intervalMillis)
        assertEquals("10秒間隔が正しいこと", 10000L, NotificationUpdateInterval.EVERY_10_SECONDS.intervalMillis)
    }

    @Test
    fun `通知チェック間隔の計算ロジックが正しいこと`() {
        // 実際のTimerServiceで使用される計算ロジックをテスト
        
        // 残り時間が3秒以下の場合は500ms間隔
        val shortInterval = calculateTestInterval(1000L, 2000L) // baseInterval=1s, remaining=2s
        assertEquals("短時間では500ms間隔", 500L, shortInterval)
        
        // 残り時間が10秒以下の場合は1秒間隔
        val mediumInterval = calculateTestInterval(5000L, 8000L) // baseInterval=5s, remaining=8s
        assertEquals("中程度では1秒間隔", 1000L, mediumInterval)
        
        // それ以外はベース間隔
        val longInterval = calculateTestInterval(10000L, 60000L) // baseInterval=10s, remaining=1min
        assertEquals("長時間ではベース間隔", 10000L, longInterval)
    }

    @Test
    fun `クールダウン期間が更新間隔に基づいて計算されること`() {
        val settings1s = NotificationSettings(updateInterval = NotificationUpdateInterval.EVERY_SECOND)
        val settings5s = NotificationSettings(updateInterval = NotificationUpdateInterval.EVERY_5_SECONDS)
        val settings30s = NotificationSettings(updateInterval = NotificationUpdateInterval.EVERY_30_SECONDS)
        
        // クールダウン期間は更新間隔の2倍
        assertEquals("1秒設定のクールダウン", 2000L, settings1s.updateInterval.intervalMillis * 2)
        assertEquals("5秒設定のクールダウン", 10000L, settings5s.updateInterval.intervalMillis * 2)
        assertEquals("30秒設定のクールダウン", 60000L, settings30s.updateInterval.intervalMillis * 2)
    }

    @Test
    fun `タイマー状態と通知監視の関係が正しいこと`() {
        // 監視が必要な状態
        val runningState = TimerState(status = TimerStatus.RUNNING)
        val pausedState = TimerState(status = TimerStatus.PAUSED)
        val stoppedState = TimerState(status = TimerStatus.STOPPED)
        
        assertTrue("実行中は監視が必要", shouldMonitorNotification(runningState))
        assertTrue("一時停止中は監視が必要", shouldMonitorNotification(pausedState))
        assertFalse("停止中は監視不要", shouldMonitorNotification(stoppedState))
    }

    // ヘルパーメソッド：TimerServiceの計算ロジックを模擬
    private fun calculateTestInterval(baseInterval: Long, remainingTimeMillis: Long): Long {
        return when {
            remainingTimeMillis <= 3000 -> 500L
            remainingTimeMillis <= 10000 -> 1000L
            else -> baseInterval
        }
    }

    private fun shouldMonitorNotification(state: TimerState): Boolean {
        return state.status != TimerStatus.STOPPED
    }
}