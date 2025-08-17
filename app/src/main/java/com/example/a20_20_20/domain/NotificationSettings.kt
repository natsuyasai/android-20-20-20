package com.example.a20_20_20.domain

import android.net.Uri

data class NotificationSettings(
    val workCompleteSound: Uri? = null, // null = デフォルト通知音
    val breakCompleteSound: Uri? = null, // null = デフォルト通知音
    val enableSound: Boolean = true,
    val enableVibration: Boolean = true,
    val soundVolume: Float = 1.0f // 0.0f - 1.0f
) {
    companion object {
        val DEFAULT = NotificationSettings()
    }
}