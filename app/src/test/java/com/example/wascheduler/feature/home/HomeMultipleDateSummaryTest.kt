package com.example.wascheduler.feature.home

import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.model.RuleTime
import com.example.wascheduler.domain.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class HomeMultipleDateSummaryTest {
    @Test
    fun `one pair has one badge and no overflow count`() {
        val summary = multipleDateTimeBadgeSummary(ruleWithPairs(1))

        assertEquals(listOf("07.09.2026 · 02:23"), summary.visibleBadges)
        assertEquals(0, summary.hiddenCount)
    }

    @Test
    fun `two pairs are both visible`() {
        val summary = multipleDateTimeBadgeSummary(ruleWithPairs(2))

        assertEquals(2, summary.visibleBadges.size)
        assertEquals(0, summary.hiddenCount)
    }

    @Test
    fun `four pairs are all visible`() {
        val summary = multipleDateTimeBadgeSummary(ruleWithPairs(4))

        assertEquals(4, summary.visibleBadges.size)
        assertEquals(0, summary.hiddenCount)
    }

    @Test
    fun `five pairs show first four and one hidden`() {
        val summary = multipleDateTimeBadgeSummary(ruleWithPairs(5))

        assertEquals(4, summary.visibleBadges.size)
        assertEquals(1, summary.hiddenCount)
    }

    @Test
    fun `seven pairs show first four and three hidden`() {
        val summary = multipleDateTimeBadgeSummary(ruleWithPairs(7))

        assertEquals(4, summary.visibleBadges.size)
        assertEquals(3, summary.hiddenCount)
    }

    private fun ruleWithPairs(count: Int): Rule {
        val date = LocalDate.of(2026, 9, 7)
        return Rule(
            id = 1,
            name = "Review",
            chatName = "Team",
            message = "Check",
            scheduleType = ScheduleType.MULTIPLE_DATES,
            dates = List(count) { date.plusDays(it.toLong()) },
            times = List(count) { index ->
                RuleTime(
                    ruleId = 1,
                    localDate = date.plusDays(index.toLong()),
                    localTime = LocalTime.of(2, 23 + index),
                    days = emptySet()
                )
            }
        )
    }
}
