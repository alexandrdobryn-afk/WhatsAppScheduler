package com.example.wascheduler.feature.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wascheduler.BuildConfig
import com.example.wascheduler.R
import com.example.wascheduler.core.intents.ExternalIntents
import com.example.wascheduler.core.intents.ExternalLinks
import com.example.wascheduler.core.intents.startActivityIfAvailable
import com.example.wascheduler.core.locale.AppLocaleController
import com.example.wascheduler.core.permissions.PermissionChecker
import com.example.wascheduler.core.permissions.PermissionState
import com.example.wascheduler.core.scheduler.AlarmScheduler
import com.example.wascheduler.data.repository.AppLanguage
import com.example.wascheduler.data.repository.AppTheme
import com.example.wascheduler.data.repository.ScheduleTimeZoneMode
import com.example.wascheduler.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val permissionChecker: PermissionChecker
) : ViewModel() {
    val theme: Flow<AppTheme> = settingsRepository.theme
    val language: Flow<AppLanguage> = settingsRepository.language
    val notifyOnSuccess: Flow<Boolean> = settingsRepository.notifyOnSuccess
    val globalAutomationEnabled: Flow<Boolean> = settingsRepository.globalAutomationEnabled
    val maxRetryAttempts: Flow<Int> = settingsRepository.maxRetryAttempts
    val timeZoneMode: Flow<ScheduleTimeZoneMode> = settingsRepository.scheduleTimeZoneMode
    val customTimeZoneId: Flow<String> = settingsRepository.customTimeZoneId
    val scheduleZoneId: Flow<ZoneId> = settingsRepository.scheduleZoneId

    private val _permissionState = MutableStateFlow<PermissionState?>(null)
    val permissionState: StateFlow<PermissionState?> = _permissionState.asStateFlow()

    init {
        refreshPermissions()
        viewModelScope.launch {
            AppLocaleController.apply(context, settingsRepository.language.first())
        }
    }

    fun refreshPermissions() {
        _permissionState.value = permissionChecker.currentState()
    }

    fun setTheme(theme: AppTheme) = viewModelScope.launch { settingsRepository.setTheme(theme) }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        AppLocaleController.apply(context, language)
        settingsRepository.setLanguage(language)
    }

    fun setNotifyOnSuccess(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setNotifyOnSuccess(enabled)
    }

    fun setGlobalAutomationEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setGlobalAutomationEnabled(enabled)
        if (enabled) alarmScheduler.rescheduleNext() else alarmScheduler.cancel()
    }

    fun setRetryAttempts(count: Int) = viewModelScope.launch {
        settingsRepository.setMaxRetryAttempts(count)
    }

    fun setTimeZoneMode(mode: ScheduleTimeZoneMode) = viewModelScope.launch {
        settingsRepository.setScheduleTimeZoneMode(mode)
        alarmScheduler.rescheduleNext()
    }

    fun setCustomTimeZone(zoneId: String) = viewModelScope.launch {
        settingsRepository.setCustomTimeZoneId(zoneId)
        alarmScheduler.rescheduleNext()
    }

    fun rescheduleNow() = viewModelScope.launch {
        settingsRepository.scheduleZoneId.first()
        alarmScheduler.rescheduleNext()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onOpenDiagnostics: () -> Unit) {
    val context = LocalContext.current
    val theme by viewModel.theme.collectAsState(initial = AppTheme.SYSTEM)
    val language by viewModel.language.collectAsState(initial = AppLanguage.SYSTEM)
    val globalAutomationEnabled by viewModel.globalAutomationEnabled.collectAsState(initial = true)
    val notifyOnSuccess by viewModel.notifyOnSuccess.collectAsState(initial = true)
    val maxRetryAttempts by viewModel.maxRetryAttempts.collectAsState(initial = 3)
    val timeZoneMode by viewModel.timeZoneMode.collectAsState(initial = ScheduleTimeZoneMode.DEVICE)
    val scheduleZoneId by viewModel.scheduleZoneId.collectAsState(initial = ZoneId.systemDefault())
    val permissionState by viewModel.permissionState.collectAsState()
    var showTimeZonePicker by remember { mutableStateOf(false) }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                SettingsSection(title = stringResource(R.string.settings_general)) {
                    SubsectionLabel(stringResource(R.string.settings_theme))
                    ChoiceRow(AppTheme.SYSTEM, theme, R.string.settings_theme_system, viewModel::setTheme)
                    ChoiceRow(AppTheme.LIGHT, theme, R.string.settings_theme_light, viewModel::setTheme)
                    ChoiceRow(AppTheme.DARK, theme, R.string.settings_theme_dark, viewModel::setTheme)

                    SubsectionLabel(stringResource(R.string.settings_language), modifier = Modifier.padding(top = 8.dp))
                    ChoiceRow(AppLanguage.SYSTEM, language, R.string.settings_language_system) { selected ->
                        viewModel.setLanguage(selected)
                        context.findActivity()?.recreate()
                    }
                    ChoiceRow(AppLanguage.RU, language, R.string.settings_language_ru) { selected ->
                        viewModel.setLanguage(selected)
                        context.findActivity()?.recreate()
                    }
                    ChoiceRow(AppLanguage.UK, language, R.string.settings_language_uk) { selected ->
                        viewModel.setLanguage(selected)
                        context.findActivity()?.recreate()
                    }
                    ChoiceRow(AppLanguage.EN, language, R.string.settings_language_en) { selected ->
                        viewModel.setLanguage(selected)
                        context.findActivity()?.recreate()
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_schedule)) {
                    SubsectionLabel(stringResource(R.string.settings_schedule_timezone))
                    ChoiceRow(
                        ScheduleTimeZoneMode.DEVICE,
                        timeZoneMode,
                        R.string.settings_timezone_device,
                        viewModel::setTimeZoneMode
                    )
                    ChoiceRow(
                        ScheduleTimeZoneMode.CUSTOM,
                        timeZoneMode,
                        R.string.settings_timezone_custom
                    ) {
                        showTimeZonePicker = true
                    }
                    ActionRow(
                        icon = Icons.Filled.Public,
                        label = stringResource(R.string.settings_timezone_select),
                        value = "${scheduleZoneId.id} · ${TimeZonePickerModel.offsetLabel(scheduleZoneId.id)}",
                        onClick = {
                            showTimeZonePicker = true
                        }
                    )
                    SettingsValueRow(stringResource(R.string.settings_device_timezone), ZoneId.systemDefault().id)
                    SettingsValueRow(stringResource(R.string.settings_schedule_timezone_current), scheduleZoneId.id)
                    SettingsValueRow(
                        stringResource(R.string.settings_scheduler_current_time),
                        ZonedDateTime.now(scheduleZoneId).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_automation)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsLabel(stringResource(R.string.home_global_switch), modifier = Modifier.weight(1f))
                        Switch(checked = globalAutomationEnabled, onCheckedChange = viewModel::setGlobalAutomationEnabled)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsLabel(stringResource(R.string.settings_notifications_success), modifier = Modifier.weight(1f))
                        Switch(checked = notifyOnSuccess, onCheckedChange = viewModel::setNotifyOnSuccess)
                    }
                    SubsectionLabel(stringResource(R.string.settings_retry_attempts), modifier = Modifier.padding(top = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val retryChoices = listOf(
                            0 to R.string.retry_0,
                            1 to R.string.retry_1,
                            2 to R.string.retry_2,
                            3 to R.string.retry_3
                        )
                        retryChoices.forEach { choice: Pair<Int, Int> ->
                            val (count, labelRes) = choice
                            FilterChip(
                                selected = maxRetryAttempts == count,
                                onClick = { viewModel.setRetryAttempts(count) },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }
                    OutlinedButton(onClick = viewModel::rescheduleNow, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(R.string.settings_reschedule_now))
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_permissions)) {
                    PermissionRow(stringResource(R.string.diagnostics_whatsapp_installed), permissionState?.whatsAppInstalled)
                    PermissionRow(stringResource(R.string.diagnostics_accessibility), permissionState?.accessibilityEnabled)
                    PermissionRow(stringResource(R.string.diagnostics_notifications), permissionState?.notificationsEnabled)
                    PermissionRow(stringResource(R.string.diagnostics_exact_alarm), permissionState?.exactAlarmAllowed)
                    PermissionRow(stringResource(R.string.settings_background_usage), permissionState?.batteryUnrestricted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAccessibilityDisclosure = true }) {
                            Icon(Icons.Filled.Accessibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.onboarding_step_accessibility_title))
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) }) {
                                Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.onboarding_step_exact_alarm_title))
                            }
                        }
                        OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }) {
                            Icon(Icons.Filled.BatterySaver, contentDescription = null, modifier = Modifier.size(18.dp))
                            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.onboarding_step_battery_title))
                        }
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        setData(Uri.parse("package:${context.packageName}"))
                                    }
                                )
                            }
                        ) {
                            Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_app_permissions))
                        }
                    }
                    if (Build.MANUFACTURER.contains("xiaomi", ignoreCase = true)) {
                        Text(
                            stringResource(R.string.settings_xiaomi_background_guidance),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.diagnostics_title)) {
                    ActionRow(
                        icon = Icons.Filled.BugReport,
                        label = stringResource(R.string.settings_diagnostics),
                        value = stringResource(R.string.settings_export_report),
                        onClick = onOpenDiagnostics
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_about)) {
                    SettingsValueRow(stringResource(R.string.settings_about_app), stringResource(R.string.app_name))
                    SettingsValueRow(stringResource(R.string.settings_version), BuildConfig.VERSION_NAME)
                    SettingsValueRow(stringResource(R.string.settings_support), ExternalLinks.SUPPORT_EMAIL)
                    ActionRow(
                        icon = Icons.Filled.PrivacyTip,
                        label = stringResource(R.string.settings_privacy_policy),
                        value = ExternalLinks.PRIVACY_POLICY_URL,
                        onClick = {
                            if (!context.startActivityIfAvailable(ExternalIntents.privacyPolicy())) {
                                Toast.makeText(context, R.string.settings_open_link_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    ActionRow(
                        icon = Icons.Filled.SupportAgent,
                        label = stringResource(R.string.settings_contact_support),
                        value = ExternalLinks.SUPPORT_EMAIL,
                        onClick = {
                            if (!context.startActivityIfAvailable(ExternalIntents.supportEmail())) {
                                Toast.makeText(context, R.string.settings_open_email_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    ActionRow(
                        icon = Icons.Filled.Public,
                        label = stringResource(R.string.settings_website),
                        value = ExternalLinks.WEBSITE_URL,
                        onClick = {
                            if (!context.startActivityIfAvailable(ExternalIntents.website())) {
                                Toast.makeText(context, R.string.settings_open_link_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAccessibilityDisclosure) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDisclosure = false },
            title = { Text(stringResource(R.string.accessibility_disclosure_title)) },
            text = {
                Text(
                    text = stringResource(R.string.accessibility_disclosure_body),
                    modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityDisclosure = false
                        if (!context.startActivityIfAvailable(ExternalIntents.accessibilitySettings())) {
                            Toast.makeText(context, R.string.accessibility_settings_open_failed, Toast.LENGTH_SHORT).show()
                        }
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

    if (showTimeZonePicker) {
        TimeZonePickerDialog(
            selectedZoneId = scheduleZoneId.id,
            onDismiss = { showTimeZonePicker = false },
            onSelected = { zoneId ->
                showTimeZonePicker = false
                viewModel.setCustomTimeZone(zoneId)
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun TimeZonePickerDialog(
    selectedZoneId: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val options = remember(query) { TimeZonePickerModel.options(query) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_timezone_select_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.settings_timezone_search)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(options, key = { it.zoneId }) { option ->
                        TimeZoneOptionRow(
                            option = option,
                            selected = option.zoneId == selectedZoneId,
                            onClick = { onSelected(option.zoneId) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun TimeZoneOptionRow(option: TimeZonePickerOption, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (selected) "✓" else "",
            modifier = Modifier.width(24.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                option.zoneId,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                option.offsetLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SubsectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SettingsLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SettingsSubLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    if (text.isNotBlank()) {
        Text(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            SettingsLabel(label)
            SettingsSubLabel(value)
        }
    }
}

@Composable
private fun <T> ChoiceRow(value: T, current: T, labelRes: Int, onSelected: (T) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected(value) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = value == current, onClick = { onSelected(value) })
        SettingsLabel(stringResource(labelRes), modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun SettingsValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        SettingsLabel(label, modifier = Modifier.weight(1f).padding(end = 12.dp))
        SettingsSubLabel(value, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PermissionRow(label: String, ok: Boolean?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsLabel(label, modifier = Modifier.weight(1f).padding(end = 12.dp))
        StatusPill(ok)
    }
}

@Composable
private fun StatusPill(ok: Boolean?) {
    val text = when (ok) {
        true -> "✓ ${stringResource(R.string.status_ok)}"
        false -> stringResource(R.string.status_attention)
        null -> stringResource(R.string.status_unknown)
    }
    val container = when (ok) {
        true -> MaterialTheme.colorScheme.primaryContainer
        false -> MaterialTheme.colorScheme.errorContainer
        null -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (ok) {
        true -> MaterialTheme.colorScheme.onPrimaryContainer
        false -> MaterialTheme.colorScheme.onErrorContainer
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = MaterialTheme.shapes.small, color = container) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
