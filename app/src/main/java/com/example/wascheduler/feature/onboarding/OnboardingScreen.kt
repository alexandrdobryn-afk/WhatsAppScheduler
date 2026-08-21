package com.example.wascheduler.feature.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    LaunchedEffect(Unit) { viewModel.refresh() }
    DisposableEffectOnResume(lifecycleOwner) { viewModel.refresh() }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OnboardingStep(
                title = stringResource(R.string.onboarding_step_accessibility_title),
                granted = state?.accessibilityEnabled,
                actionLabel = stringResource(R.string.onboarding_open_settings),
                onAction = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
            OnboardingStep(
                title = stringResource(R.string.onboarding_step_notifications_title),
                granted = state?.notificationsEnabled,
                actionLabel = stringResource(R.string.onboarding_allow),
                onAction = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    context.startActivity(intent)
                }
            )
            OnboardingStep(
                title = stringResource(R.string.onboarding_step_exact_alarm_title),
                granted = state?.exactAlarmAllowed,
                actionLabel = stringResource(R.string.onboarding_allow),
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                }
            )
            OnboardingStep(
                title = stringResource(R.string.onboarding_step_battery_title),
                granted = state?.batteryUnrestricted,
                actionLabel = stringResource(R.string.onboarding_check),
                onAction = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                }
            )

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
            val criticalOk = state?.let { it.whatsAppInstalled && it.accessibilityEnabled && it.exactAlarmAllowed } == true
            Button(
                onClick = { viewModel.markCompleted(); onContinue() },
                enabled = criticalOk,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_continue))
            }
        }
    }
}

@Composable
private fun OnboardingStep(title: String, granted: Boolean?, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (granted == true) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (granted == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(title, modifier = Modifier.padding(start = 8.dp))
        }
        if (granted != true) {
            Button(onClick = onAction) { Text(actionLabel) }
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
