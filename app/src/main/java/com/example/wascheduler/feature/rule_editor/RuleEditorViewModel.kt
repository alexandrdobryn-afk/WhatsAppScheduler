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
import com.example.wascheduler.domain.model.ScheduleType
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
    DATE_REQUIRED,
    DATE_TIME_REQUIRED,
    SAVE_FAILED
}

fun validateRuleEditorState(state: RuleEditorState): List<RuleEditorValidationError> = buildList {
    if (state.chatName.isBlank()) add(RuleEditorValidationError.CHAT_REQUIRED)
    if (state.message.isBlank()) add(RuleEditorValidationError.MESSAGE_REQUIRED)
    when (state.scheduleType) {
        ScheduleType.WEEKLY -> {
            if (state.times.isEmpty()) add(RuleEditorValidationError.TIME_REQUIRED)
            if (state.days.isEmpty()) add(RuleEditorValidationError.DAY_REQUIRED)
        }
        ScheduleType.SPECIFIC_DATE -> {
            if (state.times.isEmpty()) add(RuleEditorValidationError.TIME_REQUIRED)
            if (state.dates.isEmpty()) add(RuleEditorValidationError.DATE_REQUIRED)
        }
        ScheduleType.MULTIPLE_DATES -> {
            if (state.dateTimes.isEmpty()) add(RuleEditorValidationError.DATE_TIME_REQUIRED)
        }
    }
}

data class RuleDateTimeSelection(
    val date: LocalDate,
    val time: LocalTime
)

data class RuleEditorState(
    val ruleId: Long = 0,
    val name: String = "",
    val chatName: String = "",
    val message: String = "",
    val scheduleType: ScheduleType = ScheduleType.WEEKLY,
    val startDate: LocalDate = LocalDate.now(),
    val dates: List<LocalDate> = listOf(LocalDate.now()),
    val times: List<LocalTime> = emptyList(),
    val dateTimes: List<RuleDateTimeSelection> = emptyList(),
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
                        scheduleType = rule.scheduleType,
                        startDate = rule.startDate,
                        dates = rule.dates.ifEmpty { listOf(rule.startDate) },
                        times = if (rule.scheduleType == ScheduleType.MULTIPLE_DATES) {
                            rule.times.map { it.localTime }.distinct().sorted()
                        } else {
                            rule.times.map { it.localTime }.sorted()
                        },
                        dateTimes = rule.toDateTimeSelections(),
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
    fun updateScheduleType(value: ScheduleType) = _state.update {
        val dirty = markDirty(it)
        val seededDateTimes = if (value == ScheduleType.MULTIPLE_DATES && dirty.dateTimes.isEmpty()) {
            buildDateTimeSelections(dirty.dates.ifEmpty { listOf(LocalDate.now()) }, dirty.times)
        } else {
            dirty.dateTimes
        }
        val seededTimes = if (value != ScheduleType.MULTIPLE_DATES && dirty.times.isEmpty() && seededDateTimes.isNotEmpty()) {
            seededDateTimes.map { selection -> selection.time }.distinct().sorted()
        } else {
            dirty.times
        }
        dirty.copy(
            scheduleType = value,
            dates = when {
                value == ScheduleType.WEEKLY -> dirty.dates
                value == ScheduleType.MULTIPLE_DATES && seededDateTimes.isNotEmpty() ->
                    seededDateTimes.map { selection -> selection.date }.distinct().sorted()
                else -> dirty.dates.ifEmpty { listOf(LocalDate.now()) }
            },
            times = seededTimes,
            dateTimes = seededDateTimes
        )
    }
    fun addDate(value: LocalDate) = _state.update { markDirty(it).copy(dates = (it.dates + value).distinct().sorted()) }
    fun removeDate(value: LocalDate) = _state.update {
        val remaining = (it.dates - value).distinct().sorted()
        markDirty(it).copy(dates = remaining)
    }
    fun updateAllowedDelay(minutes: Int) = _state.update { markDirty(it).copy(allowedDelayMinutes = minutes.coerceIn(0, 24 * 60)) }
    fun updateEnabled(enabled: Boolean) = _state.update { markDirty(it).copy(enabled = enabled) }

    fun addTime(time: LocalTime) = _state.update {
        if (time in it.times) it else markDirty(it).copy(times = (it.times + time).sorted())
    }

    fun removeTime(time: LocalTime) = _state.update { markDirty(it).copy(times = it.times - time) }

    fun addDateTime(date: LocalDate, time: LocalTime) = _state.update {
        val selection = RuleDateTimeSelection(date, time)
        if (selection in it.dateTimes) {
            it
        } else {
            val dateTimes = (it.dateTimes + selection).sortedDateTimes()
            markDirty(it).copy(
                dateTimes = dateTimes,
                dates = dateTimes.map { item -> item.date }.distinct().sorted()
            )
        }
    }

    fun removeDateTime(selection: RuleDateTimeSelection) = _state.update {
        val dateTimes = (it.dateTimes - selection).sortedDateTimes()
        markDirty(it).copy(
            dateTimes = dateTimes,
            dates = dateTimes.map { item -> item.date }.distinct().sorted()
        )
    }

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
                    scheduleType = s.scheduleType,
                    startDate = s.effectiveStartDate(),
                    dates = s.persistedDates(),
                    allowedDelayMinutes = s.allowedDelayMinutes,
                    times = s.persistedTimes()
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
        val service = WhatsAppAccessibilityService.awaitConnected(permissionChecker::isAccessibilityServiceEnabled)
            ?: return AutomationResult.Failure(
                if (permissionChecker.isAccessibilityServiceEnabled()) {
                    ErrorCode.ACCESSIBILITY_NOT_CONNECTED
                } else {
                    ErrorCode.ACCESSIBILITY_DISABLED
                }
            )
        val whatsAppPackage = permissionChecker.installedWhatsAppPackage()
            ?: return AutomationResult.Failure(ErrorCode.WHATSAPP_NOT_INSTALLED)
        return if (dryRun) {
            service.runDryRun(chatName, whatsAppPackage)
        } else {
            service.runAutomation(AutomationTask(chatName, message), whatsAppPackage)
        }
    }
}

