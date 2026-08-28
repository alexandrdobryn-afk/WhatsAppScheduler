package com.example.wascheduler.domain.usecase

import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.model.RuleTime
import com.example.wascheduler.domain.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ComputeNextOccurrenceUseCaseTest {

    private val zone = ZoneId.of("Europe/Kyiv")
    private val useCase = ComputeNextOccurrenceUseCase()
    private val defaultStartDate = LocalDate.of(2026, 1, 1)

    private fun rule(
        vararg times: RuleTime,
        enabled: Boolean = true,
        scheduleType: ScheduleType = ScheduleType.WEEKLY,
        dates: List<LocalDate> = emptyList()
    ) =
        Rule(
            id = 1,
            name = "r",
            chatName = "c",
            message = "m",
            enabled = enabled,
            scheduleType = scheduleType,
            startDate = defaultStartDate,
            dates = dates,
            times = times.toList()
        )

    @Test
    fun `finds nearest time later today`() {
        val now = ZonedDateTime.of(2026, 3, 10, 8, 0, 0, 0, zone) // Tuesday
        val rule = rule(RuleTime(ruleId = 1, localTime = LocalTime.of(9, 0), days = setOf(DayOfWeek.TUESDAY)))
        val next = useCase.forRule(rule, zone, now)
        assertEquals(LocalTime.of(9, 0), next?.zonedDateTime?.toLocalTime())
        assertEquals(now.toLocalDate(), next?.zonedDateTime?.toLocalDate())
    }

    @Test
    fun `rolls over to next enabled day when today's time already passed`() {
        val now = ZonedDateTime.of(2026, 3, 10, 10, 0, 0, 0, zone) // Tuesday, after 09:00
        val rule = rule(RuleTime(ruleId = 1, localTime = LocalTime.of(9, 0), days = setOf(DayOfWeek.TUESDAY)))
        val next = useCase.forRule(rule, zone, now)
        // Next Tuesday is 7 days later.
        assertEquals(now.toLocalDate().plusDays(7), next?.zonedDateTime?.toLocalDate())
    }

    @Test
    fun `rolls over to end of week correctly for Mon-Fri rule queried on Friday evening`() {
        val now = ZonedDateTime.of(2026, 3, 13, 20, 0, 0, 0, zone) // Friday evening
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val rule = rule(RuleTime(ruleId = 1, localTime = LocalTime.of(9, 0), days = days))
        val next = useCase.forRule(rule, zone, now)
        assertEquals(DayOfWeek.MONDAY, next?.zonedDateTime?.dayOfWeek)
    }

    @Test
    fun `disabled rule yields no occurrence`() {
        val rule = rule(RuleTime(ruleId = 1, localTime = LocalTime.of(9, 0), days = setOf(DayOfWeek.MONDAY)), enabled = false)
        assertNull(useCase.forRule(rule))
    }

    @Test
    fun `disabled individual time is skipped`() {
        val rule = rule(
            RuleTime(ruleId = 1, localTime = LocalTime.of(9, 0), days = setOf(DayOfWeek.MONDAY), enabled = false)
        )
        assertNull(useCase.forRule(rule, zone, ZonedDateTime.of(2026, 3, 9, 0, 0, 0, 0, zone)))
    }

    @Test
    fun `multiple times per rule returns the earliest`() {
        val now = ZonedDateTime.of(2026, 3, 10, 7, 0, 0, 0, zone)
        val rule = rule(
            RuleTime(ruleId = 1, localTime = LocalTime.of(9, 0), days = setOf(DayOfWeek.TUESDAY)),
            RuleTime(ruleId = 1, localTime = LocalTime.of(8, 0), days = setOf(DayOfWeek.TUESDAY)),
            RuleTime(ruleId = 1, localTime = LocalTime.of(12, 0), days = setOf(DayOfWeek.TUESDAY))
        )
        val next = useCase.forRule(rule, zone, now)
        assertEquals(LocalTime.of(8, 0), next?.zonedDateTime?.toLocalTime())
    }

    @Test
    fun `forAllRules picks the earliest across rules`() {
        val now = ZonedDateTime.of(2026, 3, 10, 7, 0, 0, 0, zone)
        val ruleA = Rule(
            id = 1, name = "A", chatName = "GroupA", message = "m", enabled = true,
            startDate = defaultStartDate,
            times = listOf(RuleTime(ruleId = 1, localTime = LocalTime.of(9, 0), days = setOf(DayOfWeek.TUESDAY)))
        )
        val ruleB = Rule(
            id = 2, name = "B", chatName = "GroupB", message = "m", enabled = true,
            startDate = defaultStartDate,
            times = listOf(RuleTime(ruleId = 2, localTime = LocalTime.of(8, 30), days = setOf(DayOfWeek.TUESDAY)))
        )
        val next = useCase.forAllRules(listOf(ruleA, ruleB), zone, now)
        assertEquals("GroupB", next?.rule?.chatName)
    }

    @Test
    fun `handles spring-forward DST transition without throwing`() {
        // Europe/Kyiv springs forward on the last Sunday of March.
        val now = ZonedDateTime.of(2026, 3, 28, 0, 30, 0, 0, zone)
        val rule = rule(RuleTime(ruleId = 1, localTime = LocalTime.of(9, 0), days = DayOfWeek.entries.toSet()))
        val next = useCase.forRule(rule, zone, now)
        assertEquals(LocalTime.of(9, 0), next?.zonedDateTime?.toLocalTime())
    }

    @Test
    fun `custom schedule zone controls Sunday next occurrence`() {
        val scheduleZone = ZoneId.of("Pacific/Auckland")
        val now = ZonedDateTime.of(2026, 8, 16, 7, 30, 0, 0, scheduleZone) // Sunday
        val rule = rule(RuleTime(ruleId = 1, localTime = LocalTime.of(8, 0), days = setOf(DayOfWeek.SUNDAY)))

        val next = useCase.forRule(rule, scheduleZone, now)

        assertEquals(DayOfWeek.SUNDAY, next?.zonedDateTime?.dayOfWeek)
        assertEquals(LocalTime.of(8, 0), next?.zonedDateTime?.toLocalTime())
        assertEquals(scheduleZone, next?.zonedDateTime?.zone)
    }

    @Test
    fun `does not schedule before rule start date`() {
        val now = ZonedDateTime.of(2026, 3, 10, 8, 0, 0, 0, zone) // Tuesday
        val rule = rule(RuleTime(ruleId = 1, localTime = LocalTime.of(9, 0), days = setOf(DayOfWeek.TUESDAY)))
            .copy(startDate = LocalDate.of(2026, 3, 17))

        val next = useCase.forRule(rule, zone, now)

        assertEquals(LocalDate.of(2026, 3, 17), next?.zonedDateTime?.toLocalDate())
        assertEquals(LocalTime.of(9, 0), next?.zonedDateTime?.toLocalTime())
    }

    @Test
    fun `can schedule start date beyond one week from now`() {
        val now = ZonedDateTime.of(2026, 3, 10, 8, 0, 0, 0, zone) // Tuesday
        val startDate = LocalDate.of(2026, 4, 1)
        val rule = rule(RuleTime(ruleId = 1, localTime = LocalTime.of(9, 0), days = setOf(DayOfWeek.WEDNESDAY)))
            .copy(startDate = startDate)

        val next = useCase.forRule(rule, zone, now)

        assertEquals(startDate, next?.zonedDateTime?.toLocalDate())
    }

    @Test
    fun `specific date returns occurrence on that date`() {
        val now = ZonedDateTime.of(2026, 8, 24, 10, 0, 0, 0, zone)
        val date = LocalDate.of(2026, 8, 25)
        val rule = rule(
            RuleTime(ruleId = 1, localTime = LocalTime.of(14, 30), days = emptySet()),
            scheduleType = ScheduleType.SPECIFIC_DATE,
            dates = listOf(date)
        )

        val next = useCase.forRule(rule, zone, now)

        assertEquals(date, next?.zonedDateTime?.toLocalDate())
        assertEquals(LocalTime.of(14, 30), next?.zonedDateTime?.toLocalTime())
    }

    @Test
    fun `specific date has no next occurrence after its final time`() {
        val date = LocalDate.of(2026, 8, 25)
        val now = ZonedDateTime.of(2026, 8, 25, 15, 0, 0, 0, zone)
        val rule = rule(
            RuleTime(ruleId = 1, localTime = LocalTime.of(14, 30), days = emptySet()),
            scheduleType = ScheduleType.SPECIFIC_DATE,
            dates = listOf(date)
        )

        assertNull(useCase.forRule(rule, zone, now))
    }

    @Test
    fun `multiple dates returns earliest future date time pair`() {
        val now = ZonedDateTime.of(2026, 8, 25, 15, 0, 0, 0, zone)
        val rule = rule(
            RuleTime(ruleId = 1, localTime = LocalTime.of(9, 0), days = emptySet()),
            RuleTime(ruleId = 1, localTime = LocalTime.of(18, 0), days = emptySet()),
            scheduleType = ScheduleType.MULTIPLE_DATES,
            dates = listOf(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 28), LocalDate.of(2026, 8, 25))
        )

        val next = useCase.forRule(rule, zone, now)

        assertEquals(LocalDate.of(2026, 8, 25), next?.zonedDateTime?.toLocalDate())
        assertEquals(LocalTime.of(18, 0), next?.zonedDateTime?.toLocalTime())
    }

    @Test
    fun `weekend preset includes Sunday next occurrence`() {
        val now = ZonedDateTime.of(2026, 8, 14, 12, 0, 0, 0, zone) // Friday
        val rule = rule(RuleTime(ruleId = 1, localTime = LocalTime.of(8, 0), days = com.example.wascheduler.domain.model.DayOfWeekPresets.WEEKENDS))

        val next = useCase.forRule(rule, zone, now)

        assertEquals(DayOfWeek.SATURDAY, next?.zonedDateTime?.dayOfWeek)
    }
}
