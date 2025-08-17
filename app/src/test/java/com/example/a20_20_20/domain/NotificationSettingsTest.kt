package com.example.a20_20_20.domain

import org.junit.Test
import org.junit.Assert.*

class NotificationSettingsTest {

    @Test
    fun defaultSettings_shouldHaveCorrectDefaults() {
        val defaultSettings = NotificationSettings.DEFAULT
        
        assertNull(defaultSettings.workCompleteSound)
        assertNull(defaultSettings.breakCompleteSound)
        assertTrue(defaultSettings.enableSound)
        assertTrue(defaultSettings.enableVibration)
        assertEquals(1.0f, defaultSettings.soundVolume, 0.01f)
        assertEquals(SoundPlaybackMode.NOTIFICATION, defaultSettings.soundPlaybackMode)
        assertEquals(NotificationPriority.DEFAULT, defaultSettings.priority)
    }

    @Test
    fun constructor_shouldSetAllProperties() {
        val settings = NotificationSettings(
            workCompleteSound = null,
            breakCompleteSound = null,
            enableSound = false,
            enableVibration = false,
            soundVolume = 0.5f,
            soundPlaybackMode = SoundPlaybackMode.MUSIC,
            priority = NotificationPriority.SILENT
        )
        
        assertNull(settings.workCompleteSound)
        assertNull(settings.breakCompleteSound)
        assertFalse(settings.enableSound)
        assertFalse(settings.enableVibration)
        assertEquals(0.5f, settings.soundVolume, 0.01f)
        assertEquals(SoundPlaybackMode.MUSIC, settings.soundPlaybackMode)
        assertEquals(NotificationPriority.SILENT, settings.priority)
    }

    @Test
    fun soundPlaybackMode_shouldHaveCorrectValues() {
        assertEquals("NOTIFICATION", SoundPlaybackMode.NOTIFICATION.name)
        assertEquals("MUSIC", SoundPlaybackMode.MUSIC.name)
    }

    @Test
    fun notificationPriority_shouldHaveCorrectValues() {
        assertEquals("SILENT", NotificationPriority.SILENT.name)
        assertEquals("DEFAULT", NotificationPriority.DEFAULT.name)
    }

    @Test
    fun copy_shouldCreateNewInstanceWithChangedProperties() {
        val originalSettings = NotificationSettings.DEFAULT
        
        val modifiedSettings = originalSettings.copy(
            soundPlaybackMode = SoundPlaybackMode.MUSIC,
            soundVolume = 0.7f,
            priority = NotificationPriority.SILENT
        )
        
        assertEquals(SoundPlaybackMode.MUSIC, modifiedSettings.soundPlaybackMode)
        assertEquals(0.7f, modifiedSettings.soundVolume, 0.01f)
        assertEquals(NotificationPriority.SILENT, modifiedSettings.priority)
        // 他のプロパティは変更されていないことを確認
        assertEquals(originalSettings.enableSound, modifiedSettings.enableSound)
        assertEquals(originalSettings.enableVibration, modifiedSettings.enableVibration)
    }
}