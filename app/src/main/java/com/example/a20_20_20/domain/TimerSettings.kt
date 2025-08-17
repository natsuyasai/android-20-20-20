package com.example.a20_20_20.domain

data class TimerSettings(
    val workDurationMillis: Long = DEFAULT_WORK_DURATION_MILLIS,
    val breakDurationMillis: Long = DEFAULT_BREAK_DURATION_MILLIS,
    val repeatCount: Int = UNLIMITED_REPEAT
) {
    companion object {
        const val DEFAULT_WORK_DURATION_MILLIS = 20 * 60 * 1000L // 20分
        const val DEFAULT_BREAK_DURATION_MILLIS = 20 * 1000L // 20秒
        const val UNLIMITED_REPEAT = -1
        
        val DEFAULT = TimerSettings()
    }

    fun isUnlimitedRepeat(): Boolean {
        return repeatCount == UNLIMITED_REPEAT
    }
}