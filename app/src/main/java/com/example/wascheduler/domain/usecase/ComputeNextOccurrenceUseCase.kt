package com.example.wascheduler.domain.usecase

import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.model.RuleTime
import com.example.wascheduler.domain.model.ScheduleType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

data class NextOccurrence(
    val rule: Rule,
    val ruleTime: RuleTime,
    val zonedDateTime: ZonedDateTime
) {
    val instant: Instant get() = zonedDateTime.toInstant()
}

/**
 * Computes the next Instant at which any enabled RuleTime of an enabled Rule
 * should fire, using the selected schedule timezone. Never stores or
 * relies on a previously-computed absolute timestamp — always recomputed from
 * LocalTime + DayOfWeek, so DST transitions and timezone/date changes are
 * handled correctly by construction (spec sections 21-22).
 */
class ComputeNextOccurrenceUseCase @Inject constructor() {

    fun forRule(rule: Rule, zoneId: ZoneId = ZoneId.systemDefault(), now: ZonedDateTime = ZonedDateTime.now(zoneId)): NextOccurrence? {
        if (!rule.enabled) return null
        return rule.times
            .filter { it.enabled }
            .mapNotNull { time -> nextFor(rule, time, zoneId, now)?.let { NextOccurrence(rule, time, it) } }
            .minByOrNull { it.zonedDateTime }
    }

    fun forAllRules(rules: List<Rule>, zoneId: ZoneId = ZoneId.systemDefault(), now: ZonedDateTime = ZonedDateTime.now(zoneId)): NextOccurrence? =
        rules.mapNotNull { forRule(it, zoneId, now) }.minByOrNull { it.zonedDateTime }

    private fun nextFor(rule: Rule, time: RuleTime, zoneId: ZoneId, now: ZonedDateTime): ZonedDateTime? {
        val searchStartDate = maxOf(now.toLocalDate(), rule.startDate)
        for (candidateDate in candidateDates(rule, time, searchStartDate)) {
            val candidate = ZonedDateTime.of(candidateDate, time.localTime, zoneId)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }

    private fun candidateDates(rule: Rule, time: RuleTime, searchStartDate: LocalDate): Sequence<LocalDate> =
        when (rule.scheduleType) {
            ScheduleType.WEEKLY -> {
                if (time.days.isEmpty()) {
                    emptySequence()
                } else {
                    (0..7).asSequence()
                        .map { searchStartDate.plusDays(it.toLong()) }
                        .filter { it.dayOfWeek in time.days }
                }
            }
            ScheduleType.SPECIFIC_DATE,
            ScheduleType.MULTIPLE_DATES -> rule.dates
                .asSequence()
                .distinct()
                .sorted()
                .filter { !it.isBefore(searchStartDate) }
        }
}
