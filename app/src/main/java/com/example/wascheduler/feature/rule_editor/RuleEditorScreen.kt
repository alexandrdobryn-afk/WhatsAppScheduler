package com.example.wascheduler.feature.rule_editor

import android.app.TimePickerDialog
import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.wascheduler.R
import com.example.wascheduler.core.accessibility.AutomationResult
import com.example.wascheduler.domain.model.DayOfWeekPresets
import com.example.wascheduler.domain.model.ScheduleType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val dayLabels = mapOf(
    DayOfWeek.MONDAY to R.string.day_monday_short,
    DayOfWeek.TUESDAY to R.string.day_tuesday_short,
    DayOfWeek.WEDNESDAY to R.string.day_wednesday_short,
    DayOfWeek.THURSDAY to R.string.day_thursday_short,
    DayOfWeek.FRIDAY to R.string.day_friday_short,
    DayOfWeek.SATURDAY to R.string.day_saturday_short,
    DayOfWeek.SUNDAY to R.string.day_sunday_short
)

private val startDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleEditorScreen(viewModel: RuleEditorViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var topMenuExpanded by remember { mutableStateOf(false) }

    BackHandler { viewModel.requestClose() }

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isNew) R.string.rule_editor_title_new else R.string.rule_editor_title_edit))
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::requestClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { topMenuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                        }
                        DropdownMenu(
                            expanded = topMenuExpanded,
                            onDismissRequest = { topMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete)) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = {
                                    topMenuExpanded = false
                                    viewModel.requestDelete()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                state.validationErrors.forEach { error ->
                    Text(
                        text = stringResource(error.stringRes),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                state.saveError?.let { error ->
                    Text(
                        text = stringResource(error.stringRes),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.rule_editor_save)) }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::updateName,
                    label = { Text(stringResource(R.string.rule_editor_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.chatName,
                    onValueChange = viewModel::updateChatName,
                    label = { Text(stringResource(R.string.rule_editor_group)) },
                    isError = state.validationErrors.contains(RuleEditorValidationError.CHAT_REQUIRED),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.message,
                    onValueChange = viewModel::updateMessage,
                    label = { Text(stringResource(R.string.rule_editor_message)) },
                    isError = state.validationErrors.contains(RuleEditorValidationError.MESSAGE_REQUIRED),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(stringResource(R.string.rule_editor_schedule_type), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ScheduleType.entries.forEach { type ->
                        FilterChip(
                            selected = state.scheduleType == type,
                            onClick = { viewModel.updateScheduleType(type) },
                            label = { Text(stringResource(type.labelRes)) }
                        )
                    }
                }
            }

            item {
                when (state.scheduleType) {
                    ScheduleType.WEEKLY -> WeeklyDateSection(state, viewModel)
                    ScheduleType.SPECIFIC_DATE -> SpecificDateSection(state, viewModel)
                    ScheduleType.MULTIPLE_DATES -> MultipleDatesSection(state, viewModel)
                }
            }

            item {
                Text(stringResource(R.string.rule_editor_times), style = MaterialTheme.typography.labelLarge)
                LazyRow {
                    items(state.times) { time ->
                        AssistChip(
                            onClick = { viewModel.removeTime(time) },
                            label = { Text("$time  ×") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                OutlinedButton(onClick = {
                    val now = LocalTime.now()
                    TimePickerDialog(context, { _, hour, minute ->
                        viewModel.addTime(LocalTime.of(hour, minute))
                    }, now.hour, now.minute, true).show()
                }) { Text(stringResource(R.string.rule_editor_add_time)) }
            }

            item {
                if (state.scheduleType == ScheduleType.WEEKLY) {
                    Text(stringResource(R.string.rule_editor_repeat), style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            FilterChip(
                                selected = day in state.days,
                                onClick = { viewModel.toggleDay(day) },
                                label = { Text(dayLabels[day]?.let { stringResource(it) } ?: day.name.take(2)) }
                            )
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(onClick = { viewModel.applyPreset(DayOfWeekPresets.EVERY_DAY) }) {
                            Text(stringResource(R.string.rule_editor_preset_every_day))
                        }
                        TextButton(onClick = { viewModel.applyPreset(DayOfWeekPresets.WEEKDAYS) }) {
                            Text(stringResource(R.string.rule_editor_preset_weekdays))
                        }
                        TextButton(onClick = { viewModel.applyPreset(DayOfWeekPresets.WEEKENDS) }) {
                            Text(stringResource(R.string.rule_editor_preset_weekends))
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.rule_editor_delay) + ": ${state.allowedDelayMinutes}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = state.allowedDelayMinutes.toFloat(),
                    onValueChange = { viewModel.updateAllowedDelay(it.toInt()) },
                    valueRange = 0f..120f
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.rule_editor_active), style = MaterialTheme.typography.titleMedium)
                    Switch(checked = state.enabled, onCheckedChange = viewModel::updateEnabled)
                }
            }

            item {
                OutlinedButton(
                    onClick = viewModel::runDryRun,
                    enabled = !state.actionInProgress && state.chatName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.rule_editor_dry_run)) }
            }
            item {
                OutlinedButton(
                    onClick = viewModel::requestTestSendPreview,
                    enabled = !state.actionInProgress && state.chatName.isNotBlank() && state.message.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.rule_editor_test_send)) }
            }

            state.lastActionResult?.let { result ->
                item {
                    Text(
                        when (result) {
                            is AutomationResult.Success -> stringResource(R.string.dry_run_ready)
                            is AutomationResult.Failure -> stringResource(
                                context.resources.getIdentifier(result.errorCode.stringResKey, "string", context.packageName)
                                    .takeIf { it != 0 } ?: R.string.error_UNKNOWN_ERROR
                            )
                        }
                    )
                }
            }

            if (!state.isNew) {
                item {
                    OutlinedButton(onClick = viewModel::requestDelete, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.rule_editor_delete))
                    }
                }
            }
        }
    }

    if (state.testSendPreviewVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissTestSendPreview,
            title = { Text(stringResource(R.string.test_send_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.test_send_confirm_group) + ": ${state.chatName}")
                    Text(stringResource(R.string.test_send_confirm_message) + ": \"${state.message}\"")
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmTestSend) { Text(stringResource(R.string.action_send_test)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissTestSendPreview) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text(stringResource(R.string.rule_delete_title)) },
            text = { Text(stringResource(R.string.rule_delete_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirm) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (state.showUnsavedChangesConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissUnsavedChangesConfirm,
            title = { Text(stringResource(R.string.unsaved_changes_title)) },
            text = { Text(stringResource(R.string.unsaved_changes_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::discardUnsavedChanges) { Text(stringResource(R.string.action_discard)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = viewModel::dismissUnsavedChangesConfirm) { Text(stringResource(R.string.action_cancel)) }
                    TextButton(onClick = viewModel::save) { Text(stringResource(R.string.rule_editor_save)) }
                }
            }
        )
    }
}

private val RuleEditorValidationError.stringRes: Int
    get() = when (this) {
        RuleEditorValidationError.CHAT_REQUIRED -> R.string.validation_chat_required
        RuleEditorValidationError.MESSAGE_REQUIRED -> R.string.validation_message_required
        RuleEditorValidationError.TIME_REQUIRED -> R.string.validation_time_required
        RuleEditorValidationError.DAY_REQUIRED -> R.string.validation_day_required
        RuleEditorValidationError.DATE_REQUIRED -> R.string.validation_date_required
        RuleEditorValidationError.SAVE_FAILED -> R.string.validation_save_failed
    }

private val ScheduleType.labelRes: Int
    get() = when (this) {
        ScheduleType.WEEKLY -> R.string.schedule_type_weekly
        ScheduleType.SPECIFIC_DATE -> R.string.schedule_type_specific_date
        ScheduleType.MULTIPLE_DATES -> R.string.schedule_type_multiple_dates
    }

@Composable
private fun WeeklyDateSection(state: RuleEditorState, viewModel: RuleEditorViewModel) {
    val context = LocalContext.current
    Text(stringResource(R.string.rule_editor_start_date), style = MaterialTheme.typography.labelLarge)
    DateButton(
        date = state.startDate,
        onClick = {
            showDatePicker(context, state.startDate) { viewModel.updateStartDate(it) }
        }
    )
}

@Composable
private fun SpecificDateSection(state: RuleEditorState, viewModel: RuleEditorViewModel) {
    val context = LocalContext.current
    val date = state.dates.firstOrNull() ?: LocalDate.now()
    Text(stringResource(R.string.rule_editor_date), style = MaterialTheme.typography.labelLarge)
    DateButton(
        date = date,
        onClick = {
            showDatePicker(context, date) { selected ->
                state.dates.forEach(viewModel::removeDate)
                viewModel.addDate(selected)
            }
        }
    )
}

@Composable
private fun MultipleDatesSection(state: RuleEditorState, viewModel: RuleEditorViewModel) {
    val context = LocalContext.current
    val pickerBase = state.dates.lastOrNull() ?: LocalDate.now()
    Text(stringResource(R.string.rule_editor_dates), style = MaterialTheme.typography.labelLarge)
    LazyRow {
        items(state.dates) { date ->
            AssistChip(
                onClick = { viewModel.removeDate(date) },
                label = { Text("${date.format(startDateFormatter)}  ×") },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
    OutlinedButton(onClick = {
        showDatePicker(context, pickerBase) { viewModel.addDate(it) }
    }) {
        Text(stringResource(R.string.rule_editor_add_date))
    }
}

@Composable
private fun DateButton(date: LocalDate, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(date.format(startDateFormatter))
    }
}

private fun showDatePicker(
    context: android.content.Context,
    initial: LocalDate,
    onDate: (LocalDate) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> onDate(LocalDate.of(year, month + 1, dayOfMonth)) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth
    ).show()
}
