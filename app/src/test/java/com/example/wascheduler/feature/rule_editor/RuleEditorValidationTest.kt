package com.example.wascheduler.feature.rule_editor

import com.example.wascheduler.domain.model.DayOfWeekPresets
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
}
