package com.example.wascheduler.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class OccurrenceIdTest {

    @Test
    fun `same rule and scheduled time always produce the same id`() {
        val scheduledAt = LocalDateTime.of(2026, 8, 14, 9, 0)
        val idA = OccurrenceId(12, scheduledAt).toString()
        val idB = OccurrenceId(12, scheduledAt).toString()
        assertEquals(idA, idB)
        assertEquals("rule_12_2026-08-14T09:00", idA)
    }

    @Test
    fun `different rule ids never collide`() {
        val scheduledAt = LocalDateTime.of(2026, 8, 14, 9, 0)
        val idA = OccurrenceId(1, scheduledAt).toString()
        val idB = OccurrenceId(2, scheduledAt).toString()
        assertNotEquals(idA, idB)
    }

    @Test
    fun `different times for the same rule never collide`() {
        val idA = OccurrenceId(1, LocalDateTime.of(2026, 8, 14, 9, 0)).toString()
        val idB = OccurrenceId(1, LocalDateTime.of(2026, 8, 14, 10, 0)).toString()
        assertNotEquals(idA, idB)
    }

    @Test
    fun `round-trips through parse`() {
        val original = OccurrenceId(42, LocalDateTime.of(2026, 8, 14, 9, 0))
        val parsed = OccurrenceId.parse(original.toString())
        assertEquals(original, parsed)
    }

    @Test
    fun `parse rejects garbage`() {
        assertNull(OccurrenceId.parse("not_an_occurrence_id"))
    }
}
