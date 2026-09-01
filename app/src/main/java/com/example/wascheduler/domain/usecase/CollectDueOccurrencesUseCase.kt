package com.example.wascheduler.domain.usecase

import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.model.RuleTime
import com.example.wascheduler.domain.model.ScheduleType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

data class DueOccurrence(val rule: Rule, val scheduledAt: LocalDateTime)

/**
 * Finds every (rule, scheduledAt) pair whose target wall-clock time has already
 * passed today, within that rule's own allowedDelayMinutes window. Deliberately
 * does not look further back than each rule's own delay window, so a phone that
 * was off for hours does not fire a backlog of old messages when it comes back
 * (spec section 38) — AutomationEngine additionally re-checks lateness and
 * marks MISSED_WINDOW as a second safety net.
 *
 * When several rules share the same due minute, all are returned so the caller
 * (AlarmReceiver / boot restore) can run them as a small sequential queue
 * rather than concurrently (spec section 76).
 */
class CollectDueOccurrencesUseCase @Inject constructor() {

    fun collect(
        rules: List<Rule>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: ZonedDateTime = ZonedDateTime.now(zoneId)
    ): List<DueOccurrence> {
        val today = now.toLocalDate()
        val results = mutableListOf<DueOccurrence>()
        for (rule in rules) {
            if (!rule.enabled) continue
            if (today.isBefore(rule.startDate)) continue
            for (time in rule.times) {
                if (!time.enabled) continue
                if (!isDueDate(rule, time, today)) continue
                collectIfWithinWindow(rule, time.localTime, zoneId, now, today, results)
            }
        }
        return results.sortedBy { it.scheduledAt }
    }

    private fun isDueDate(
        rule: Rule,
        time: RuleTime,
        today: LocalDate
    ): Boolean =
        when (rule.scheduleType) {
            ScheduleType.WEEKLY -> today.dayOfWeek in time.days
            ScheduleType.SPECIFIC_DATE -> today in rule.dates && rule.dates.any { date ->
                date == today && !date.atTime(time.localTime).toLocalDate().isBefore(rule.startDate)
            }
            ScheduleType.MULTIPLE_DATES -> time.localDate == today && !today.isBefore(rule.startDate)
        }

    private fun collectIfWithinWindow(
        rule: Rule,
        localTime: LocalTime,
        zoneId: ZoneId,
        now: ZonedDateTime,
        today: LocalDate,
        results: MutableList<DueOccurrence>
    ) {
        val scheduledZoned = ZonedDateTime.of(today, localTime, zoneId)
        if (scheduledZoned.isAfter(now)) return
        val minutesLate = Duration.between(scheduledZoned, now).toMinutes()
        if (minutesLate > rule.allowedDelayMinutes) return
        results += DueOccurrence(rule, scheduledZoned.toLocalDateTime())
    }
}
