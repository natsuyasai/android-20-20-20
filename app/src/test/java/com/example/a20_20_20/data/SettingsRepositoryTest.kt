package com.example.a20_20_20.data

import com.example.a20_20_20.domain.NotificationPriority
import com.example.a20_20_20.domain.NotificationSettings
import com.example.a20_20_20.domain.SoundPlaybackMode
import com.example.a20_20_20.domain.TimerSettings
import org.junit.Test
import org.junit.Assert.*

class SettingsRepositoryTest {
    
    @Test
    fun notificationPriority_enumValues_shouldBeCorrect() {
        assertEquals("SILENT", NotificationPriority.SILENT.name)
        assertEquals("DEFAULT", NotificationPriority.DEFAULT.name)
    }
    
    @Test
    fun notificationSettings_withPriority_shouldCreateCorrectInstance() {
        val settings = NotificationSettings(
            workCompleteSound = null,
            breakCompleteSound = null,
            enableSound = false,
            enableVibration = true,
            soundVolume = 0.8f,
            soundPlaybackMode = SoundPlaybackMode.MUSIC,
            priority = NotificationPriority.SILENT
        )
        
        assertEquals(NotificationPriority.SILENT, settings.priority)
        assertFalse(settings.enableSound)
        assertTrue(settings.enableVibration)
        assertEquals(0.8f, settings.soundVolume, 0.01f)
        assertEquals(SoundPlaybackMode.MUSIC, settings.soundPlaybackMode)
    }
    
    @Test
    fun notificationSettings_default_shouldHaveDefaultPriority() {
        val defaultSettings = NotificationSettings.DEFAULT
        assertEquals(NotificationPriority.DEFAULT, defaultSettings.priority)
    }
    
    @Test
    fun timerSettings_default_shouldExist() {
        val defaultSettings = TimerSettings.DEFAULT
        assertEquals(20 * 60 * 1000L, defaultSettings.workDurationMillis)
        assertEquals(20 * 1000L, defaultSettings.breakDurationMillis)
        assertEquals(-1, defaultSettings.repeatCount)
    }
}