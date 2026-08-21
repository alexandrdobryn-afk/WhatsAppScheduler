package com.example.wascheduler.feature.diagnostics

import android.content.Context
import android.os.Build
import com.example.wascheduler.BuildConfig
import com.example.wascheduler.core.permissions.PermissionState
import com.example.wascheduler.core.permissions.WhatsAppPackages
import com.example.wascheduler.domain.model.Execution
import com.example.wascheduler.domain.model.Rule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes a plain-text diagnostic report to app-private external files storage.
 * Per spec section 62/61: the ONLY message text ever written anywhere is the
 * text the user themselves configured in a rule (as a short preview) — nothing
 * read from WhatsApp's own UI is ever persisted or exported.
 */
@Singleton
class DiagnosticReportExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun export(permissionState: PermissionState, rules: List<Rule>, recentExecutions: List<Execution>): String {
        val whatsAppVersion = WhatsAppPackages.ALL.firstNotNullOfOrNull { pkg ->
            runCatching { context.packageManager.getPackageInfo(pkg, 0).versionName }.getOrNull()
        } ?: "n/a"

        val sb = StringBuilder()
        sb.appendLine("WhatsApp Sender — diagnostic report")
        sb.appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        sb.appendLine("Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("WhatsApp version: $whatsAppVersion")
        sb.appendLine()
        sb.appendLine("Permissions:")
        sb.appendLine("  WhatsApp installed: ${permissionState.whatsAppInstalled}")
        sb.appendLine("  Accessibility enabled: ${permissionState.accessibilityEnabled}")
        sb.appendLine("  Notifications enabled: ${permissionState.notificationsEnabled}")
        sb.appendLine("  Exact alarm allowed: ${permissionState.exactAlarmAllowed}")
        sb.appendLine("  Battery unrestricted: ${permissionState.batteryUnrestricted}")
        sb.appendLine()
        sb.appendLine("Rules count: ${rules.size}")
        sb.appendLine()
        sb.appendLine("Recent executions:")
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        recentExecutions.take(50).forEach { e ->
            sb.appendLine("  ${e.scheduledAt.format(fmt)}  ${e.status}  ${e.errorCode ?: ""}")
        }

        val dir = File(context.getExternalFilesDir(null), "diagnostics").apply { mkdirs() }
        val file = File(dir, "diagnostic_report_${System.currentTimeMillis()}.txt")
        file.writeText(sb.toString())
        return file.absolutePath
    }
}
