package com.example.wascheduler.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * A single scheduled-send rule: one target group, one message, one or more
 * times, one set of active weekdays. Multiple different messages/times require
 * multiple rules (spec section 15) — this type deliberately does not support
 * per-time messages.
 */
data class Rule(
    val id: Long = 0,
    val name: String,
    val chatName: String,
    val message: String,
    val enabled: Boolean = true,
    val startDate: LocalDate = LocalDate.now(),
    val allowedDelayMinutes: Int = 10,
    val times: List<RuleTime> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * One time-of-day + weekday-set entry belonging to a [Rule]. Stored as local
 * wall-clock time and weekdays (never as an absolute UTC instant) so that DST
 * and timezone changes are handled by recomputing the next Instant on demand
 * rather than by drifting a stored timestamp (spec sections 21-22).
 */
data class RuleTime(
    val id: Long = 0,
    val ruleId: Long,
    val localTime: LocalTime,
    val days: Set<DayOfWeek>,
    val enabled: Boolean = true
)

/** Convenience quick-pick sets used by the rule editor UI (spec section 13). */
object DayOfWeekPresets {
    val EVERY_DAY: Set<DayOfWeek> = DayOfWeek.entries.toSet()
    val WEEKDAYS: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    )
}
