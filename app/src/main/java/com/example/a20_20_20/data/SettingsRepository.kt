package com.example.a20_20_20.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.a20_20_20.domain.NotificationPriority
import com.example.a20_20_20.domain.NotificationSettings
import com.example.a20_20_20.domain.NotificationUpdateInterval
import com.example.a20_20_20.domain.SoundPlaybackMode
import com.example.a20_20_20.domain.TimerSettings

class SettingsRepository(context: Context) {
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "timer_settings", Context.MODE_PRIVATE
    )
    
    // TimerSettings keys
    private companion object {
        const val KEY_WORK_DURATION = "work_duration_millis"
        const val KEY_BREAK_DURATION = "break_duration_millis"
        const val KEY_REPEAT_COUNT = "repeat_count"
        
        // NotificationSettings keys
        const val KEY_WORK_COMPLETE_SOUND = "work_complete_sound"
        const val KEY_BREAK_COMPLETE_SOUND = "break_complete_sound"
        const val KEY_ENABLE_SOUND = "enable_sound"
        const val KEY_ENABLE_VIBRATION = "enable_vibration"
        const val KEY_SOUND_VOLUME = "sound_volume"
        const val KEY_SOUND_PLAYBACK_MODE = "sound_playback_mode"
        const val KEY_NOTIFICATION_PRIORITY = "notification_priority"
        const val KEY_UPDATE_INTERVAL = "update_interval"
    }
    
    fun saveTimerSettings(settings: TimerSettings) {
        preferences.edit().apply {
            putLong(KEY_WORK_DURATION, settings.workDurationMillis)
            putLong(KEY_BREAK_DURATION, settings.breakDurationMillis)
            putInt(KEY_REPEAT_COUNT, settings.repeatCount)
            apply()
        }
    }
    
    fun loadTimerSettings(): TimerSettings {
        return TimerSettings(
            workDurationMillis = preferences.getLong(KEY_WORK_DURATION, TimerSettings.DEFAULT.workDurationMillis),
            breakDurationMillis = preferences.getLong(KEY_BREAK_DURATION, TimerSettings.DEFAULT.breakDurationMillis),
            repeatCount = preferences.getInt(KEY_REPEAT_COUNT, TimerSettings.DEFAULT.repeatCount)
        )
    }
    
    fun saveNotificationSettings(settings: NotificationSettings) {
        preferences.edit().apply {
            putString(KEY_WORK_COMPLETE_SOUND, settings.workCompleteSound?.toString())
            putString(KEY_BREAK_COMPLETE_SOUND, settings.breakCompleteSound?.toString())
            putBoolean(KEY_ENABLE_SOUND, settings.enableSound)
            putBoolean(KEY_ENABLE_VIBRATION, settings.enableVibration)
            putFloat(KEY_SOUND_VOLUME, settings.soundVolume)
            putString(KEY_SOUND_PLAYBACK_MODE, settings.soundPlaybackMode.name)
            putString(KEY_NOTIFICATION_PRIORITY, settings.priority.name)
            putString(KEY_UPDATE_INTERVAL, settings.updateInterval.name)
            apply()
        }
    }
    
    fun loadNotificationSettings(): NotificationSettings {
        val workSoundStr = preferences.getString(KEY_WORK_COMPLETE_SOUND, null)
        val breakSoundStr = preferences.getString(KEY_BREAK_COMPLETE_SOUND, null)
        val soundPlaybackModeStr = preferences.getString(KEY_SOUND_PLAYBACK_MODE, SoundPlaybackMode.NOTIFICATION.name)
        val priorityStr = preferences.getString(KEY_NOTIFICATION_PRIORITY, NotificationPriority.DEFAULT.name)
        val updateIntervalStr = preferences.getString(KEY_UPDATE_INTERVAL, NotificationUpdateInterval.EVERY_SECOND.name)
        
        return NotificationSettings(
            workCompleteSound = workSoundStr?.let { Uri.parse(it) },
            breakCompleteSound = breakSoundStr?.let { Uri.parse(it) },
            enableSound = preferences.getBoolean(KEY_ENABLE_SOUND, NotificationSettings.DEFAULT.enableSound),
            enableVibration = preferences.getBoolean(KEY_ENABLE_VIBRATION, NotificationSettings.DEFAULT.enableVibration),
            soundVolume = preferences.getFloat(KEY_SOUND_VOLUME, NotificationSettings.DEFAULT.soundVolume),
            soundPlaybackMode = try {
                SoundPlaybackMode.valueOf(soundPlaybackModeStr!!)
            } catch (e: Exception) {
                SoundPlaybackMode.NOTIFICATION
            },
            priority = try {
                NotificationPriority.valueOf(priorityStr!!)
            } catch (e: Exception) {
                NotificationPriority.DEFAULT
            },
            updateInterval = try {
                NotificationUpdateInterval.valueOf(updateIntervalStr!!)
            } catch (e: Exception) {
                NotificationUpdateInterval.EVERY_SECOND
            }
        )
    }
}