package com.example.wascheduler.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.wascheduler.core.logging.LogComponent
import com.example.wascheduler.core.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "wascheduler_settings")

enum class AppTheme { SYSTEM, LIGHT, DARK }
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    RU("ru"),
    UK("uk"),
    EN("en")
}
enum class ScheduleTimeZoneMode { DEVICE, CUSTOM }

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore = context.dataStore

    private object Keys {
        val GLOBAL_ENABLED = booleanPreferencesKey("global_automation_enabled")
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val SCHEDULE_TIME_ZONE_MODE = stringPreferencesKey("schedule_time_zone_mode")
        val CUSTOM_TIME_ZONE_ID = stringPreferencesKey("custom_time_zone_id")
        val NOTIFY_ON_SUCCESS = booleanPreferencesKey("notify_on_success")
        val MAX_RETRY_ATTEMPTS = stringPreferencesKey("max_retry_attempts")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    val globalAutomationEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.GLOBAL_ENABLED] ?: true }

    suspend fun setGlobalAutomationEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.GLOBAL_ENABLED] = enabled }
    }

    val theme: Flow<AppTheme> =
        dataStore.data.map { prefs ->
            prefs[Keys.THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM
        }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[Keys.THEME] = theme.name }
    }

    val language: Flow<AppLanguage> =
        dataStore.data.map { prefs ->
            prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.SYSTEM
        }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    val scheduleTimeZoneMode: Flow<ScheduleTimeZoneMode> =
        dataStore.data.map { prefs ->
            prefs[Keys.SCHEDULE_TIME_ZONE_MODE]?.let {
                runCatching { ScheduleTimeZoneMode.valueOf(it) }.getOrNull()
            } ?: ScheduleTimeZoneMode.DEVICE
        }

    val customTimeZoneId: Flow<String> =
        dataStore.data.map { it[Keys.CUSTOM_TIME_ZONE_ID] ?: DEFAULT_CUSTOM_ZONE_ID }

    val scheduleZoneId: Flow<ZoneId> =
        dataStore.data.map { prefs ->
            val mode = prefs[Keys.SCHEDULE_TIME_ZONE_MODE]?.let {
                runCatching { ScheduleTimeZoneMode.valueOf(it) }.getOrNull()
            } ?: ScheduleTimeZoneMode.DEVICE
            resolveScheduleZoneId(mode, prefs[Keys.CUSTOM_TIME_ZONE_ID])
        }

    suspend fun setScheduleTimeZoneMode(mode: ScheduleTimeZoneMode) {
        dataStore.edit { it[Keys.SCHEDULE_TIME_ZONE_MODE] = mode.name }
    }

    suspend fun setCustomTimeZoneId(zoneId: String) {
        val parsed = ZoneId.of(zoneId)
        dataStore.edit {
            it[Keys.CUSTOM_TIME_ZONE_ID] = parsed.id
            it[Keys.SCHEDULE_TIME_ZONE_MODE] = ScheduleTimeZoneMode.CUSTOM.name
        }
    }

    val notifyOnSuccess: Flow<Boolean> =
        dataStore.data.map { it[Keys.NOTIFY_ON_SUCCESS] ?: true }

    suspend fun setNotifyOnSuccess(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFY_ON_SUCCESS] = enabled }
    }

    val maxRetryAttempts: Flow<Int> =
        dataStore.data.map { it[Keys.MAX_RETRY_ATTEMPTS]?.toIntOrNull() ?: 3 }

    suspend fun setMaxRetryAttempts(count: Int) {
        dataStore.edit { it[Keys.MAX_RETRY_ATTEMPTS] = count.coerceIn(0, 3).toString() }
    }

    companion object {
        const val DEFAULT_CUSTOM_ZONE_ID = "Europe/Kyiv"
    }
}

internal fun resolveScheduleZoneId(
    mode: ScheduleTimeZoneMode,
    customTimeZoneId: String?,
    deviceZoneId: ZoneId = ZoneId.systemDefault(),
    onInvalidCustomZoneId: (String, Throwable) -> Unit = { stored, throwable ->
        Logger.w(LogComponent.SCHEDULER, "Invalid stored schedule ZoneId '$stored'; falling back to device time zone", throwable)
    }
): ZoneId =
    if (mode == ScheduleTimeZoneMode.CUSTOM) {
        customTimeZoneId?.let { stored ->
            runCatching { ZoneId.of(stored) }
                .onFailure { onInvalidCustomZoneId(stored, it) }
                .getOrNull()
        } ?: deviceZoneId
    } else {
        deviceZoneId
    }
