package com.example.wascheduler.core.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.wascheduler.core.logging.LogComponent
import com.example.wascheduler.core.logging.Logger
import com.example.wascheduler.core.permissions.PermissionChecker
import com.example.wascheduler.data.repository.SettingsRepository
import com.example.wascheduler.domain.model.OccurrenceId
import com.example.wascheduler.domain.repository.ExecutionRepository
import com.example.wascheduler.domain.repository.RuleRepository
import com.example.wascheduler.domain.usecase.CollectDueOccurrencesUseCase
import com.example.wascheduler.domain.usecase.ComputeNextOccurrenceUseCase
import com.example.wascheduler.service.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps AlarmManager to schedule exactly one alarm: the single nearest next
 * occurrence across every enabled rule. AlarmManager is never treated as the
 * source of truth (spec section 100) — every reschedule recomputes from the
 * current DB state via [RuleRepository] + [ComputeNextOccurrenceUseCase], so a
 * stale or lost alarm can always be rebuilt from scratch.
 *
 * WorkManager is deliberately NOT used for this step (spec section 18): it does
 * not guarantee close-to-the-second timing. AlarmManager's exact-alarm APIs are
 * used instead, gated on the user actually having granted that permission.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ruleRepository: RuleRepository,
    private val executionRepository: ExecutionRepository,
    private val computeNextOccurrence: ComputeNextOccurrenceUseCase,
    private val collectDueOccurrences: CollectDueOccurrencesUseCase,
    private val permissionChecker: PermissionChecker,
    private val settingsRepository: SettingsRepository,
    private val scheduleTimeZoneProvider: ScheduleTimeZoneProvider
) {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val pendingIntent: PendingIntent
        get() {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_SCHEDULED_TASK
            }
            return PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }

    /** Recomputes the nearest next occurrence from the DB and (re)schedules the single alarm for it. */
    suspend fun rescheduleNext() {
        cancel()
        if (!settingsRepository.globalAutomationEnabled.first()) {
            Logger.i(LogComponent.SCHEDULER, "Global automation disabled — leaving alarms cancelled")
            return
        }
        val zoneId = scheduleTimeZoneProvider.currentZoneId()
        val rules = ruleRepository.getAllEnabledRules()

        if (!permissionChecker.canScheduleExactAlarms()) {
            Logger.w(LogComponent.SCHEDULER, "Exact alarm permission missing — cannot schedule next occurrence")
            return
        }

        val due = collectDueOccurrences.collect(rules, zoneId).filterNot { occurrence ->
            executionRepository.hasTerminalOrRunning(
                OccurrenceId(occurrence.rule.id, occurrence.scheduledAt).toString()
            )
        }
        val triggerAtMillis = if (due.isNotEmpty()) {
            Logger.i(LogComponent.SCHEDULER, "Due occurrence exists — scheduling immediate execution")
            System.currentTimeMillis() + IMMEDIATE_TRIGGER_DELAY_MS
        } else {
            val next = computeNextOccurrence.forAllRules(rules, zoneId) ?: run {
                Logger.i(LogComponent.SCHEDULER, "No enabled rule times — nothing to schedule")
                return
            }
            next.instant.toEpochMilli()
        }
        runCatching {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }.onFailure {
            Logger.e(LogComponent.SCHEDULER, "Failed to schedule exact alarm", it)
        }.onSuccess {
            Logger.i(LogComponent.SCHEDULER, "Scheduled next alarm")
        }
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        private const val REQUEST_CODE = 1001
        private const val IMMEDIATE_TRIGGER_DELAY_MS = 1_000L
        const val ACTION_SCHEDULED_TASK = "io.github.alexandrdobryn.waschedule.action.SCHEDULED_TASK"
    }
}
