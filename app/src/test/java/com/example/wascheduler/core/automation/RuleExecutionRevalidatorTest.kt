package com.example.wascheduler.core.automation

import com.example.wascheduler.domain.model.DayOfWeekPresets
import com.example.wascheduler.domain.model.ErrorCode
import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.model.RuleTime
import com.example.wascheduler.domain.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RuleExecutionRevalidatorTest {
    private val scheduledAt = LocalDateTime.of(2026, 9, 7, 9, 0)
    private val activeRule = Rule(
        id = 7,
        name = "Morning",
        chatName = "Project Team",
        message = "Status update",
        enabled = true,
        scheduleType = ScheduleType.WEEKLY,
        startDate = LocalDate.of(2026, 9, 1),
        times = listOf(
            RuleTime(
                ruleId = 7,
                localTime = LocalTime.of(9, 0),
                days = DayOfWeekPresets.WEEKDAYS
            )
        )
    )

    @Test
    fun `uses current rule when occurrence still matches`() {
        val decision = RuleExecutionRevalidator.revalidate(
            snapshot = activeRule,
            current = activeRule.copy(name = "Renamed"),
            scheduledAt = scheduledAt,
            globalAutomationEnabled = true
        )

        assertTrue(decision is RuleRevalidationDecision.UseCurrentRule)
        assertEquals("Renamed", (decision as RuleRevalidationDecision.UseCurrentRule).rule.name)
    }

    @Test
    fun `deleted rule skips without automation`() {
        assertSkipped(current = null, expectedError = ErrorCode.RULE_NOT_ACTIVE)
    }

    @Test
    fun `disabled rule skips without automation`() {
        assertSkipped(current = activeRule.copy(enabled = false), expectedError = ErrorCode.RULE_NOT_ACTIVE)
    }

    @Test
    fun `global automation off skips without automation`() {
        val decision = RuleExecutionRevalidator.revalidate(
            snapshot = activeRule,
            current = activeRule,
            scheduledAt = scheduledAt,
            globalAutomationEnabled = false
        )

        assertTrue(decision is RuleRevalidationDecision.Skip)
        assertSame(activeRule, (decision as RuleRevalidationDecision.Skip).ruleForHistory)
        assertEquals(null, decision.errorCode)
    }

    @Test
    fun `changed message skips stale occurrence`() {
        assertSkipped(current = activeRule.copy(message = "Changed"), expectedError = ErrorCode.STALE_RULE_OCCURRENCE)
    }

    @Test
    fun `changed schedule skips stale occurrence`() {
        val changed = activeRule.copy(
            times = listOf(activeRule.times.first().copy(localTime = LocalTime.of(10, 0)))
        )
        assertSkipped(current = changed, expectedError = ErrorCode.STALE_RULE_OCCURRENCE)
    }

    private fun assertSkipped(current: Rule?, expectedError: ErrorCode) {
        val decision = RuleExecutionRevalidator.revalidate(
            snapshot = activeRule,
            current = current,
            scheduledAt = scheduledAt,
            globalAutomationEnabled = true
        )

        assertTrue(decision is RuleRevalidationDecision.Skip)
        assertEquals(expectedError, (decision as RuleRevalidationDecision.Skip).errorCode)
    }
}
