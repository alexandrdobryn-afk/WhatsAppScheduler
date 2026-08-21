package com.example.wascheduler.feature.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    executionRepository: ExecutionRepository
) : ViewModel() {
    val executions: StateFlow<List<Execution>> = executionRepository.observeRecent(200)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val executions by viewModel.executions.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(executions, key = { it.id }) { execution ->
                ExecutionRow(execution)
            }
        }
    }
}

@Composable
private fun ExecutionRow(execution: Execution) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${execution.scheduledAt.toLocalDate()}  ${execution.scheduledAt.toLocalTime()}", style = MaterialTheme.typography.labelLarge)
            Text(execution.targetChat, style = MaterialTheme.typography.bodyMedium)
            Text("\"${execution.messagePreview}\"", style = MaterialTheme.typography.bodySmall)
            val statusText = statusSymbol(execution.status) + " " + statusLabel(execution.status)
            Text(statusText, style = MaterialTheme.typography.bodyMedium)
            if (execution.status == ExecutionStatus.FAILED && execution.errorCode != null) {
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
