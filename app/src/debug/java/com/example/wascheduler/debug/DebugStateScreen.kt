package com.example.wascheduler.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wascheduler.core.accessibility.WhatsAppAccessibilityService
import com.example.wascheduler.domain.repository.ExecutionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Debug-build-only diagnostics. Deliberately never shows message content read
 * from WhatsApp — only our own automation/scheduler state (spec section 84).
 */
@HiltViewModel
class DebugStateViewModel @Inject constructor(
    executionRepository: ExecutionRepository
) : ViewModel() {
    val latestExecution: StateFlow<String> = executionRepository.observeLatest()
        .map { it?.let { e -> "${e.status} @ ${e.scheduledAt} (${e.errorCode ?: "-"})" } ?: "none" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "loading...")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugStateScreen() {
    val viewModel: DebugStateViewModel = hiltViewModel()
    val latest by viewModel.latestExecution.collectAsState()
    val serviceRunning = WhatsAppAccessibilityService.instance != null

    Scaffold(topBar = { TopAppBar(title = { Text("Debug state") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Accessibility service running: $serviceRunning")
            Text("Latest execution: $latest")
        }
    }
}
