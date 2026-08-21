package com.example.wascheduler.feature.diagnostics

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wascheduler.core.accessibility.AutomationResult
import com.example.wascheduler.core.accessibility.AccessibilityConnectionStatus
import com.example.wascheduler.core.accessibility.WhatsAppAccessibilityService
import com.example.wascheduler.core.device.DeviceWakeController
import com.example.wascheduler.core.permissions.PermissionChecker
import com.example.wascheduler.core.permissions.PermissionState
import com.example.wascheduler.core.scheduler.ScheduleTimeZoneProvider
import com.example.wascheduler.domain.model.ErrorCode
import com.example.wascheduler.domain.repository.ExecutionRepository
import com.example.wascheduler.domain.repository.RuleRepository
import com.example.wascheduler.domain.usecase.ComputeNextOccurrenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DiagnosticsUiState(
    val permissionState: PermissionState? = null,
    val accessibilityConnectionStatus: AccessibilityConnectionStatus = AccessibilityConnectionStatus.ENABLED_NOT_CONNECTED,
    val accessibilityServiceConnected: Boolean = false,
    val processPid: Int = android.os.Process.myPid(),
    val screenLabel: String = "ON",
    val keyguardLabel: String = "UNLOCKED",
    val secureLockLabel: String = "NO",
    val autoDismissAvailableLabel: String = "NO",
    val isXiaomiDevice: Boolean = false,
    val activeRuleCount: Int = 0,
    val nextExecutionLabel: String? = null,
    val scheduleZoneId: ZoneId = ZoneId.systemDefault(),
    val deviceZoneId: ZoneId = ZoneId.systemDefault(),
    val lastExecutionLabel: String? = null,
    val lastErrorLabel: String? = null,
    val checkingWhatsApp: Boolean = false,
    val whatsAppCheckResult: String? = null,
    val exportedReportPath: String? = null
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val deviceWakeController: DeviceWakeController,
    private val ruleRepository: RuleRepository,
    private val executionRepository: ExecutionRepository,
    private val computeNextOccurrence: ComputeNextOccurrenceUseCase,
    private val scheduleTimeZoneProvider: ScheduleTimeZoneProvider,
    private val reportExporter: DiagnosticReportExporter
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticsUiState())
    val state: StateFlow<DiagnosticsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val rules = ruleRepository.getAllEnabledRules()
            val scheduleZoneId = scheduleTimeZoneProvider.currentZoneId()
            val next = computeNextOccurrence.forAllRules(rules, scheduleZoneId)
            val recent = executionRepository.observeRecent(1).first().firstOrNull()
            val permissionState = permissionChecker.currentState()
            val connectionSnapshot = WhatsAppAccessibilityService.snapshot(permissionState.accessibilityEnabled)
            val deviceSnapshot = deviceWakeController.snapshot()
            _state.update {
                it.copy(
                    permissionState = permissionState,
                    accessibilityConnectionStatus = connectionSnapshot.status,
                    accessibilityServiceConnected = connectionSnapshot.serviceConnected,
                    processPid = connectionSnapshot.processPid,
                    screenLabel = deviceSnapshot.screenLabel,
                    keyguardLabel = deviceSnapshot.keyguardLabel,
                    secureLockLabel = deviceSnapshot.secureLockLabel,
                    autoDismissAvailableLabel = deviceSnapshot.autoDismissAvailableLabel,
                    isXiaomiDevice = Build.MANUFACTURER.contains("xiaomi", ignoreCase = true),
                    activeRuleCount = rules.size,
                    nextExecutionLabel = next?.zonedDateTime?.format(DateTimeFormatter.ofPattern("dd.MM HH:mm")),
                    scheduleZoneId = scheduleZoneId,
                    deviceZoneId = ZoneId.systemDefault(),
                    lastExecutionLabel = recent?.let { execution ->
                        "${execution.status.name} ${execution.finishedAt ?: execution.startedAt ?: execution.scheduledAt}"
                    },
                    lastErrorLabel = recent?.errorCode?.name
                )
            }
        }
    }

    /** Runs the same node-discovery pipeline as Dry Run, without a specific target chat — just landmark checks. */
    fun checkWhatsAppCompatibility() {
        _state.update { it.copy(checkingWhatsApp = true, whatsAppCheckResult = null) }
        viewModelScope.launch {
            val service = WhatsAppAccessibilityService.instance
            val whatsAppPackage = permissionChecker.installedWhatsAppPackage()
            val resultText = when {
                !permissionChecker.isAccessibilityServiceEnabled() -> ErrorCode.ACCESSIBILITY_DISABLED.stringResKey
                service == null -> ErrorCode.ACCESSIBILITY_NOT_CONNECTED.stringResKey
                whatsAppPackage == null -> ErrorCode.WHATSAPP_NOT_INSTALLED.stringResKey
                else -> when (val result = service.runCompatibilityProbe(whatsAppPackage)) {
                    is AutomationResult.Success -> "action_ok"
                    is AutomationResult.Failure -> result.errorCode.stringResKey
                }
            }
            _state.update { it.copy(checkingWhatsApp = false, whatsAppCheckResult = resultText) }
        }
    }

    fun exportReport() {
        viewModelScope.launch {
            val rules = ruleRepository.getAllEnabledRules()
            val recent = executionRepository.observeRecent(50).first()
            val path = reportExporter.export(permissionChecker.currentState(), rules, recent)
            _state.update { it.copy(exportedReportPath = path) }
        }
    }
}
