package com.example.wascheduler.core.automation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ExecutionWindowPolicyTest {
    private val scheduledAt = LocalDateTime.of(2026, 9, 7, 2, 34, 0)

    @Test
    fun `zero allowed delay permits only the scheduled minute`() {
        assertTrue(ExecutionWindowPolicy.isWithinWindow(scheduledAt, scheduledAt, 0))
        assertTrue(ExecutionWindowPolicy.isWithinWindow(scheduledAt, scheduledAt.plusSeconds(1), 0))
        assertTrue(ExecutionWindowPolicy.isWithinWindow(scheduledAt, scheduledAt.plusSeconds(30), 0))
        assertTrue(ExecutionWindowPolicy.isWithinWindow(scheduledAt, scheduledAt.plusSeconds(59), 0))
        assertFalse(ExecutionWindowPolicy.isWithinWindow(scheduledAt, scheduledAt.plusMinutes(1), 0))
    }

    @Test
    fun `one minute allowed delay includes the next minute only`() {
        assertTrue(ExecutionWindowPolicy.isWithinWindow(scheduledAt, scheduledAt.plusMinutes(1).plusSeconds(59), 1))
        assertFalse(ExecutionWindowPolicy.isWithinWindow(scheduledAt, scheduledAt.plusMinutes(2), 1))
    }

    @Test
    fun `five minute allowed delay includes minute five only`() {
        assertTrue(ExecutionWindowPolicy.isWithinWindow(scheduledAt, scheduledAt.plusMinutes(5).plusSeconds(59), 5))
        assertFalse(ExecutionWindowPolicy.isWithinWindow(scheduledAt, scheduledAt.plusMinutes(6), 5))
    }

    @Test
    fun `time before scheduled minute is outside window`() {
        assertFalse(ExecutionWindowPolicy.isWithinWindow(scheduledAt, scheduledAt.minusSeconds(1), 10))
    }
}
