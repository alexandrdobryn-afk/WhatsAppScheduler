package com.example.wascheduler.domain.usecase

import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.model.RuleTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class CollectDueOccurrencesUseCaseTest {

    private val zone = ZoneId.of("Europe/Kyiv")
    private val useCase = CollectDueOccurrencesUseCase()
    private val defaultStartDate = LocalDate.of(2026, 1, 1)

    private fun rule(time: LocalTime, delay: Int, days: Set<DayOfWeek> = setOf(DayOfWeek.TUESDAY)) = Rule(
        id = 1, name = "r", chatName = "c", message = "m", enabled = true, startDate = defaultStartDate, allowedDelayMinutes = delay,
        times = listOf(RuleTime(ruleId = 1, localTime = time, days = days))
    )

    @Test
    fun `occurrence within allowed delay is due`() {
        val now = ZonedDateTime.of(2026, 3, 10, 9, 5, 0, 0, zone) // Tuesday, 5 min after 09:00
        val rule = rule(LocalTime.of(9, 0), delay = 10)
        val due = useCase.collect(listOf(rule), zone, now)
        assertEquals(1, due.size)
    }

    @Test
    fun `occurrence beyond allowed delay is not due (spec section 38, MISSED_WINDOW handled upstream)`() {
        val now = ZonedDateTime.of(2026, 3, 10, 9, 15, 0, 0, zone) // 15 min after 09:00
        val rule = rule(LocalTime.of(9, 0), delay = 10)
        val due = useCase.collect(listOf(rule), zone, now)
        assertTrue(due.isEmpty())
    }

    @Test
    fun `future occurrence today is not yet due`() {
        val now = ZonedDateTime.of(2026, 3, 10, 8, 0, 0, 0, zone)
        val rule = rule(LocalTime.of(9, 0), delay = 10)
        val due = useCase.collect(listOf(rule), zone, now)
        assertTrue(due.isEmpty())
    }

    @Test
    fun `disabled rule is never due`() {
        val now = ZonedDateTime.of(2026, 3, 10, 9, 5, 0, 0, zone)
        val rule = rule(LocalTime.of(9, 0), delay = 10).copy(enabled = false)
        val due = useCase.collect(listOf(rule), zone, now)
        assertTrue(due.isEmpty())
    }

    @Test
    fun `two rules due at the same time are both returned for sequential execution`() {
        val now = ZonedDateTime.of(2026, 3, 10, 9, 1, 0, 0, zone)
        val ruleA = rule(LocalTime.of(9, 0), delay = 10).copy(id = 1, chatName = "A")
            .let { it.copy(times = it.times.map { t -> t.copy(ruleId = 1) }) }
        val ruleB = rule(LocalTime.of(9, 0), delay = 10).copy(id = 2, chatName = "B")
            .let { it.copy(times = it.times.map { t -> t.copy(ruleId = 2) }) }
        val due = useCase.collect(listOf(ruleA, ruleB), zone, now)
        assertEquals(2, due.size)
    }

    @Test
    fun `occurrence before rule start date is not due`() {
        val now = ZonedDateTime.of(2026, 3, 10, 9, 5, 0, 0, zone)
        val rule = rule(LocalTime.of(9, 0), delay = 10).copy(startDate = LocalDate.of(2026, 3, 11))

        val due = useCase.collect(listOf(rule), zone, now)

        assertTrue(due.isEmpty())
    }
}
