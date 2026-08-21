package com.example.wascheduler.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.wascheduler.core.logging.LogComponent
import com.example.wascheduler.core.logging.Logger
import com.example.wascheduler.core.permissions.WhatsAppPackages
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        Logger.i(LogComponent.ACCESSIBILITY, "WhatsAppAccessibilityService connected")
        instance = this
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
        Logger.w(LogComponent.ACCESSIBILITY, "Accessibility service interrupted by the system")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
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
        var instance: WhatsAppAccessibilityService? = null
            private set
    }
}
