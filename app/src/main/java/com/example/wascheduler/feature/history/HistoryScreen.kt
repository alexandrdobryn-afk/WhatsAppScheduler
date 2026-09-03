package com.example.wascheduler.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon(execution.status),
                contentDescription = null,
                tint = statusColor(execution.status),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${execution.scheduledAt.toLocalDate()} · ${execution.scheduledAt.toLocalTime()}",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    execution.targetChat,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                execution.errorCode?.let {
                    Text(
                        it.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AssistChip(
                onClick = onClick,
                label = {
                    Text(
                        "${statusLabel(execution.status)} · ${stringResource(R.string.history_attempt)} ${execution.attemptNumber}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

private fun statusIcon(status: ExecutionStatus): ImageVector = when (status) {
    ExecutionStatus.SENT -> Icons.Filled.CheckCircle
    ExecutionStatus.FAILED -> Icons.Filled.Error
    ExecutionStatus.SKIPPED -> Icons.Filled.Warning
    ExecutionStatus.RUNNING -> Icons.Filled.Schedule
    ExecutionStatus.SCHEDULED -> Icons.Filled.Schedule
}

@Composable
private fun statusColor(status: ExecutionStatus): Color = when (status) {
    ExecutionStatus.SENT -> MaterialTheme.colorScheme.primary
    ExecutionStatus.FAILED -> MaterialTheme.colorScheme.error
    ExecutionStatus.SKIPPED -> MaterialTheme.colorScheme.tertiary
    ExecutionStatus.RUNNING -> MaterialTheme.colorScheme.secondary
    ExecutionStatus.SCHEDULED -> MaterialTheme.colorScheme.onSurfaceVariant
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
