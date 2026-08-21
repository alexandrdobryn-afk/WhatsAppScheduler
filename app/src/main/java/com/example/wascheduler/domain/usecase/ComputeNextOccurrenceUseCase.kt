package com.example.wascheduler.domain.usecase

import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.model.RuleTime
import java.time.Instant
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
        if (time.days.isEmpty()) return null
        val searchStartDate = maxOf(now.toLocalDate(), rule.startDate)
        // Search the next 8 days (today + one full week) for the first day whose
        // weekday is enabled and whose time-of-day is still in the future.
        for (dayOffset in 0..7) {
            val candidateDate = searchStartDate.plusDays(dayOffset.toLong())
            if (candidateDate.dayOfWeek !in time.days) continue
            val candidate = ZonedDateTime.of(candidateDate, time.localTime, zoneId)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }
}
