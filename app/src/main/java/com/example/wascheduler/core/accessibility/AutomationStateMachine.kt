package com.example.wascheduler.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.example.wascheduler.core.logging.LogComponent
import com.example.wascheduler.core.logging.Logger
import com.example.wascheduler.domain.model.ErrorCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** Explicit states per spec section 65 — no single giant onAccessibilityEvent handler. */
enum class AutomationState {
    IDLE, LAUNCHING_WHATSAPP, FINDING_CHAT, OPENING_CHAT, VERIFYING_CHAT,
    FINDING_INPUT, SETTING_TEXT, FINDING_SEND_BUTTON, SENDING, VERIFYING, SUCCESS, FAILED
}

data class AutomationTask(val chatName: String, val message: String)

sealed class AutomationResult {
    data object Success : AutomationResult()
    data class Failure(val errorCode: ErrorCode, val detail: String? = null) : AutomationResult()
}

/** Per-step timeouts (spec section 64) — no operation waits forever. */
private object Timeouts {
    const val LAUNCH_MS = 10_000L
    const val FIND_CHAT_MS = 15_000L
    const val OPEN_CHAT_MS = 10_000L
    const val FIND_INPUT_MS = 10_000L
    const val SEND_MS = 10_000L
    const val POLL_INTERVAL_MS = 200L
}

/**
 * Drives one send attempt through the states in spec section 65. Reads the
 * WhatsApp node tree exclusively through [WhatsAppUiAdapter] — never touches
 * screen coordinates. Every transition to FAILED is paired with one of the
 * structured [ErrorCode]s; nothing here silently "reframes" an ambiguous state
 * into success.
 */
