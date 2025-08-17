package com.example.a20_20_20.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.a20_20_20.MainActivity
import com.example.a20_20_20.R
import com.example.a20_20_20.domain.NotificationSettings
import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerState
import com.example.a20_20_20.domain.TimerStatus
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerNotificationManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var notificationSettings = NotificationSettings.DEFAULT
    private var mediaPlayer: MediaPlayer? = null
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
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

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("$phaseLabel - $statusText")
            .setContentText("残り時間: $formattedTime")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 実際のアプリではカスタムアイコンを使用
            .setContentIntent(pendingIntent)
            .setOngoing(timerState.status == TimerStatus.RUNNING)
            .setAutoCancel(false)

        // 状態に応じてアクションボタンを追加
        when (timerState.status) {
            TimerStatus.RUNNING -> {
                // 一時停止と停止ボタン
                builder.addAction(createPauseAction())
                builder.addAction(createStopAction())
            }
            TimerStatus.PAUSED -> {
                // 再開と停止ボタン
                builder.addAction(createStartAction())
                builder.addAction(createStopAction())
            }
            TimerStatus.STOPPED -> {
                // 開始ボタンのみ
                builder.addAction(createStartAction())
            }
        }

        return builder.build()
    }

    private fun createStartAction(): NotificationCompat.Action {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START_TIMER
        }
        val pendingIntent = PendingIntent.getService(
            context, 1, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action(
            android.R.drawable.ic_media_play,
            "開始",
            pendingIntent
        )
    }

    private fun createPauseAction(): NotificationCompat.Action {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE_TIMER
        }
        val pendingIntent = PendingIntent.getService(
            context, 2, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action(
            android.R.drawable.ic_media_pause,
            "一時停止",
            pendingIntent
        )
    }

    private fun createStopAction(): NotificationCompat.Action {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP_TIMER
        }
        val pendingIntent = PendingIntent.getService(
            context, 3, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action(
            android.R.drawable.ic_delete,
            "停止",
            pendingIntent
        )
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
            .setTimeoutAfter(2000) // 2秒後に自動削除
            .build()

        notificationManager.notify(PHASE_COMPLETION_NOTIFICATION_ID, notification)
        
        // 2秒後に通知を手動で削除（確実に削除するため）
        notificationScope.launch {
            delay(2000)
            notificationManager.cancel(PHASE_COMPLETION_NOTIFICATION_ID)
        }
        
        // 通知音とバイブレーションを再生
        playNotificationSound(completedPhase)
        triggerVibration()
    }

    private fun playNotificationSound(completedPhase: TimerPhase) {
        if (!notificationSettings.enableSound) return

        val soundUri = when (completedPhase) {
            TimerPhase.WORK -> notificationSettings.workCompleteSound
            TimerPhase.BREAK -> notificationSettings.breakCompleteSound
        } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                setVolume(notificationSettings.soundVolume, notificationSettings.soundVolume)
                setOnCompletionListener { player ->
                    player.release()
                    mediaPlayer = null
                }
                prepareAsync()
                setOnPreparedListener { player ->
                    player.start()
                }
            }
        } catch (e: Exception) {
            // サウンド再生エラーの場合はログに記録し、続行
            e.printStackTrace()
        }
    }

    private fun triggerVibration() {
        if (!notificationSettings.enableVibration) return

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            val vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 200) // パターン: 待機, バイブ, 停止, バイブ, 停止, バイブ
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibrationPattern, -1)
            }
        }
    }

    fun updateSettings(settings: NotificationSettings) {
        notificationSettings = settings
    }

    fun cleanup() {
        mediaPlayer?.release()
        mediaPlayer = null
        notificationScope.cancel()
    }

    fun notify(notificationId: Int, notification: Notification) {
        notificationManager.notify(notificationId, notification)
    }

    private fun formatTime(timeInMillis: Long): String {
        val totalSeconds = ceil(timeInMillis / 1000.0).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}