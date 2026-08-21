package com.example.wascheduler.core.accessibility

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Every WhatsApp-specific resource ID, contentDescription, or text selector in
 * the whole codebase must live behind this interface (spec sections 67-69). No
 * other layer — ViewModel, scheduler, AutomationEngine — is allowed to know
 * about WhatsApp's UI structure.
 *
 * All lookups use the Accessibility Node Tree exclusively. Coordinate-based
 * automation (tap at x,y) is forbidden by spec section 27 and is never used
 * here, even as a fallback.
 */
interface WhatsAppUiAdapter {

    /** True once the root node belongs to a screen this adapter recognizes at all. */
    fun isKnownScreen(root: AccessibilityNodeInfo): Boolean

    /** Finds the search entry point on WhatsApp's chat list screen, if visible. */
    fun findSearchEntryPoint(root: AccessibilityNodeInfo): AccessibilityNodeInfo?

    /** Finds the text field used to type a search query, if a search UI is open. */
    fun findSearchInput(root: AccessibilityNodeInfo): AccessibilityNodeInfo?

    /** Finds the first search-result row whose visible title matches [chatName] exactly. */
    fun findMatchingSearchResults(root: AccessibilityNodeInfo, chatName: String): List<AccessibilityNodeInfo>

    /** Reads the title actually shown in the currently-open chat's header, if any. */
    fun readOpenChatTitle(root: AccessibilityNodeInfo): String?

    /** Finds the message composer (text input) inside an open chat. */
    fun findMessageInput(root: AccessibilityNodeInfo): AccessibilityNodeInfo?

    /** Finds the send button inside an open chat. */
    fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo?

    /** True if a blocking, unrecognized dialog/popup appears to be showing. */
    fun isUnknownBlockingDialog(root: AccessibilityNodeInfo): Boolean
}
