package com.example.wascheduler.feature.rule_editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wascheduler.core.accessibility.AutomationResult
import com.example.wascheduler.core.accessibility.AutomationTask
import com.example.wascheduler.core.accessibility.WhatsAppAccessibilityService
import com.example.wascheduler.core.permissions.PermissionChecker
import com.example.wascheduler.core.scheduler.AlarmScheduler
import com.example.wascheduler.domain.model.DayOfWeekPresets
import com.example.wascheduler.domain.model.ErrorCode
import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.model.RuleTime
import com.example.wascheduler.domain.repository.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

const val MAX_MESSAGE_LENGTH = 4000

enum class RuleEditorValidationError {
    CHAT_REQUIRED,
    MESSAGE_REQUIRED,
    TIME_REQUIRED,
    DAY_REQUIRED,
    SAVE_FAILED
}

fun validateRuleEditorState(state: RuleEditorState): List<RuleEditorValidationError> = buildList {
    if (state.chatName.isBlank()) add(RuleEditorValidationError.CHAT_REQUIRED)
    if (state.message.isBlank()) add(RuleEditorValidationError.MESSAGE_REQUIRED)
    if (state.times.isEmpty()) add(RuleEditorValidationError.TIME_REQUIRED)
    if (state.days.isEmpty()) add(RuleEditorValidationError.DAY_REQUIRED)
}

data class RuleEditorState(
    val ruleId: Long = 0,
    val name: String = "",
    val chatName: String = "",
    val message: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val times: List<LocalTime> = emptyList(),
    val days: Set<DayOfWeek> = DayOfWeekPresets.EVERY_DAY,
    val allowedDelayMinutes: Int = 10,
    val enabled: Boolean = true,
    val isNew: Boolean = true,
    val isDirty: Boolean = false,
    val validationErrors: List<RuleEditorValidationError> = emptyList(),
    val saveError: RuleEditorValidationError? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val showUnsavedChangesConfirm: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val testSendPreviewVisible: Boolean = false,
    val actionInProgress: Boolean = false,
    val lastActionResult: AutomationResult? = null
)

