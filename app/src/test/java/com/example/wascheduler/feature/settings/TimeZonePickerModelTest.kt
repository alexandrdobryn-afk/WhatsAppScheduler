package com.example.wascheduler.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TimeZonePickerModelTest {
    private val summer = Instant.parse("2026-07-01T12:00:00Z")
    private val winter = Instant.parse("2026-01-01T12:00:00Z")

    @Test
    fun `picker exposes representative IANA zones`() {
        val all = TimeZonePickerModel.options("")
            .map { it.zoneId }
            .toSet()

        listOf(
            "Europe/Kyiv",
            "Europe/London",
            "America/New_York",
            "Asia/Tokyo",
            "Australia/Sydney"
        ).forEach { zoneId ->
            assertTrue("Expected $zoneId in available IANA zones", zoneId in all)
        }
    }

    @Test
    fun `search matches zone id city part and UTC offset`() {
        assertContainsZone(TimeZonePickerModel.options("New_York", summer), "America/New_York")
        assertContainsZone(TimeZonePickerModel.options("Europe", summer), "Europe/Kyiv")
        assertContainsZone(TimeZonePickerModel.options("Tokyo", summer), "Asia/Tokyo")
        assertContainsZone(TimeZonePickerModel.options("+09", summer), "Asia/Tokyo")
        assertContainsZone(TimeZonePickerModel.options("UTC+03", summer), "Europe/Kyiv")
    }

    @Test
    fun `offset labels are computed dynamically for DST zones`() {
        assertEquals("UTC+02:00", TimeZonePickerModel.offsetLabel("Europe/Warsaw", summer))
        assertEquals("UTC+01:00", TimeZonePickerModel.offsetLabel("Europe/Warsaw", winter))
        assertEquals("UTC-04:00", TimeZonePickerModel.offsetLabel("America/New_York", summer))
        assertEquals("UTC-05:00", TimeZonePickerModel.offsetLabel("America/New_York", winter))
    }

    private fun assertContainsZone(options: List<TimeZonePickerOption>, zoneId: String) {
        assertTrue("Expected search results to contain $zoneId", options.any { it.zoneId == zoneId })
    }
}
