package com.example.wascheduler.core.automation

import com.example.wascheduler.domain.model.ErrorCode
import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.model.ScheduleType
import java.time.LocalDateTime

sealed class RuleRevalidationDecision {
    data class UseCurrentRule(val rule: Rule) : RuleRevalidationDecision()
    data class Skip(val ruleForHistory: Rule, val errorCode: ErrorCode?) : RuleRevalidationDecision()
}

object RuleExecutionRevalidator {
    fun revalidate(
        snapshot: Rule,
        current: Rule?,
        scheduledAt: LocalDateTime,
        globalAutomationEnabled: Boolean
    ): RuleRevalidationDecision {
        if (!globalAutomationEnabled) {
            return RuleRevalidationDecision.Skip(snapshot, null)
        }
        val freshRule = current ?: return RuleRevalidationDecision.Skip(snapshot, ErrorCode.RULE_NOT_ACTIVE)
        if (!freshRule.enabled) {
            return RuleRevalidationDecision.Skip(freshRule, ErrorCode.RULE_NOT_ACTIVE)
        }
        if (freshRule.chatName != snapshot.chatName || freshRule.message != snapshot.message) {
            return RuleRevalidationDecision.Skip(freshRule, ErrorCode.STALE_RULE_OCCURRENCE)
        }
        if (!freshRule.containsOccurrence(scheduledAt)) {
            return RuleRevalidationDecision.Skip(freshRule, ErrorCode.STALE_RULE_OCCURRENCE)
        }
        return RuleRevalidationDecision.UseCurrentRule(freshRule)
    }

    private fun Rule.containsOccurrence(scheduledAt: LocalDateTime): Boolean {
        val scheduledDate = scheduledAt.toLocalDate()
        val scheduledTime = scheduledAt.toLocalTime()
        if (scheduledDate.isBefore(startDate)) return false
        return when (scheduleType) {
            ScheduleType.WEEKLY -> times.any { time ->
                time.enabled && time.localTime == scheduledTime && scheduledDate.dayOfWeek in time.days
            }
            ScheduleType.SPECIFIC_DATE -> {
                scheduledDate in dates && times.any { time ->
                    time.enabled && time.localTime == scheduledTime
                }
            }
            ScheduleType.MULTIPLE_DATES -> times.any { time ->
                time.enabled && time.localDate == scheduledDate && time.localTime == scheduledTime
            }
        }
    }
}
