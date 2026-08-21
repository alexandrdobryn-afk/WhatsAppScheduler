package com.example.wascheduler.core.automation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.wascheduler.core.accessibility.AutomationResult
import com.example.wascheduler.core.accessibility.AutomationTask
import com.example.wascheduler.core.accessibility.WhatsAppAccessibilityService
import com.example.wascheduler.core.device.DeviceWakeController
import com.example.wascheduler.core.logging.LogComponent
import com.example.wascheduler.core.logging.Logger
import com.example.wascheduler.core.notifications.NotificationHelper
import com.example.wascheduler.core.permissions.PermissionChecker
import com.example.wascheduler.core.scheduler.ScheduleTimeZoneProvider
import com.example.wascheduler.data.repository.SettingsRepository
import com.example.wascheduler.domain.model.ErrorCode
import com.example.wascheduler.domain.model.Execution
import com.example.wascheduler.domain.model.ExecutionStatus
import com.example.wascheduler.domain.model.OccurrenceId
import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.repository.ExecutionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

sealed class EngineOutcome {
    data object Sent : EngineOutcome()
    data class Failed(val errorCode: ErrorCode, val willRetry: Boolean) : EngineOutcome()
    data object Skipped : EngineOutcome()
    /** Another component already owns this occurrence — this call did nothing. */
    data object AlreadyClaimed : EngineOutcome()
}

/**
 * Central pipeline: receive task -> validate -> check state -> launch WhatsApp
 * -> hand off to the Accessibility Adapter -> verify result -> log
 * (spec section 23). This is the ONLY component that touches the Accessibility
 * service; the scheduler never talks to WhatsApp directly.
 */
