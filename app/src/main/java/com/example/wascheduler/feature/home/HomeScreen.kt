package com.example.wascheduler.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.wascheduler.R
import com.example.wascheduler.domain.model.DayOfWeekPresets
import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.model.ScheduleType
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

private val summaryDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddSchedule: () -> Unit,
    onEditRule: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingDeleteRule by remember { mutableStateOf<Rule?>(null) }
    val deletedMessage = stringResource(R.string.rule_deleted_snackbar)

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.home_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SystemStatusCard(state) }
            item { GlobalSwitchCard(state.globalEnabled, viewModel::setGlobalEnabled) }
            item { NextMessageCard(state) }
            items(state.rules, key = { it.id }) { rule ->
                RuleSummaryCard(rule, onClick = { onEditRule(rule.id) }, onToggle = { enabled ->
                    viewModel.setRuleEnabled(rule.id, enabled)
                }, onEdit = { onEditRule(rule.id) }, onDuplicate = {
                    viewModel.duplicateRule(rule.id, onEditRule)
                }, onDelete = { pendingDeleteRule = rule })
            }
            item {
                Button(onClick = onAddSchedule, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(" " + stringResource(R.string.home_add_schedule))
                }
            }
            item { LastOperationCard(state) }
        }
    }

    pendingDeleteRule?.let { rule ->
        AlertDialog(
            onDismissRequest = { pendingDeleteRule = null },
            title = { Text(stringResource(R.string.rule_delete_title)) },
            text = { Text(stringResource(R.string.rule_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteRule = null
                        viewModel.deleteRule(rule.id)
                        scope.launch { snackbarHostState.showSnackbar(deletedMessage) }
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRule = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SystemStatusCard(state: HomeUiState) {
    val ok = state.permissionState?.allCriticalGranted == true && state.globalEnabled
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(0.dp))
            Text(
                text = if (ok) stringResource(R.string.home_system_ok) else stringResource(R.string.home_system_needs_setup),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun GlobalSwitchCard(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.home_global_switch), style = MaterialTheme.typography.titleMedium)
            Switch(checked = enabled, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun NextMessageCard(state: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.home_next_message), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            val next = state.nextOccurrence
            if (next == null) {
                Text(stringResource(R.string.home_no_scheduled))
            } else {
                Text("${next.zonedDateTime.toLocalDate()} ${next.ruleTime.localTime}", style = MaterialTheme.typography.headlineSmall)
                Text(state.scheduleZoneId.id, style = MaterialTheme.typography.bodySmall)
                Text(next.rule.chatName, style = MaterialTheme.typography.bodyMedium)
                Text("\"${next.rule.message}\"", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RuleSummaryCard(
    rule: Rule,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(rule.name.ifBlank { rule.chatName }, style = MaterialTheme.typography.titleMedium)
                Text(rule.chatName, style = MaterialTheme.typography.bodySmall)
                Text("\"${rule.message}\"", style = MaterialTheme.typography.bodySmall)
                Text(scheduleSummary(rule), style = MaterialTheme.typography.bodySmall)
                val times = timeSummary(rule)
                Text(times, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_more)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_edit)) },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(if (rule.enabled) R.string.action_disable else R.string.action_enable)) },
                        onClick = {
                            menuExpanded = false
                            onToggle(!rule.enabled)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_duplicate)) },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
            }
        }
    }
}

@Composable
private fun daySummary(days: Set<DayOfWeek>): String = when (days) {
    DayOfWeekPresets.EVERY_DAY -> stringResource(R.string.rule_days_every_day)
    DayOfWeekPresets.WEEKDAYS -> stringResource(R.string.rule_days_weekdays)
    DayOfWeekPresets.WEEKENDS -> stringResource(R.string.rule_days_weekends)
    else -> DayOfWeek.entries.filter { it in days }.map { day -> stringResource(day.shortLabelRes) }.joinToString(" ")
}

@Composable
private fun scheduleSummary(rule: Rule): String =
    when (rule.scheduleType) {
        ScheduleType.WEEKLY -> {
            val days = rule.times.firstOrNull()?.days.orEmpty()
            "${stringResource(R.string.schedule_type_weekly)} · ${daySummary(days)} · ${stringResource(R.string.rule_start_date)} ${rule.startDate.format(summaryDateFormatter)}"
        }
        ScheduleType.SPECIFIC_DATE -> {
            val date = rule.dates.firstOrNull()?.format(summaryDateFormatter) ?: stringResource(R.string.status_unknown)
            "${stringResource(R.string.schedule_type_specific_date)} · $date"
        }
        ScheduleType.MULTIPLE_DATES -> {
            val dates = rule.dates.joinToString(" ") { it.format(summaryDateFormatter) }
            "${stringResource(R.string.schedule_type_multiple_dates)} · $dates"
        }
    }

private fun timeSummary(rule: Rule): String =
    if (rule.scheduleType == ScheduleType.MULTIPLE_DATES) {
        rule.times.joinToString("  ") { time ->
            val date = time.localDate?.format(summaryDateFormatter) ?: "?"
            "$date · ${time.localTime}"
        }
    } else {
        rule.times.joinToString("  ") { it.localTime.toString() }
    }

private val DayOfWeek.shortLabelRes: Int
    get() = when (this) {
        DayOfWeek.MONDAY -> R.string.day_monday_short
        DayOfWeek.TUESDAY -> R.string.day_tuesday_short
        DayOfWeek.WEDNESDAY -> R.string.day_wednesday_short
        DayOfWeek.THURSDAY -> R.string.day_thursday_short
        DayOfWeek.FRIDAY -> R.string.day_friday_short
        DayOfWeek.SATURDAY -> R.string.day_saturday_short
        DayOfWeek.SUNDAY -> R.string.day_sunday_short
    }

@Composable
private fun LastOperationCard(state: HomeUiState) {
    val last = state.lastOperation ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.home_last_operation), style = MaterialTheme.typography.labelLarge)
            Text("${last.scheduledAt.toLocalTime()} · ${last.targetChat}")
            Text("\"${last.messagePreview}\"")
            Text(last.status.name)
        }
    }
}
