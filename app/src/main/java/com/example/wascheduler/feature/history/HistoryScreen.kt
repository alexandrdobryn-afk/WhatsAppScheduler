package com.example.wascheduler.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wascheduler.R
import com.example.wascheduler.domain.model.Execution
import com.example.wascheduler.domain.model.ExecutionStatus
import com.example.wascheduler.domain.repository.ExecutionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class HistoryFilter {
    ALL,
    SENT,
    FAILED,
    SKIPPED
}

data class HistoryUiState(
    val filter: HistoryFilter = HistoryFilter.ALL,
    val executions: List<Execution> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    executionRepository: ExecutionRepository
) : ViewModel() {
    private val filter = MutableStateFlow(HistoryFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        executionRepository.observeRecent(200),
        filter.asStateFlow()
    ) { executions, selectedFilter ->
        HistoryUiState(
            filter = selectedFilter,
            executions = executions.filter { execution ->
                when (selectedFilter) {
                    HistoryFilter.ALL -> true
                    HistoryFilter.SENT -> execution.status == ExecutionStatus.SENT
                    HistoryFilter.FAILED -> execution.status == ExecutionStatus.FAILED
                    HistoryFilter.SKIPPED -> execution.status == ExecutionStatus.SKIPPED
                }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun setFilter(value: HistoryFilter) {
        filter.value = value
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var selectedExecution by remember { mutableStateOf<Execution?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HistoryFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(stringResource(filter.labelRes)) }
                        )
                    }
                }
            }
            items(state.executions, key = { it.id }) { execution ->
                ExecutionRow(execution, onClick = { selectedExecution = execution })
            }
        }
    }

    selectedExecution?.let { execution ->
        ExecutionDetailsDialog(execution = execution, onDismiss = { selectedExecution = null })
    }
}

@Composable
private fun ExecutionRow(execution: Execution, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${execution.scheduledAt.toLocalDate()}  ${execution.scheduledAt.toLocalTime()}", style = MaterialTheme.typography.labelLarge)
            Text(execution.targetChat, style = MaterialTheme.typography.bodyMedium)
            val statusText = statusSymbol(execution.status) + " " + statusLabel(execution.status)
            Text(statusText, style = MaterialTheme.typography.bodyMedium)
            Text("${stringResource(R.string.history_attempt)} ${execution.attemptNumber}", style = MaterialTheme.typography.bodySmall)
            if (execution.errorCode != null) {
                Text(execution.errorCode.name, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun statusLabel(status: ExecutionStatus): String = stringResource(
    when (status) {
        ExecutionStatus.SCHEDULED -> R.string.status_scheduled
        ExecutionStatus.RUNNING -> R.string.status_running
        ExecutionStatus.SENT -> R.string.status_sent
        ExecutionStatus.FAILED -> R.string.status_failed
        ExecutionStatus.SKIPPED -> R.string.status_skipped
    }
)

private fun statusSymbol(status: ExecutionStatus): String = when (status) {
    ExecutionStatus.SENT -> "✓"
    ExecutionStatus.FAILED -> "✕"
    ExecutionStatus.SKIPPED -> "–"
    ExecutionStatus.RUNNING -> "…"
    ExecutionStatus.SCHEDULED -> "○"
}

@Composable
private fun ExecutionDetailsDialog(execution: Execution, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_details_title)) },
        text = {
            Column {
                DetailRow(stringResource(R.string.history_scheduled), execution.scheduledAt.toString())
                DetailRow(stringResource(R.string.history_started), execution.startedAt?.toString() ?: "—")
                DetailRow(stringResource(R.string.history_finished), execution.finishedAt?.toString() ?: "—")
                DetailRow(stringResource(R.string.history_target), execution.targetChat)
                DetailRow(stringResource(R.string.history_status), statusLabel(execution.status))
                DetailRow(stringResource(R.string.history_attempt), execution.attemptNumber.toString())
                execution.errorCode?.let { DetailRow(stringResource(R.string.history_error), it.name) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.widthIn(max = 180.dp))
    }
}

private val HistoryFilter.labelRes: Int
    get() = when (this) {
        HistoryFilter.ALL -> R.string.history_filter_all
        HistoryFilter.SENT -> R.string.history_filter_sent
        HistoryFilter.FAILED -> R.string.history_filter_failed
        HistoryFilter.SKIPPED -> R.string.history_filter_skipped
    }
