package com.example.wascheduler.feature.rule_editor

import com.example.wascheduler.domain.model.DayOfWeekPresets
import com.example.wascheduler.domain.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class RuleEditorValidationTest {

    @Test
    fun `new rule defaults to every day including Sunday`() {
        val state = RuleEditorState()

        assertEquals(DayOfWeekPresets.EVERY_DAY, state.days)
        assertTrue(DayOfWeek.SUNDAY in state.days)
        assertEquals(LocalDate.now(), state.startDate)
    }

    @Test
    fun `validation requires chat message time and at least one day`() {
        val errors = validateRuleEditorState(
            RuleEditorState(
                chatName = " ",
                message = "",
                times = emptyList(),
                days = emptySet()
            )
        )

        assertEquals(
            listOf(
                RuleEditorValidationError.CHAT_REQUIRED,
                RuleEditorValidationError.MESSAGE_REQUIRED,
                RuleEditorValidationError.TIME_REQUIRED,
                RuleEditorValidationError.DAY_REQUIRED
            ),
            errors
        )
    }

    @Test
    fun `valid state with Sunday has no validation errors`() {
        val errors = validateRuleEditorState(
            RuleEditorState(
                chatName = "Team chat",
                message = "Daily report",
                times = listOf(LocalTime.of(9, 0)),
                days = setOf(DayOfWeek.SUNDAY)
            )
        )

        assertTrue(errors.isEmpty())
        assertFalse(errors.contains(RuleEditorValidationError.DAY_REQUIRED))
    }

    @Test
    fun `specific date requires date but not weekday`() {
        val errors = validateRuleEditorState(
            RuleEditorState(
                chatName = "Team chat",
                message = "Daily report",
                scheduleType = ScheduleType.SPECIFIC_DATE,
                dates = emptyList(),
                times = listOf(LocalTime.of(9, 0)),
                days = emptySet()
            )
        )

        assertEquals(listOf(RuleEditorValidationError.DATE_REQUIRED), errors)
    }

    @Test
    fun `multiple dates state accepts multiple concrete dates without weekdays`() {
        val errors = validateRuleEditorState(
            RuleEditorState(
                chatName = "Team chat",
                message = "Daily report",
                scheduleType = ScheduleType.MULTIPLE_DATES,
                dates = listOf(LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 28)),
                times = listOf(LocalTime.of(9, 0)),
                days = emptySet()
            )
        )

        assertTrue(errors.isEmpty())
    }
}