private fun RuleEditorState.persistedDates(): List<LocalDate> =
    when (scheduleType) {
        ScheduleType.WEEKLY -> emptyList()
        ScheduleType.SPECIFIC_DATE -> dates.distinct().sorted().take(1)
        ScheduleType.MULTIPLE_DATES -> dateTimes.map { it.date }.distinct().sorted()
    }

private fun RuleEditorState.effectiveStartDate(): LocalDate =
    when (scheduleType) {
        ScheduleType.WEEKLY -> startDate
        ScheduleType.SPECIFIC_DATE,
        ScheduleType.MULTIPLE_DATES -> persistedDates().minOrNull() ?: startDate
    }

private fun RuleEditorState.persistedTimes(): List<RuleTime> =
    when (scheduleType) {
        ScheduleType.WEEKLY -> times.map { time ->
            RuleTime(ruleId = ruleId, localTime = time, days = days)
        }
        ScheduleType.SPECIFIC_DATE -> times.map { time ->
            RuleTime(ruleId = ruleId, localTime = time, days = emptySet())
        }
        ScheduleType.MULTIPLE_DATES -> dateTimes.sortedDateTimes().map { selection ->
            RuleTime(
                ruleId = ruleId,
                localDate = selection.date,
                localTime = selection.time,
                days = emptySet()
            )
        }
    }

private fun Rule.toDateTimeSelections(): List<RuleDateTimeSelection> =
    if (scheduleType != ScheduleType.MULTIPLE_DATES) {
        emptyList()
    } else {
        val explicit = times.mapNotNull { time ->
            time.localDate?.let { date -> RuleDateTimeSelection(date, time.localTime) }
        }
        explicit.ifEmpty { buildDateTimeSelections(dates, times.map { it.localTime }) }.sortedDateTimes()
    }

private fun buildDateTimeSelections(
    dates: List<LocalDate>,
    times: List<LocalTime>
): List<RuleDateTimeSelection> =
    dates.distinct().flatMap { date ->
        times.distinct().map { time -> RuleDateTimeSelection(date, time) }
    }.sortedDateTimes()

private fun List<RuleDateTimeSelection>.sortedDateTimes(): List<RuleDateTimeSelection> =
    distinct().sortedWith(compareBy<RuleDateTimeSelection> { it.date }.thenBy { it.time })
