package com.example.wascheduler.feature.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wascheduler.R
import com.example.wascheduler.core.permissions.PermissionChecker
import com.example.wascheduler.core.permissions.PermissionState
import com.example.wascheduler.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow<PermissionState?>(null)
    val state: StateFlow<PermissionState?> = _state.asStateFlow()

    fun refresh() {
        _state.value = permissionChecker.currentState()
    }

    suspend fun wasCompletedBefore(): Boolean = settingsRepository.onboardingCompleted.first()

    fun markCompleted() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(true) }
    }
}

/**
 * Shown at first launch (and whenever a critical permission is missing) so the
 * user always sees the real status of each step — never a false "all set"
 * (spec sections 45-46).
 */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onContinue: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }
    DisposableEffectOnResume(lifecycleOwner) { viewModel.refresh() }

    val criticalOk = state?.let { it.whatsAppInstalled && it.accessibilityEnabled && it.exactAlarmAllowed } == true

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = { viewModel.markCompleted(); onContinue() },
                    enabled = criticalOk,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.onboarding_continue))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OnboardingStep(
                    title = stringResource(R.string.onboarding_step_accessibility_title),
                    description = stringResource(R.string.onboarding_accessibility_description),
                    granted = state?.accessibilityEnabled,
                    required = true,
                    actionLabel = stringResource(R.string.onboarding_open_settings),
                    onAction = { showAccessibilityDisclosure = true }
                )
            }
            item {
                OnboardingStep(
                    title = stringResource(R.string.onboarding_step_notifications_title),
                    description = stringResource(R.string.onboarding_notifications_description),
                    granted = state?.notificationsEnabled,
                    required = false,
                    actionLabel = stringResource(R.string.onboarding_allow),
                    onAction = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        context.startActivity(intent)
                    }
                )
            }
            item {
                OnboardingStep(
                    title = stringResource(R.string.onboarding_step_exact_alarm_title),
                    description = stringResource(R.string.onboarding_exact_alarm_description),
                    granted = state?.exactAlarmAllowed,
                    required = true,
                    actionLabel = stringResource(R.string.onboarding_allow),
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                )
            }
            item {
                OnboardingStep(
                    title = stringResource(R.string.onboarding_step_battery_title),
                    description = stringResource(R.string.onboarding_battery_description),
                    granted = state?.batteryUnrestricted,
                    required = false,
                    actionLabel = stringResource(R.string.onboarding_check),
                    onAction = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }
    }

    if (showAccessibilityDisclosure) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDisclosure = false },
            title = { Text(stringResource(R.string.accessibility_disclosure_title)) },
            text = { Text(stringResource(R.string.accessibility_disclosure_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityDisclosure = false
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                ) {
                    Text(stringResource(R.string.accessibility_disclosure_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDisclosure = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun OnboardingStep(
    title: String,
    description: String,
    granted: Boolean?,
    required: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    val isGranted = granted == true
    val statusText = when {
        isGranted -> stringResource(R.string.onboarding_status_allowed)
        required -> stringResource(R.string.onboarding_status_not_enabled)
        else -> stringResource(R.string.onboarding_status_recommended)
    }
    val statusColor = when {
        isGranted -> MaterialTheme.colorScheme.primary
        required -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(description, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(statusText, color = statusColor, style = MaterialTheme.typography.labelLarge)
                }
            }
            if (isGranted) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_action_done),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.End)
                )
            } else {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun DisposableEffectOnResume(lifecycleOwner: androidx.lifecycle.LifecycleOwner, onResume: () -> Unit) {
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
