package com.example.wascheduler.core.automation

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wascheduler.core.logging.LogComponent
import com.example.wascheduler.core.logging.Logger
import com.example.wascheduler.core.scheduler.AlarmScheduler
import com.example.wascheduler.core.scheduler.ScheduleTimeZoneProvider
import com.example.wascheduler.domain.model.OccurrenceId
import com.example.wascheduler.domain.repository.ExecutionRepository
import com.example.wascheduler.domain.repository.RuleRepository
import com.example.wascheduler.domain.usecase.CollectDueOccurrencesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

/**
 * Runs once per fired alarm (from [com.example.wascheduler.service.AlarmReceiver])
 * or once at boot / package-replace ([com.example.wascheduler.service.BootReceiver]).
 * Collects every occurrence that is currently due, executes them one at a time
 * with a short spacing interval so two simultaneous rules never run
 * concurrently (spec section 76), schedules retries for recoverable failures,
 * and finally recomputes and re-arms the single next alarm — the DB, not
 * AlarmManager, remains the source of truth (spec section 100).
 */
@HiltWorker
class ExecutionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val ruleRepository: RuleRepository,
    private val executionRepository: ExecutionRepository,
    private val collectDueOccurrences: CollectDueOccurrencesUseCase,
    private val automationEngine: AutomationEngine,
    private val alarmScheduler: AlarmScheduler,
    private val retryScheduler: RetryScheduler,
    private val scheduleTimeZoneProvider: ScheduleTimeZoneProvider
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Logger.i(LogComponent.EXECUTION, "ExecutionWorker started")
        val rules = ruleRepository.getAllEnabledRules()
        val scheduleZoneId = scheduleTimeZoneProvider.currentZoneId()
        val due = collectDueOccurrences.collect(rules, scheduleZoneId)

        val unclaimedDue = due.filterNot { occurrence ->
            executionRepository.hasTerminalOrRunning(
                OccurrenceId(occurrence.rule.id, occurrence.scheduledAt).toString()
            )
        }

        unclaimedDue.forEachIndexed { index, occurrence ->
            if (index > 0) delay(SEQUENTIAL_SPACING_MS)
            val outcome = automationEngine.execute(occurrence.rule, occurrence.scheduledAt, attemptNumber = 1)
            if (outcome is EngineOutcome.Failed && outcome.willRetry) {
                retryScheduler.scheduleRetry(occurrence.rule.id, occurrence.scheduledAt, nextAttempt = 2)
            }
        }

        alarmScheduler.rescheduleNext()
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "execution_worker"
        private const val SEQUENTIAL_SPACING_MS = 1_500L
    }
}
