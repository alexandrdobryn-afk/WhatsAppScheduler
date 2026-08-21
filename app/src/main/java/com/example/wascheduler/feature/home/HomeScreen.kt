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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.wascheduler.R
import com.example.wascheduler.domain.model.DayOfWeekPresets
import com.example.wascheduler.domain.model.Rule
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

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.home_title)) }) }
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
                })
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
private fun RuleSummaryCard(rule: Rule, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
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
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(rule.name.ifBlank { rule.chatName }, style = MaterialTheme.typography.titleMedium)
                Text(rule.chatName, style = MaterialTheme.typography.bodySmall)
                Text("\"${rule.message}\"", style = MaterialTheme.typography.bodySmall)
                Text(
                    "${stringResource(R.string.rule_start_date)}: ${rule.startDate.format(summaryDateFormatter)}",
                    style = MaterialTheme.typography.bodySmall
                )
                val times = rule.times.joinToString("  ") { it.localTime.toString() }
                Text(times, style = MaterialTheme.typography.bodySmall)
                Text(daySummary(rule.times.firstOrNull()?.days.orEmpty()), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = rule.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun daySummary(days: Set<DayOfWeek>): String = when (days) {
    DayOfWeekPresets.EVERY_DAY -> stringResource(R.string.rule_days_every_day)
    DayOfWeekPresets.WEEKDAYS -> stringResource(R.string.rule_days_weekdays)
    else -> DayOfWeek.entries.filter { it in days }.map { day -> stringResource(day.shortLabelRes) }.joinToString(" ")
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
