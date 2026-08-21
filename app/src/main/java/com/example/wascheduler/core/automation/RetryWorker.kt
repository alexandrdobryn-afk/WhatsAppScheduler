package com.example.wascheduler.core.automation

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.wascheduler.core.logging.LogComponent
import com.example.wascheduler.core.logging.Logger
import com.example.wascheduler.domain.repository.RuleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_RULE_ID = "ruleId"
private const val KEY_SCHEDULED_AT = "scheduledAt"
private const val KEY_ATTEMPT = "attempt"

/**
 * Retries are not required to be to-the-second exact (spec section 18), so
 * WorkManager's delayed one-off work is an appropriate (and simpler) mechanism
 * here, unlike the primary scheduled sends which use AlarmManager.
 */
@Singleton
class RetryScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val retryPolicy: RetryPolicy
) {
    suspend fun scheduleRetry(ruleId: Long, scheduledAt: LocalDateTime, nextAttempt: Int) {
        val delayMs = retryPolicy.delayBeforeAttempt(nextAttempt) ?: return
        val data = workDataOf(
            KEY_RULE_ID to ruleId,
            KEY_SCHEDULED_AT to scheduledAt.toString(),
            KEY_ATTEMPT to nextAttempt
        )
        val request = OneTimeWorkRequestBuilder<RetryWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        val uniqueName = "retry_${ruleId}_${scheduledAt}"
        WorkManager.getInstance(context).enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
        Logger.i(LogComponent.EXECUTION, "Scheduled retry attempt $nextAttempt in ${delayMs}ms")
    }
}

@HiltWorker
class RetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val ruleRepository: RuleRepository,
    private val automationEngine: AutomationEngine,
    private val retryScheduler: RetryScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ruleId = inputData.getLong(KEY_RULE_ID, -1L)
        val scheduledAtRaw = inputData.getString(KEY_SCHEDULED_AT)
        val attempt = inputData.getInt(KEY_ATTEMPT, 2)
        if (ruleId < 0 || scheduledAtRaw == null) return Result.failure()

        val rule = ruleRepository.getRule(ruleId) ?: return Result.failure()
        if (!rule.enabled) return Result.success()

        val scheduledAt = runCatching { LocalDateTime.parse(scheduledAtRaw) }.getOrNull() ?: return Result.failure()

        val outcome = automationEngine.execute(rule, scheduledAt, attemptNumber = attempt)
        if (outcome is EngineOutcome.Failed && outcome.willRetry) {
            retryScheduler.scheduleRetry(ruleId, scheduledAt, nextAttempt = attempt + 1)
        }
        return Result.success()
    }
}
