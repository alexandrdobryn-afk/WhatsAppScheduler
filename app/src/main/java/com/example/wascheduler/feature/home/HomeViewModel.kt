package com.example.wascheduler.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wascheduler.core.permissions.PermissionChecker
import com.example.wascheduler.core.permissions.PermissionState
import com.example.wascheduler.core.scheduler.AlarmScheduler
import com.example.wascheduler.core.scheduler.ScheduleTimeZoneProvider
import com.example.wascheduler.data.repository.SettingsRepository
import com.example.wascheduler.domain.model.Execution
import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.repository.ExecutionRepository
import com.example.wascheduler.domain.repository.RuleRepository
import com.example.wascheduler.domain.usecase.ComputeNextOccurrenceUseCase
import com.example.wascheduler.domain.usecase.NextOccurrence
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val globalEnabled: Boolean = true,
    val permissionState: PermissionState? = null,
    val rules: List<Rule> = emptyList(),
    val nextOccurrence: NextOccurrence? = null,
    val scheduleZoneId: ZoneId = ZoneId.systemDefault(),
    val lastOperation: Execution? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val executionRepository: ExecutionRepository,
    private val settingsRepository: SettingsRepository,
    private val computeNextOccurrence: ComputeNextOccurrenceUseCase,
    private val permissionChecker: PermissionChecker,
    private val alarmScheduler: AlarmScheduler,
    private val scheduleTimeZoneProvider: ScheduleTimeZoneProvider
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.globalAutomationEnabled,
        ruleRepository.observeRules(),
        executionRepository.observeLatest(),
        scheduleTimeZoneProvider.zoneId
    ) { globalEnabled, rules, lastOp, scheduleZoneId ->
        HomeUiState(
            globalEnabled = globalEnabled,
            permissionState = permissionChecker.currentState(),
            rules = rules,
            nextOccurrence = computeNextOccurrence.forAllRules(rules.filter { it.enabled }, scheduleZoneId),
            scheduleZoneId = scheduleZoneId,
            lastOperation = lastOp
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun refreshPermissions() {
        // uiState recomputes permissionChecker.currentState() on every upstream
        // emission; nudge one through by touching settings (cheap, already cached).
        viewModelScope.launch { /* no-op trigger point for pull-to-refresh if added later */ }
    }

    fun setGlobalEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGlobalAutomationEnabled(enabled)
            if (enabled) alarmScheduler.rescheduleNext() else alarmScheduler.cancel()
        }
    }

    fun setRuleEnabled(ruleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            ruleRepository.setRuleEnabled(ruleId, enabled)
            alarmScheduler.rescheduleNext()
        }
    }
}