@HiltViewModel
class RuleEditorViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val alarmScheduler: AlarmScheduler,
    private val permissionChecker: PermissionChecker,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(RuleEditorState())
    val state: StateFlow<RuleEditorState> = _state.asStateFlow()

    init {
        val ruleId = savedStateHandle.get<Long>("ruleId") ?: 0L
        if (ruleId > 0) {
            viewModelScope.launch {
                ruleRepository.getRule(ruleId)?.let { rule ->
                    _state.value = RuleEditorState(
                        ruleId = rule.id,
                        name = rule.name,
                        chatName = rule.chatName,
                        message = rule.message,
                        startDate = rule.startDate,
                        times = rule.times.map { it.localTime }.sorted(),
                        days = rule.times.firstOrNull()?.days ?: DayOfWeekPresets.EVERY_DAY,
                        allowedDelayMinutes = rule.allowedDelayMinutes,
                        enabled = rule.enabled,
                        isNew = false,
                        isDirty = false
                    )
                }
            }
        }
    }

    private fun markDirty(state: RuleEditorState): RuleEditorState =
        state.copy(isDirty = true, validationErrors = emptyList(), saveError = null)

    fun updateName(value: String) = _state.update { markDirty(it).copy(name = value) }
    fun updateChatName(value: String) = _state.update { markDirty(it).copy(chatName = value) }
    fun updateMessage(value: String) = _state.update { markDirty(it).copy(message = value.take(MAX_MESSAGE_LENGTH)) }
    fun updateStartDate(value: LocalDate) = _state.update { markDirty(it).copy(startDate = value) }
    fun updateAllowedDelay(minutes: Int) = _state.update { markDirty(it).copy(allowedDelayMinutes = minutes.coerceIn(0, 24 * 60)) }
    fun updateEnabled(enabled: Boolean) = _state.update { markDirty(it).copy(enabled = enabled) }

    fun addTime(time: LocalTime) = _state.update {
        if (time in it.times) it else markDirty(it).copy(times = (it.times + time).sorted())
    }

    fun removeTime(time: LocalTime) = _state.update { markDirty(it).copy(times = it.times - time) }

    fun toggleDay(day: DayOfWeek) = _state.update {
        markDirty(it).copy(days = if (day in it.days) it.days - day else it.days + day)
    }

    fun applyPreset(days: Set<DayOfWeek>) = _state.update { markDirty(it).copy(days = days) }

    fun requestClose() = _state.update {
        if (it.isDirty) it.copy(showUnsavedChangesConfirm = true) else it.copy(deleted = true)
    }

    fun dismissUnsavedChangesConfirm() = _state.update { it.copy(showUnsavedChangesConfirm = false) }
    fun discardUnsavedChanges() = _state.update { it.copy(isDirty = false, showUnsavedChangesConfirm = false, deleted = true) }

    fun requestDelete() = _state.update { it.copy(showDeleteConfirm = true) }
    fun dismissDeleteConfirm() = _state.update { it.copy(showDeleteConfirm = false) }

    fun confirmDelete() {
        val ruleId = _state.value.ruleId
        if (ruleId == 0L) return
        viewModelScope.launch {
            runCatching {
                ruleRepository.deleteRule(ruleId)
                alarmScheduler.rescheduleNext()
            }.onSuccess {
                _state.update { it.copy(isDirty = false, deleted = true, showDeleteConfirm = false) }
            }.onFailure {
                _state.update { it.copy(showDeleteConfirm = false, saveError = RuleEditorValidationError.SAVE_FAILED) }
            }
        }
    }

    fun requestTestSendPreview() = _state.update { it.copy(testSendPreviewVisible = true) }
    fun dismissTestSendPreview() = _state.update { it.copy(testSendPreviewVisible = false) }

    fun save() {
        val s = _state.value
        val validationErrors = validateRuleEditorState(s)
        if (validationErrors.isNotEmpty()) {
            _state.update { it.copy(validationErrors = validationErrors, saveError = null) }
            return
        }
        _state.update { it.copy(isSaving = true, validationErrors = emptyList(), saveError = null) }
        viewModelScope.launch {
            runCatching {
                val rule = Rule(
                    id = s.ruleId,
                    name = s.name.trim(),
                    chatName = s.chatName.trim(),
                    message = s.message,
                    enabled = s.enabled,
                    startDate = s.startDate,
                    allowedDelayMinutes = s.allowedDelayMinutes,
                    times = s.times.map { time -> RuleTime(ruleId = s.ruleId, localTime = time, days = s.days) }
                )
                val persistedRuleId = ruleRepository.upsertRule(rule)
                val persistedRule = ruleRepository.getRule(persistedRuleId)
                require(persistedRule != null && persistedRule.times.isNotEmpty()) {
                    "Saved rule verification failed"
                }
                alarmScheduler.rescheduleNext()
                persistedRuleId
            }.onSuccess { persistedRuleId ->
                _state.update {
                    it.copy(
                        ruleId = persistedRuleId,
                        isNew = false,
                        isDirty = false,
                        isSaving = false,
                        showUnsavedChangesConfirm = false,
                        saved = true
                    )
                }
            }.onFailure {
                _state.update {
                    it.copy(
                        isSaving = false,
                        saveError = RuleEditorValidationError.SAVE_FAILED
                    )
                }
            }
        }
    }

    /** Confirmed test send — the ONE place a real send happens outside the normal schedule (spec section 48). */
    fun confirmTestSend() {
        val s = _state.value
        _state.update { it.copy(testSendPreviewVisible = false, actionInProgress = true, lastActionResult = null) }
        viewModelScope.launch {
            val result = runAutomationAction(s.chatName, s.message, dryRun = false)
            _state.update { it.copy(actionInProgress = false, lastActionResult = result) }
        }
    }

    fun runDryRun() {
        val s = _state.value
        _state.update { it.copy(actionInProgress = true, lastActionResult = null) }
        viewModelScope.launch {
            val result = runAutomationAction(s.chatName, s.message, dryRun = true)
            _state.update { it.copy(actionInProgress = false, lastActionResult = result) }
        }
    }

    private suspend fun runAutomationAction(chatName: String, message: String, dryRun: Boolean): AutomationResult {
        val service = WhatsAppAccessibilityService.instance
            ?: return AutomationResult.Failure(ErrorCode.ACCESSIBILITY_DISABLED)
        val whatsAppPackage = permissionChecker.installedWhatsAppPackage()
            ?: return AutomationResult.Failure(ErrorCode.WHATSAPP_NOT_INSTALLED)
        return if (dryRun) {
            service.runDryRun(chatName, whatsAppPackage)
        } else {
            service.runAutomation(AutomationTask(chatName, message), whatsAppPackage)
        }
    }
}
