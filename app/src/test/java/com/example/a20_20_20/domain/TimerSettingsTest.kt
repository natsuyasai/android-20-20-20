package com.example.a20_20_20.domain

import org.junit.Test
import org.junit.Assert.*

class TimerSettingsTest {

    @Test
    fun `デフォルト設定は20分のワーク時間と20秒のブレイク時間を持つ`() {
        val settings = TimerSettings()
        
        assertEquals(20 * 60 * 1000L, settings.workDurationMillis)
        assertEquals(20 * 1000L, settings.breakDurationMillis)
    }

    @Test
    fun `デフォルト設定は無制限リピートを持つ`() {
        val settings = TimerSettings()
        
        assertEquals(TimerSettings.UNLIMITED_REPEAT, settings.repeatCount)
    }

    @Test
    fun `カスタム設定を作成できる`() {
        val customWorkMinutes = 25
        val customBreakSeconds = 30
        val customRepeatCount = 5
        
        val settings = TimerSettings(
            workDurationMillis = customWorkMinutes * 60 * 1000L,
            breakDurationMillis = customBreakSeconds * 1000L,
            repeatCount = customRepeatCount
        )
        
        assertEquals(customWorkMinutes * 60 * 1000L, settings.workDurationMillis)
        assertEquals(customBreakSeconds * 1000L, settings.breakDurationMillis)
        assertEquals(customRepeatCount, settings.repeatCount)
    }

    @Test
    fun `無制限リピートが正しく判定される`() {
        val unlimitedSettings = TimerSettings()
        val limitedSettings = TimerSettings(repeatCount = 5)
        
        assertTrue(unlimitedSettings.isUnlimitedRepeat())
        assertFalse(limitedSettings.isUnlimitedRepeat())
    }
}