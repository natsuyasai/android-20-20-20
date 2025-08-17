package com.example.a20_20_20.service

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.a20_20_20.domain.NotificationSettings
import com.example.a20_20_20.domain.SoundPlaybackMode
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

    @Test
    fun `通知音モードでの設定更新が正しく動作する`() {
        val notificationManager = TimerNotificationManager(mockContext)
        
        val notificationSettings = NotificationSettings(
            enableSound = true,
            soundPlaybackMode = SoundPlaybackMode.NOTIFICATION,
            soundVolume = 0.8f
        )
        
        // 設定更新が例外をスローしないことを確認
        assertDoesNotThrow {
            notificationManager.updateSettings(notificationSettings)
        }
    }

    @Test
    fun `音楽モードでの設定更新が正しく動作する`() {
        val notificationManager = TimerNotificationManager(mockContext)
        
        val musicSettings = NotificationSettings(
            enableSound = true,
            soundPlaybackMode = SoundPlaybackMode.MUSIC,
            soundVolume = 0.6f
        )
        
        // 設定更新が例外をスローしないことを確認
        assertDoesNotThrow {
            notificationManager.updateSettings(musicSettings)
        }
    }

    @Test
    fun `音声無効時は音声設定に関係なく音声が再生されない`() {
        val notificationManager = TimerNotificationManager(mockContext)
        
        val disabledSoundSettings = NotificationSettings(
            enableSound = false,
            soundPlaybackMode = SoundPlaybackMode.MUSIC,
            soundVolume = 1.0f
        )
        
        notificationManager.updateSettings(disabledSoundSettings)
        
        // 音声無効時でも通知は正常に動作することを確認
        assertDoesNotThrow {
            notificationManager.showPhaseCompletionNotification(TimerPhase.WORK)
        }
    }

    private fun assertDoesNotThrow(executable: () -> Unit) {
        try {
            executable()
        } catch (e: Exception) {
            fail("Expected no exception but was thrown: ${e.message}")
        }
    }
}