@Singleton
class AutomationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val executionRepository: ExecutionRepository,
    private val permissionChecker: PermissionChecker,
    private val settingsRepository: SettingsRepository,
    private val retryPolicy: RetryPolicy,
    private val notificationHelper: NotificationHelper,
    private val scheduleTimeZoneProvider: ScheduleTimeZoneProvider,
    private val deviceWakeController: DeviceWakeController
) {

    suspend fun execute(rule: Rule, scheduledAt: LocalDateTime, attemptNumber: Int = 1): EngineOutcome {
        val occurrenceId = OccurrenceId(rule.id, scheduledAt).toString()
        val scheduleZoneId = scheduleTimeZoneProvider.currentZoneId()
        val nowInScheduleZone = ZonedDateTime.now(scheduleZoneId).toLocalDateTime()

        if (!settingsRepository.globalAutomationEnabled.first()) {
            Logger.i(LogComponent.EXECUTION, "Global automation disabled — skipping occurrence")
            return EngineOutcome.Skipped
        }

        val allowedDelay = rule.allowedDelayMinutes.toLong()
        val minutesLate = Duration.between(scheduledAt, nowInScheduleZone).toMinutes()
        if (minutesLate > allowedDelay) {
            recordTerminal(occurrenceId, rule, scheduledAt, attemptNumber, ExecutionStatus.SKIPPED, ErrorCode.MISSED_WINDOW)
            return EngineOutcome.Skipped
        }

        val claimCandidate = Execution(
            occurrenceId = occurrenceId,
            ruleId = rule.id,
            scheduledAt = scheduledAt,
            startedAt = nowInScheduleZone,
            status = ExecutionStatus.RUNNING,
            attemptNumber = attemptNumber,
            targetChat = rule.chatName,
            messagePreview = rule.message.take(80)
        )
        val claimed = executionRepository.tryClaim(claimCandidate)
        if (!claimed) {
            Logger.i(LogComponent.EXECUTION, "Occurrence already RUNNING or SENT — refusing duplicate execution")
            return EngineOutcome.AlreadyClaimed
        }

        val devicePreparation = deviceWakeController.prepareForScheduledSend()
        try {
            if (devicePreparation.errorCode != null) {
                return finishWithFailure(occurrenceId, rule, scheduledAt, attemptNumber, devicePreparation.errorCode)
            }

            val precheckError = runPrechecks()
            if (precheckError != null) {
                return finishWithFailure(occurrenceId, rule, scheduledAt, attemptNumber, precheckError)
            }

            val service = WhatsAppAccessibilityService.awaitConnected(
                isPermissionEnabled = permissionChecker::isAccessibilityServiceEnabled,
                timeoutMs = ACCESSIBILITY_BIND_TIMEOUT_MS
            )
            if (service == null) {
                val error = if (permissionChecker.isAccessibilityServiceEnabled()) {
                    ErrorCode.ACCESSIBILITY_NOT_CONNECTED
                } else {
                    ErrorCode.ACCESSIBILITY_DISABLED
                }
                return finishWithFailure(occurrenceId, rule, scheduledAt, attemptNumber, error)
            }

            val whatsAppPackage = permissionChecker.installedWhatsAppPackage()
            if (whatsAppPackage == null) {
                return finishWithFailure(occurrenceId, rule, scheduledAt, attemptNumber, ErrorCode.WHATSAPP_NOT_INSTALLED)
            }

            val result = service.runAutomation(AutomationTask(rule.chatName, rule.message), whatsAppPackage)
            return when (result) {
                is AutomationResult.Success -> {
                    recordTerminal(occurrenceId, rule, scheduledAt, attemptNumber, ExecutionStatus.SENT, null)
                    if (settingsRepository.notifyOnSuccess.first()) {
                        notificationHelper.notifySent(rule.chatName, scheduledAt.toLocalTime().toString())
                    }
                    EngineOutcome.Sent
                }
                is AutomationResult.Failure -> finishWithFailure(occurrenceId, rule, scheduledAt, attemptNumber, result.errorCode)
            }
        } finally {
            devicePreparation.session.close()
        }
    }

    private suspend fun finishWithFailure(
        occurrenceId: String,
        rule: Rule,
        scheduledAt: LocalDateTime,
        attemptNumber: Int,
        errorCode: ErrorCode
    ): EngineOutcome {
        val willRetry = retryPolicy.hasMoreAttempts(attemptNumber)
        recordTerminal(occurrenceId, rule, scheduledAt, attemptNumber, status = ExecutionStatus.FAILED, errorCode = errorCode)
        // Error notifications stay on regardless of the success-notification
        // preference (spec section 55: "errors желательно оставлять включёнными").
        if (!willRetry) {
            notificationHelper.notifyFailed(rule.chatName, scheduledAt.toLocalTime().toString(), errorCode)
        }
        return EngineOutcome.Failed(errorCode, willRetry)
    }

    private suspend fun recordTerminal(
        occurrenceId: String,
        rule: Rule,
        scheduledAt: LocalDateTime,
        attemptNumber: Int,
        status: ExecutionStatus,
        errorCode: ErrorCode?
    ) {
        val existing = executionRepository.getByOccurrenceId(occurrenceId)
        val nowInScheduleZone = ZonedDateTime.now(scheduleTimeZoneProvider.currentZoneId()).toLocalDateTime()
        if (existing == null) {
            // No RUNNING row exists yet (e.g. this occurrence was skipped before it
            // was ever claimed) — tryClaim() also serves as a plain insert here
            // since there is nothing to collide with.
            executionRepository.tryClaim(
                Execution(
                    occurrenceId = occurrenceId,
                    ruleId = rule.id,
                    scheduledAt = scheduledAt,
                    startedAt = nowInScheduleZone,
                    finishedAt = nowInScheduleZone,
                    status = status,
                    attemptNumber = attemptNumber,
                    targetChat = rule.chatName,
                    messagePreview = rule.message.take(80),
                    errorCode = errorCode,
                    errorMessage = errorCode?.let { "See errorCode: ${it.name}" }
                )
            )
            return
        }
        val execution = existing.copy(
            finishedAt = nowInScheduleZone,
            status = status,
            attemptNumber = attemptNumber,
            errorCode = errorCode,
            errorMessage = errorCode?.let { "See errorCode: ${it.name}" }
        )
        executionRepository.update(execution)
    }

    /**
     * Checks the conditions that must hold before we even try to touch WhatsApp.
     * Returns the first applicable [ErrorCode], or null if all prechecks pass.
     * Never bypasses a secure locked device (spec section 42) — that is handled
     * before this method by [DeviceWakeController].
     */
    private fun runPrechecks(): ErrorCode? {
        if (!permissionChecker.isWhatsAppInstalled()) return ErrorCode.WHATSAPP_NOT_INSTALLED
        if (!permissionChecker.isAccessibilityServiceEnabled()) return ErrorCode.ACCESSIBILITY_DISABLED

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (!hasInternet) return ErrorCode.NO_NETWORK

        return null
    }

    companion object {
        private const val ACCESSIBILITY_BIND_TIMEOUT_MS = 10_000L
    }
}
