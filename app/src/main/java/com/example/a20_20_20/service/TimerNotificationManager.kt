package com.example.a20_20_20.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.a20_20_20.MainActivity
import com.example.a20_20_20.R
import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerState
import com.example.a20_20_20.domain.TimerStatus
import kotlin.math.ceil

class TimerNotificationManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1
        const val PHASE_COMPLETION_NOTIFICATION_ID = 2
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "タイマー通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "20-20-20タイマーの通知"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createTimerNotification(timerState: TimerState): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val formattedTime = formatTime(timerState.remainingTimeMillis)
        val phaseLabel = when (timerState.currentPhase) {
            TimerPhase.WORK -> "ワーク"
            TimerPhase.BREAK -> "ブレイク"
        }
        
        val statusText = when (timerState.status) {
            TimerStatus.RUNNING -> "実行中"
            TimerStatus.PAUSED -> "一時停止"
            TimerStatus.STOPPED -> "停止"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("$phaseLabel - $statusText")
            .setContentText("残り時間: $formattedTime")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 実際のアプリではカスタムアイコンを使用
            .setContentIntent(pendingIntent)
            .setOngoing(timerState.status == TimerStatus.RUNNING)
            .setAutoCancel(false)
            .build()
    }

    fun showPhaseCompletionNotification(completedPhase: TimerPhase) {
        val message = when (completedPhase) {
            TimerPhase.WORK -> "ワーク時間が完了しました。ブレイクタイムです！"
            TimerPhase.BREAK -> "ブレイク時間が完了しました。ワークを開始してください。"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("フェーズ完了")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(PHASE_COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun formatTime(timeInMillis: Long): String {
        val totalSeconds = ceil(timeInMillis / 1000.0).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}