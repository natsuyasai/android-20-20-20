package com.example.a20_20_20.domain

data class TimerState(
    val currentPhase: TimerPhase = TimerPhase.WORK,
    val status: TimerStatus = TimerStatus.STOPPED,
    val remainingTimeMillis: Long,
    val completedCycles: Int = 0,
    val settings: TimerSettings = TimerSettings()
) {
    constructor(
        currentPhase: TimerPhase = TimerPhase.WORK,
        status: TimerStatus = TimerStatus.STOPPED,
        completedCycles: Int = 0,
        settings: TimerSettings = TimerSettings()
    ) : this(
        currentPhase = currentPhase,
        status = status,
        remainingTimeMillis = when (currentPhase) {
            TimerPhase.WORK -> settings.workDurationMillis
            TimerPhase.BREAK -> settings.breakDurationMillis
        },
        completedCycles = completedCycles,
        settings = settings
    )

    fun start(): TimerState {
        return copy(status = TimerStatus.RUNNING)
    }

    fun pause(): TimerState {
        return copy(status = TimerStatus.PAUSED)
    }

    fun stop(): TimerState {
        return TimerState(
            currentPhase = TimerPhase.WORK,
            status = TimerStatus.STOPPED,
            remainingTimeMillis = settings.workDurationMillis,
            completedCycles = 0,
            settings = settings
        )
    }

    fun nextPhase(): TimerState {
        return when (currentPhase) {
            TimerPhase.WORK -> {
                copy(
                    currentPhase = TimerPhase.BREAK,
                    remainingTimeMillis = settings.breakDurationMillis
                )
            }
            TimerPhase.BREAK -> {
                copy(
                    currentPhase = TimerPhase.WORK,
                    remainingTimeMillis = settings.workDurationMillis,
                    completedCycles = completedCycles + 1
                )
            }
        }
    }

    fun isCompleted(): Boolean {
        return !settings.isUnlimitedRepeat() && 
               completedCycles >= settings.repeatCount
    }

    companion object {
        operator fun invoke(settings: TimerSettings): TimerState {
            return TimerState(
                settings = settings,
                remainingTimeMillis = settings.workDurationMillis
            )
        }
    }
}