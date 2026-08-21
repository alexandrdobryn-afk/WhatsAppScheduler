package com.example.wascheduler.feature.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.wascheduler.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnostics_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth()) {
            val p = state.permissionState
            DiagRow(stringResource(R.string.diagnostics_whatsapp_installed), p?.whatsAppInstalled)
            DiagRow(stringResource(R.string.diagnostics_accessibility), p?.accessibilityEnabled)
            DiagRow(stringResource(R.string.diagnostics_notifications), p?.notificationsEnabled)
            DiagRow(stringResource(R.string.diagnostics_exact_alarm), p?.exactAlarmAllowed)
            DiagRow(stringResource(R.string.diagnostics_battery), p?.batteryUnrestricted?.not())
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.diagnostics_active_rules))
                Text("${state.activeRuleCount}")
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.diagnostics_next_execution))
                Text(state.nextExecutionLabel ?: "—")
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.diagnostics_schedule_timezone))
                Text(state.scheduleZoneId.id)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.diagnostics_device_timezone))
                Text(state.deviceZoneId.id)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.diagnostics_last_execution))
                Text(state.lastExecutionLabel ?: "—")
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.diagnostics_last_error))
                Text(state.lastErrorLabel ?: "—")
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
            OutlinedButton(onClick = viewModel::checkWhatsAppCompatibility, enabled = !state.checkingWhatsApp) {
                Text(stringResource(R.string.diagnostics_check_whatsapp))
            }
            state.whatsAppCheckResult?.let { key ->
                val resId = context.resources.getIdentifier(key, "string", context.packageName)
                Text(
                    text = if (resId != 0) stringResource(resId) else key,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
            Button(onClick = viewModel::exportReport) {
                Text(stringResource(R.string.settings_export_report))
            }
            state.exportedReportPath?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun DiagRow(label: String, ok: Boolean?) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(
            when (ok) {
                true -> "✓"
                false -> "!"
                null -> "…"
            }
        )
    }
}
