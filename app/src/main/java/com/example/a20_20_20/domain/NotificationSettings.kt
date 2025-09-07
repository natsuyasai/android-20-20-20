package com.example.a20_20_20.domain

import android.net.Uri

enum class SoundPlaybackMode {
    NOTIFICATION, // 通知音として再生
    MUSIC        // 音楽として再生
}

enum class NotificationPriority {
    SILENT,  // サイレント（優先度低）
    DEFAULT  // デフォルト（通常の優先度）
}

enum class NotificationUpdateInterval(val displayName: String, val intervalMillis: Long) {
    EVERY_SECOND("1秒", 1000L),
    EVERY_2_SECONDS("2秒", 2000L),
    EVERY_5_SECONDS("5秒", 5000L),
    EVERY_10_SECONDS("10秒", 10000L),
    EVERY_30_SECONDS("30秒", 30000L),
    EVERY_MINUTE("1分", 60000L)
}

data class NotificationSettings(
    val workCompleteSound: Uri? = null, // null = デフォルト通知音
    val breakCompleteSound: Uri? = null, // null = デフォルト通知音
    val enableSound: Boolean = true,
    val enableVibration: Boolean = true,
    val soundVolume: Float = 1.0f, // 0.0f - 1.0f
    val soundPlaybackMode: SoundPlaybackMode = SoundPlaybackMode.NOTIFICATION, // 音声再生方式
    val priority: NotificationPriority = NotificationPriority.DEFAULT, // 通知の優先度
    val updateInterval: NotificationUpdateInterval = NotificationUpdateInterval.EVERY_SECOND, // 通知更新間隔
    val keepScreenOnDuringTimer: Boolean = false // カウントダウン中の画面自動ロック無効化
) {
    companion object {
        val DEFAULT = NotificationSettings()
    }
}