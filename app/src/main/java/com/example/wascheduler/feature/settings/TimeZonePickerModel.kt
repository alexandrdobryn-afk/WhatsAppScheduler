package com.example.wascheduler.feature.settings

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class TimeZonePickerOption(
    val zoneId: String,
    val offsetLabel: String
)

object TimeZonePickerModel {
    private val allZoneIds: List<String> by lazy {
        ZoneId.getAvailableZoneIds()
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.replace('_', ' ') })
    }

    fun options(query: String, instant: Instant = Instant.now()): List<TimeZonePickerOption> {
        val normalized = query.trim()
        return allZoneIds
            .asSequence()
            .map { zoneId -> TimeZonePickerOption(zoneId, offsetLabel(zoneId, instant)) }
            .filter { option -> normalized.isBlank() || option.matches(normalized) }
            .toList()
    }

    fun offsetLabel(zoneId: String, instant: Instant = Instant.now()): String {
        val offset = ZonedDateTime.ofInstant(instant, ZoneId.of(zoneId)).offset.id
        return "UTC${if (offset == "Z") "+00:00" else offset}"
    }

    private fun TimeZonePickerOption.matches(query: String): Boolean {
        val normalized = query.lowercase()
        val id = zoneId.lowercase()
        val city = zoneId.substringAfterLast('/').replace('_', ' ').lowercase()
        val offset = offsetLabel.lowercase()
        val offsetCompact = offset.replace("utc", "").replace(":", "")
        val queryCompact = normalized.replace("utc", "").replace(":", "").replace(" ", "")

        return id.contains(normalized) ||
            city.contains(normalized.replace('_', ' ')) ||
            offset.contains(normalized) ||
            offsetCompact.contains(queryCompact)
    }
}
