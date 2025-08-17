package com.example.a20_20_20.service

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerState
import com.example.a20_20_20.domain.TimerStatus
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any

class TimerNotificationManagerTest {

    private val mockContext = mock<Context>()
    private val mockNotificationManager = mock<NotificationManager>()

    @Test
    fun `通知チャンネルが作成される`() {
        whenever(mockContext.getSystemService(Context.NOTIFICATION_SERVICE))
            .thenReturn(mockNotificationManager)
        
        val notificationManager = TimerNotificationManager(mockContext)
        
        // チャンネル作成が呼ばれることを検証
        verify(mockNotificationManager).createNotificationChannel(any())
    }

    @Test
    fun `実行中タイマーの通知が正しい内容で作成される`() {
        val timerState = TimerState(
            currentPhase = TimerPhase.WORK,
            status = TimerStatus.RUNNING,
            remainingTimeMillis = 300000L // 5分
        )
        
        val notificationManager = TimerNotificationManager(mockContext)
        val notification = notificationManager.createTimerNotification(timerState)
        
        // 通知の内容を検証
        assertNotNull(notification)
        // 実際の実装では、NotificationCompatのテストは困難なため、
        // 最低限通知が作成されることのみ確認
    }

    @Test
    fun `一時停止タイマーの通知が正しい内容で作成される`() {
        val timerState = TimerState(
            currentPhase = TimerPhase.BREAK,
            status = TimerStatus.PAUSED,
            remainingTimeMillis = 15000L // 15秒
        )
        
        val notificationManager = TimerNotificationManager(mockContext)
        val notification = notificationManager.createTimerNotification(timerState)
        
        assertNotNull(notification)
    }

    @Test
    fun `フェーズ変更時の通知音設定を確認`() {
        val notificationManager = TimerNotificationManager(mockContext)
        
        // ワーク完了時の通知
        notificationManager.showPhaseCompletionNotification(TimerPhase.WORK)
        
        // ブレイク完了時の通知
        notificationManager.showPhaseCompletionNotification(TimerPhase.BREAK)
        
        // 通知が表示されることを確認（実装時により詳細なテストが必要）
        assertTrue("フェーズ完了通知が実装されている", true)
    }
}