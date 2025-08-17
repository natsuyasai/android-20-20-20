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
import com.example.a20_20_20.domain.NotificationPriority
import com.example.a20_20_20.domain.SoundPlaybackMode
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
    private var channelCreated = false
    
    companion object {
        const val CHANNEL_ID_SILENT = "timer_channel_silent"
        const val CHANNEL_ID_DEFAULT = "timer_channel_default"
        const val CHANNEL_ID_COMPLETION = "timer_channel_completion" // フェーズ完了用
        const val NOTIFICATION_ID = 1
        const val PHASE_COMPLETION_NOTIFICATION_ID = 2
    }

    init {
        // 初期化時には通知チャンネルを作成しない
        // updateSettings()が呼ばれた時に作成する
    }

    private fun createNotificationChannel() {
        // 古いチャンネルがあれば削除
        deleteOldChannelIfExists()
        
        // 通知チャンネルを作成（必要に応じて）
        createChannelIfNotExists(CHANNEL_ID_SILENT, "タイマー通知（サイレント）", NotificationManager.IMPORTANCE_LOW, true)
        createChannelIfNotExists(CHANNEL_ID_DEFAULT, "タイマー通知（デフォルト）", NotificationManager.IMPORTANCE_DEFAULT, false)
        createCompletionChannelIfNotExists()
    }
    
    private fun createCompletionChannelIfNotExists() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID_COMPLETION)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID_COMPLETION,
                    "フェーズ完了通知",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "ワーク・ブレイク完了時の通知"
                    setShowBadge(false)
                    // フェーズ完了時はバイブレーションと音を有効
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    private fun deleteOldChannelIfExists() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // 古いチャンネルID "timer_channel" を削除
                notificationManager.deleteNotificationChannel("timer_channel")
            } catch (e: Exception) {
                // 削除に失敗してもクラッシュしないように
            }
        }
    }
    
    private fun createChannelIfNotExists(channelId: String, channelName: String, importance: Int, silent: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 既存のチャンネルを確認
            val existingChannel = notificationManager.getNotificationChannel(channelId)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    importance
                ).apply {
                    description = "20-20-20タイマーの通知"
                    setShowBadge(false)
                    
                    if (silent) {
                        setSound(null, null)
                        enableVibration(false)
                    } else {
                        // デフォルト優先度チャンネルの場合は削除不可に設定
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            setAllowBubbles(false)
                        }
                        // ロック画面での表示設定
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        // 進行状況通知ではバイブレーションを抑制
                        enableVibration(false)
                    }
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    private fun getCurrentChannelId(): String {
        return when (notificationSettings.priority) {
            NotificationPriority.SILENT -> CHANNEL_ID_SILENT
            NotificationPriority.DEFAULT -> CHANNEL_ID_DEFAULT
        }
    }
    
    private fun updateNotificationChannel() {
        createNotificationChannel()
        channelCreated = true
    }

    fun createTimerNotification(timerState: TimerState): Notification {
        ensureNotificationChannelExists()
        
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

        // デフォルト優先度かつ実行中の場合は常駐・削除不可に設定
        val isRunningWithDefault = timerState.status == TimerStatus.RUNNING && 
                                  notificationSettings.priority == NotificationPriority.DEFAULT
        
        val builder = NotificationCompat.Builder(context, getCurrentChannelId())
            .setContentTitle("$phaseLabel - $statusText")
            .setContentText("残り時間: $formattedTime")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(isRunningWithDefault) // 実行中かつデフォルト優先度の場合は常駐
            .setAutoCancel(false) // スワイプでの削除を無効化
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // ロック画面でも表示
            
        // デフォルト優先度かつ実行中の場合は、より強固な削除防止設定
        if (isRunningWithDefault) {
            builder.setOnlyAlertOnce(true) // 最初の1回のみアラート（バイブレーション防止）
                   .setPriority(NotificationCompat.PRIORITY_HIGH) // 高優先度
                   .setCategory(NotificationCompat.CATEGORY_PROGRESS) // 進行状況カテゴリ
        }

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
        ensureNotificationChannelExists()
        
        val message = when (completedPhase) {
            TimerPhase.WORK -> "ワーク時間が完了しました。ブレイクタイムです！"
            TimerPhase.BREAK -> "ブレイク時間が完了しました。ワークを開始してください。"
        }

        val priority = when (notificationSettings.priority) {
            NotificationPriority.SILENT -> NotificationCompat.PRIORITY_LOW
            NotificationPriority.DEFAULT -> NotificationCompat.PRIORITY_HIGH
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_COMPLETION)
            .setContentTitle("フェーズ完了")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setPriority(priority)
            .setAutoCancel(true)
            .setTimeoutAfter(2000) // 2秒後に自動削除
            .build()

        notificationManager.notify(PHASE_COMPLETION_NOTIFICATION_ID, notification)
        
        // 2秒後に通知を手動で削除（確実に削除するため）
        notificationScope.launch {
            delay(2000)
            notificationManager.cancel(PHASE_COMPLETION_NOTIFICATION_ID)
        }
        
        // サイレントモード以外の場合のみ通知音とバイブレーションを再生
        if (notificationSettings.priority != NotificationPriority.SILENT) {
            playNotificationSound(completedPhase)
            triggerVibration()
        }
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
                
                // 再生方式に応じてAudioAttributesを設定
                val audioAttributes = when (notificationSettings.soundPlaybackMode) {
                    SoundPlaybackMode.NOTIFICATION -> AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                    SoundPlaybackMode.MUSIC -> AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                }
                
                setAudioAttributes(audioAttributes)
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

            vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1))
        }
    }

    fun updateSettings(settings: NotificationSettings) {
        notificationSettings = settings
        updateNotificationChannel()
    }
    
    private fun ensureNotificationChannelExists() {
        if (!channelCreated) {
            createNotificationChannel()
            channelCreated = true
        }
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