package com.example.wascheduler.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class ScheduleType {
    WEEKLY,
    SPECIFIC_DATE,
    MULTIPLE_DATES
}

/**
 * A single scheduled-send rule: one target chat/group, one message, one or more
 * times, and either weekly days or concrete calendar dates. Multiple different
 * messages still require multiple rules.
 */
data class Rule(
    val id: Long = 0,
    val name: String,
    val chatName: String,
    val message: String,
    val enabled: Boolean = true,
    val scheduleType: ScheduleType = ScheduleType.WEEKLY,
    val startDate: LocalDate = LocalDate.now(),
    val dates: List<LocalDate> = emptyList(),
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
    val WEEKENDS: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
}
