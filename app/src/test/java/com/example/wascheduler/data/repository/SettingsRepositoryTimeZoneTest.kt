package com.example.wascheduler.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class SettingsRepositoryTimeZoneTest {
    @Test
    fun `custom mode resolves stored IANA ZoneId`() {
        val resolved = resolveScheduleZoneId(
            mode = ScheduleTimeZoneMode.CUSTOM,
            customTimeZoneId = "Asia/Tokyo",
            deviceZoneId = ZoneId.of("Europe/Kyiv")
        )

        assertEquals(ZoneId.of("Asia/Tokyo"), resolved)
    }

    @Test
    fun `device mode uses device time zone even when custom zone exists`() {
        val resolved = resolveScheduleZoneId(
            mode = ScheduleTimeZoneMode.DEVICE,
            customTimeZoneId = "America/New_York",
            deviceZoneId = ZoneId.of("Europe/London")
        )

        assertEquals(ZoneId.of("Europe/London"), resolved)
    }

    @Test
    fun `invalid stored custom ZoneId falls back to device zone and reports invalid value`() {
        val invalidValues = mutableListOf<String>()
        val resolved = resolveScheduleZoneId(
            mode = ScheduleTimeZoneMode.CUSTOM,
            customTimeZoneId = "Not/A_Real_Zone",
            deviceZoneId = ZoneId.of("Australia/Sydney"),
            onInvalidCustomZoneId = { stored, _ -> invalidValues += stored }
        )

        assertEquals(ZoneId.of("Australia/Sydney"), resolved)
        assertTrue(invalidValues.contains("Not/A_Real_Zone"))
    }
}