class AutomationStateMachine(
    private val service: AccessibilityService,
    private val adapter: WhatsAppUiAdapter,
    private val whatsAppPackage: String
) {
    var state: AutomationState = AutomationState.IDLE
        private set

    suspend fun run(task: AutomationTask): AutomationResult {
        state = AutomationState.LAUNCHING_WHATSAPP
        if (!launchWhatsApp()) {
            return fail(ErrorCode.WHATSAPP_LAUNCH_FAILED)
        }

        state = AutomationState.FINDING_CHAT
        val chatOpened = withTimeoutOrNull(Timeouts.FIND_CHAT_MS) { openChatViaSearch(task.chatName) }
        when (chatOpened) {
            null -> return fail(ErrorCode.AUTOMATION_TIMEOUT)
            ChatOpenResult.NOT_FOUND -> return fail(ErrorCode.CHAT_NOT_FOUND)
            ChatOpenResult.AMBIGUOUS -> return fail(ErrorCode.AMBIGUOUS_CHAT)
            ChatOpenResult.UNKNOWN_UI -> return fail(ErrorCode.UNKNOWN_UI_STATE)
            ChatOpenResult.OPENED -> Unit
        }

        state = AutomationState.VERIFYING_CHAT
        val verified = withTimeoutOrNull(Timeouts.OPEN_CHAT_MS) { verifyChatTitle(task.chatName) } ?: false
        if (!verified) return fail(ErrorCode.WRONG_CHAT)

        state = AutomationState.FINDING_INPUT
        val inputNode = withTimeoutOrNull(Timeouts.FIND_INPUT_MS) { pollFor { root -> adapter.findMessageInput(root) } }
        if (inputNode == null) return fail(ErrorCode.INPUT_NOT_FOUND)

        state = AutomationState.SETTING_TEXT
        if (!setText(inputNode, task.message)) return fail(ErrorCode.SET_TEXT_FAILED)

        state = AutomationState.FINDING_SEND_BUTTON
        val sendButton = withTimeoutOrNull(Timeouts.SEND_MS) { pollFor { root -> adapter.findSendButton(root) } }
        if (sendButton == null) return fail(ErrorCode.SEND_BUTTON_NOT_FOUND)

        state = AutomationState.SENDING
        if (!clickSend(sendButton)) return fail(ErrorCode.CLICK_SEND_FAILED)

        state = AutomationState.VERIFYING
        val sendConfirmed = withTimeoutOrNull(Timeouts.SEND_MS) { verifySendPerformed(task.message) } ?: false
        if (!sendConfirmed) return fail(ErrorCode.AUTOMATION_TIMEOUT)

        state = AutomationState.SUCCESS
        return AutomationResult.Success
    }

    /** Dry-run variant: everything up to (but not including) FINDING_SEND_BUTTON's click. */
    suspend fun runDryRun(chatName: String): AutomationResult {
        state = AutomationState.LAUNCHING_WHATSAPP
        if (!launchWhatsApp()) return fail(ErrorCode.WHATSAPP_LAUNCH_FAILED)

        state = AutomationState.FINDING_CHAT
        val chatOpened = withTimeoutOrNull(Timeouts.FIND_CHAT_MS) { openChatViaSearch(chatName) }
        when (chatOpened) {
            null -> return fail(ErrorCode.AUTOMATION_TIMEOUT)
            ChatOpenResult.NOT_FOUND -> return fail(ErrorCode.CHAT_NOT_FOUND)
            ChatOpenResult.AMBIGUOUS -> return fail(ErrorCode.AMBIGUOUS_CHAT)
            ChatOpenResult.UNKNOWN_UI -> return fail(ErrorCode.UNKNOWN_UI_STATE)
            ChatOpenResult.OPENED -> Unit
        }

        state = AutomationState.VERIFYING_CHAT
        val verified = withTimeoutOrNull(Timeouts.OPEN_CHAT_MS) { verifyChatTitle(chatName) } ?: false
        if (!verified) return fail(ErrorCode.WRONG_CHAT)

        state = AutomationState.FINDING_INPUT
        val inputNode = withTimeoutOrNull(Timeouts.FIND_INPUT_MS) { pollFor { root -> adapter.findMessageInput(root) } }
        if (inputNode == null) return fail(ErrorCode.INPUT_NOT_FOUND)

        state = AutomationState.FINDING_SEND_BUTTON
        val sendButton = withTimeoutOrNull(Timeouts.SEND_MS) { pollFor { root -> adapter.findSendButton(root) } }
        if (sendButton == null) return fail(ErrorCode.SEND_BUTTON_NOT_FOUND)

        state = AutomationState.SUCCESS
        return AutomationResult.Success
    }

    /**
     * Landmark-only compatibility probe (spec section 91): launches WhatsApp and
     * checks that the chat-list/search landmarks are still recognizable, without
     * searching for or opening any specific chat and without sending anything.
     */
    suspend fun runCompatibilityProbe(): AutomationResult {
        state = AutomationState.LAUNCHING_WHATSAPP
        if (!launchWhatsApp()) return fail(ErrorCode.WHATSAPP_LAUNCH_FAILED)

        state = AutomationState.FINDING_CHAT
        val searchEntry = withTimeoutOrNull(Timeouts.FIND_CHAT_MS) { pollFor { root -> adapter.findSearchEntryPoint(root) } }
        if (searchEntry == null) return fail(ErrorCode.UNKNOWN_UI_STATE)

        state = AutomationState.SUCCESS
        return AutomationResult.Success
    }

    private fun fail(errorCode: ErrorCode): AutomationResult.Failure {
        val failedState = state
        state = AutomationState.FAILED
        Logger.w(LogComponent.ACCESSIBILITY, "Automation failed: $errorCode at state $failedState")
        return AutomationResult.Failure(errorCode)
    }

    private suspend fun launchWhatsApp(): Boolean {
        val intent = service.packageManager.getLaunchIntentForPackage(whatsAppPackage) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            service.startActivity(intent)
        }.isSuccess.also {
            // Give WhatsApp a moment to come to the foreground before we start
            // looking for nodes.
            delay(600L)
        }
    }

    private enum class ChatOpenResult { OPENED, NOT_FOUND, AMBIGUOUS, UNKNOWN_UI }

    private suspend fun openChatViaSearch(chatName: String): ChatOpenResult {
        state = AutomationState.OPENING_CHAT

        currentRoot()?.let { root ->
            val openTitle = adapter.readOpenChatTitle(root)
            if (openTitle?.trim() == chatName.trim() && adapter.findMessageInput(root) != null) {
                return ChatOpenResult.OPENED
            }
            if (openTitle != null && adapter.findMessageInput(root) != null) {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                delay(500L)
            }
        }

        // pollFor() only returns once a node is found; the surrounding
        // withTimeoutOrNull() at the call site is what turns "never found"
        // into a clean AUTOMATION_TIMEOUT instead of hanging forever.
        val existingSearchInput = currentRoot()?.let { root -> adapter.findSearchInput(root) }
        val searchInput = if (existingSearchInput != null) {
            existingSearchInput
        } else {
            val searchEntry = pollFor { root -> adapter.findSearchEntryPoint(root) }
            searchEntry.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            delay(300L)
            pollFor { root -> adapter.findSearchInput(root) }
        }
        setText(searchInput, chatName)
        delay(700L) // allow WhatsApp's search results to populate

        val root = currentRoot() ?: return ChatOpenResult.UNKNOWN_UI
        val matches = adapter.findMatchingSearchResults(root, chatName)
        return when {
            matches.isEmpty() -> ChatOpenResult.NOT_FOUND
            matches.size > 1 -> ChatOpenResult.AMBIGUOUS
            else -> {
                matches.first().performAction(AccessibilityNodeInfo.ACTION_CLICK)
                delay(500L)
                ChatOpenResult.OPENED
            }
        }
    }

    private suspend fun verifyChatTitle(expected: String): Boolean {
        val actual = pollFor { root -> adapter.readOpenChatTitle(root) }
        Logger.d(LogComponent.ACCESSIBILITY, "Chat verification — expected vs actual title comparison performed")
        return actual.trim() == expected.trim()
    }

    private fun setText(node: AccessibilityNodeInfo, text: String): Boolean {
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun clickSend(node: AccessibilityNodeInfo): Boolean =
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

    private suspend fun verifySendPerformed(sentText: String): Boolean {
        // Best-effort signal only (spec section 33): the input field returning to
        // an empty/placeholder value is the most reliable node-tree-visible
        // indicator we have that the UI accepted the send action. This is NOT
        // proof of delivery.
        return pollUntil {
            val root = currentRoot() ?: return@pollUntil false
            val input = adapter.findMessageInput(root) ?: return@pollUntil false
            input.text?.toString() != sentText
        }
    }

    private fun currentRoot(): AccessibilityNodeInfo? = service.rootInActiveWindow

    private suspend fun <T> pollFor(extract: (AccessibilityNodeInfo) -> T?): T {
        while (true) {
            val root = currentRoot()
            if (root != null) {
                extract(root)?.let { return it }
            }
            delay(Timeouts.POLL_INTERVAL_MS)
        }
    }

    private suspend fun pollUntil(condition: () -> Boolean): Boolean {
        while (true) {
            if (condition()) return true
            delay(Timeouts.POLL_INTERVAL_MS)
        }
    }
}
