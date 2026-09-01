package com.example.wascheduler.core.permissions

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Package names of officially supported WhatsApp builds (spec section 24). */
object WhatsAppPackages {
    const val CONSUMER = "com.whatsapp"
    val ALL = listOf(CONSUMER)
}

data class PermissionState(
    val whatsAppInstalled: Boolean,
    val accessibilityEnabled: Boolean,
    val notificationsEnabled: Boolean,
    val exactAlarmAllowed: Boolean,
    val batteryUnrestricted: Boolean
) {
    val allCriticalGranted: Boolean
        get() = whatsAppInstalled && accessibilityEnabled && exactAlarmAllowed
}

/**
 * Reports permission state honestly (spec section 46: never show "all good" when
 * something is actually missing). Every check queries the real system state at
 * call time — nothing here is cached or assumed.
 */
@Singleton
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun currentState(): PermissionState = PermissionState(
        whatsAppInstalled = isWhatsAppInstalled(),
        accessibilityEnabled = isAccessibilityServiceEnabled(),
        notificationsEnabled = areNotificationsEnabled(),
        exactAlarmAllowed = canScheduleExactAlarms(),
        batteryUnrestricted = isIgnoringBatteryOptimizations()
    )

    fun isWhatsAppInstalled(): Boolean =
        WhatsAppPackages.ALL.any { pkg ->
            runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
        }

    fun installedWhatsAppPackage(): String? =
        WhatsAppPackages.ALL.firstOrNull { pkg ->
            runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
        }

    fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = "${context.packageName}/com.example.wascheduler.core.accessibility.WhatsAppAccessibilityService"
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (enabledServicesSetting != null) {
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledServicesSetting)
            while (splitter.hasNext()) {
                if (splitter.next().equals(expectedComponent, ignoreCase = true)) return true
            }
        }
        // Fall back to the system AccessibilityManager view, which is more
        // reliable on some OEM skins.
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledList = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledList?.any { it.resolveInfo.serviceInfo.packageName == context.packageName } ?: false
    }

    fun areNotificationsEnabled(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.areNotificationsEnabled()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
