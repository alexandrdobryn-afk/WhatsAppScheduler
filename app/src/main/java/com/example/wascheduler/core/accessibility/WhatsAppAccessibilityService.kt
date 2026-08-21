package com.example.wascheduler.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Process
import android.view.accessibility.AccessibilityEvent
import com.example.wascheduler.core.logging.LogComponent
import com.example.wascheduler.core.logging.Logger
import com.example.wascheduler.core.permissions.WhatsAppPackages
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

enum class AccessibilityConnectionStatus {
    DISABLED,
    ENABLED_NOT_CONNECTED,
    CONNECTED,
    INTERRUPTED,
    DESTROYED
}

data class AccessibilityConnectionSnapshot(
    val permissionEnabled: Boolean,
    val status: AccessibilityConnectionStatus,
    val serviceConnected: Boolean,
    val processPid: Int,
    val lastEventTimestamp: Long
)

/**
 * Accessibility service scoped, both in [android.R.xml] config and in code, to
 * WhatsApp's own package names only (spec sections 24-25). This service never
 * inspects, logs, or reacts to events from any other application — the
 * packageNames allow-list in accessibility_service_config.xml enforces this at
 * the OS level, and [onAccessibilityEvent] re-checks it defensively.
 *
 * This service does not run automation on every event — it exposes a single
 * suspend entry point, [runAutomation], that AutomationEngine calls when (and
 * only when) a scheduled occurrence needs to be executed. Between executions,
 * the service is otherwise idle.
 */
@AndroidEntryPoint
class WhatsAppAccessibilityService : AccessibilityService() {

    @Inject lateinit var adapter: WhatsAppUiAdapter

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pendingCompletion: CompletableDeferred<Unit>? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Logger.i(LogComponent.ACCESSIBILITY, "onServiceConnected pid=${Process.myPid()} timestamp=${System.currentTimeMillis()}")
        instance = this
        updateStatus(AccessibilityConnectionStatus.CONNECTED)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName !in WhatsAppPackages.ALL) return
        // Intentionally no-op beyond the guard above: this service is driven by
        // the polling AutomationStateMachine rather than by reacting to
        // individual events, which keeps event handling simple and avoids a
        // single sprawling event handler (spec section 65).
    }

    override fun onInterrupt() {
        updateStatus(AccessibilityConnectionStatus.INTERRUPTED)
        Logger.w(LogComponent.ACCESSIBILITY, "onInterrupt pid=${Process.myPid()} timestamp=${System.currentTimeMillis()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
        updateStatus(AccessibilityConnectionStatus.DESTROYED)
        Logger.w(LogComponent.ACCESSIBILITY, "onDestroy pid=${Process.myPid()} timestamp=${System.currentTimeMillis()}")
    }

    /** Runs one automation attempt for [task] against [whatsAppPackage] and returns its result. */
    suspend fun runAutomation(task: AutomationTask, whatsAppPackage: String): AutomationResult {
        val machine = AutomationStateMachine(this, adapter, whatsAppPackage)
        return machine.run(task)
    }

    suspend fun runDryRun(chatName: String, whatsAppPackage: String): AutomationResult {
        val machine = AutomationStateMachine(this, adapter, whatsAppPackage)
        return machine.runDryRun(chatName)
    }

    suspend fun runCompatibilityProbe(whatsAppPackage: String): AutomationResult {
        val machine = AutomationStateMachine(this, adapter, whatsAppPackage)
        return machine.runCompatibilityProbe()
    }

    companion object {
        /**
         * The running service instance, if the user has enabled it. AutomationEngine
         * checks this (rather than assuming) before attempting anything, and reports
         * ACCESSIBILITY_DISABLED when it is null — never simulating success.
         */
        @Volatile
        var instance: WhatsAppAccessibilityService? = null
            private set

        @Volatile
        private var lastStatus: AccessibilityConnectionStatus = AccessibilityConnectionStatus.ENABLED_NOT_CONNECTED

        @Volatile
        private var lastEventTimestamp: Long = 0L

        fun snapshot(permissionEnabled: Boolean): AccessibilityConnectionSnapshot {
            val connected = instance != null
            val status = when {
                !permissionEnabled -> AccessibilityConnectionStatus.DISABLED
                connected && lastStatus != AccessibilityConnectionStatus.INTERRUPTED -> AccessibilityConnectionStatus.CONNECTED
                connected -> lastStatus
                lastStatus == AccessibilityConnectionStatus.DESTROYED -> AccessibilityConnectionStatus.DESTROYED
                else -> AccessibilityConnectionStatus.ENABLED_NOT_CONNECTED
            }
            return AccessibilityConnectionSnapshot(
                permissionEnabled = permissionEnabled,
                status = status,
                serviceConnected = connected,
                processPid = Process.myPid(),
                lastEventTimestamp = lastEventTimestamp
            )
        }

        suspend fun awaitConnected(
            isPermissionEnabled: () -> Boolean,
            timeoutMs: Long = 10_000L
        ): WhatsAppAccessibilityService? {
            if (!isPermissionEnabled()) {
                updateStatus(AccessibilityConnectionStatus.DISABLED)
                return null
            }
            instance?.let { return it }
            updateStatus(AccessibilityConnectionStatus.ENABLED_NOT_CONNECTED)
            return withTimeoutOrNull(timeoutMs) {
                while (isPermissionEnabled()) {
                    instance?.let { return@withTimeoutOrNull it }
                    delay(200L)
                }
                null
            }
        }

        private fun updateStatus(status: AccessibilityConnectionStatus) {
            lastStatus = status
            lastEventTimestamp = System.currentTimeMillis()
        }
    }
}
