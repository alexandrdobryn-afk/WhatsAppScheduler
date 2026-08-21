package com.example.wascheduler.core.scheduler

import com.example.wascheduler.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

interface ScheduleTimeZoneProvider {
    val zoneId: Flow<ZoneId>
    suspend fun currentZoneId(): ZoneId
}

@Singleton
class DataStoreScheduleTimeZoneProvider @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ScheduleTimeZoneProvider {
    override val zoneId: Flow<ZoneId> = settingsRepository.scheduleZoneId

    override suspend fun currentZoneId(): ZoneId = zoneId.first()
}
