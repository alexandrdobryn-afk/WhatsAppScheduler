package com.example.wascheduler.core.automation

import java.time.Duration
import java.time.LocalDateTime

object ExecutionWindowPolicy {
    fun isWithinWindow(
        scheduledAt: LocalDateTime,
        now: LocalDateTime,
        allowedDelayMinutes: Int
    ): Boolean {
        if (now.isBefore(scheduledAt)) return false
        val minutesLate = Duration.between(scheduledAt, now).toMinutes()
        return minutesLate <= allowedDelayMinutes
    }
}
