package com.example.wascheduler.core.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.example.wascheduler.core.logging.LogComponent
import com.example.wascheduler.core.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Heuristic, resource-id-first implementation of [WhatsAppUiAdapter].
 *
 * IMPORTANT / KNOWN LIMITATION (see spec sections 89-91 and README "Known
 * limitations"): WhatsApp does not publish a stable, versioned accessibility
 * contract. The `viewIdResourceName` values below reflect commonly-observed
 * WhatsApp builds at the time this adapter was written, but WhatsApp can (and
 * periodically does) change its view hierarchy, which will break resource-id
 * matching until this file is updated. That is exactly why every lookup here
 * falls back to semantic properties (className + editable/clickable flags +
 * contentDescription) rather than ever falling back to screen coordinates, and
 * why the app surfaces a clear "WhatsApp changed, automation unavailable"
 * message (spec section 90) instead of guessing when nothing matches.
 *
 * Localization (spec section 69): content-description / text fallbacks are
 * checked against multiple language variants because WhatsApp's UI language
 * follows the phone's system language, not this app's language.
 */
@Singleton
class WhatsAppUiAdapterImpl @Inject constructor() : WhatsAppUiAdapter {

    private val searchEntryIds = listOf(
        "com.whatsapp:id/menuitem_search",
        "com.whatsapp:id/search",
        "com.whatsapp:id/search_bar_inner_layout"
    )
    private val searchInputIds = listOf("com.whatsapp:id/search_src_text", "com.whatsapp:id/search_input")
    private val chatRowTitleIds = listOf("com.whatsapp:id/conversations_row_contact_name")
    private val chatResultContainerIds = listOf("com.whatsapp:id/contact_row_container")
    private val chatHeaderTitleIds = listOf("com.whatsapp:id/conversation_contact_name")
    private val messageInputIds = listOf("com.whatsapp:id/entry")
    private val sendButtonIds = listOf("com.whatsapp:id/send")

    private val searchLabels = listOf("Search", "Поиск", "Пошук")
    private val sendLabels = listOf("Send", "Отправить", "Надіслати")

    override fun isKnownScreen(root: AccessibilityNodeInfo): Boolean {
        // A screen is "known" if we can find at least one landmark node type we
        // understand (chat list, search box, message input, or send button).
        return findByIdOrLabel(root, searchEntryIds, searchLabels, requireClickable = true) != null ||
            findByIdOrLabel(root, messageInputIds, emptyList(), requireEditable = true) != null ||
            findByIdOrLabel(root, sendButtonIds, sendLabels, requireClickable = true) != null ||
            findByAnyId(root, chatRowTitleIds) != null
    }

    override fun findSearchEntryPoint(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        findByIdOrLabel(root, searchEntryIds, searchLabels, requireClickable = true)

    override fun findSearchInput(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        findByIdOrLabel(root, searchInputIds, emptyList(), requireEditable = true)
            ?: findFirstEditable(root)

    override fun findMatchingSearchResults(root: AccessibilityNodeInfo, chatName: String): List<AccessibilityNodeInfo> {
        val rows = mutableListOf<AccessibilityNodeInfo>()
        collectByIds(root, chatRowTitleIds, rows)
        return rows.mapNotNull { node ->
            val text = node.text?.toString()?.trim()
            if (text != null && text.equals(chatName.trim(), ignoreCase = false)) {
                node.ancestorWithAnyId(chatResultContainerIds)
            } else {
                null
            }
        }
    }

    override fun readOpenChatTitle(root: AccessibilityNodeInfo): String? {
        val node = findByAnyId(root, chatHeaderTitleIds) ?: return null
        return node.text?.toString()?.trim()
    }

    override fun findMessageInput(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        findByIdOrLabel(root, messageInputIds, emptyList(), requireEditable = true)
            ?: findFirstEditable(root)

    override fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        findByIdOrLabel(root, sendButtonIds, sendLabels, requireClickable = true)

    override fun isUnknownBlockingDialog(root: AccessibilityNodeInfo): Boolean {
        // Heuristic: a window whose className indicates a system/alert dialog and
        // that does not contain any of our known landmark nodes is treated as an
        // unknown blocking screen. We deliberately never click buttons on it.
        val looksLikeDialog = root.className?.contains("Dialog", ignoreCase = true) == true
        return looksLikeDialog && !isKnownScreen(root)
    }

    // ---- node tree helpers -------------------------------------------------

    private fun findByAnyId(root: AccessibilityNodeInfo, ids: List<String>): AccessibilityNodeInfo? {
        for (id in ids) {
            val matches = root.findAccessibilityNodeInfosByViewId(id)
            if (!matches.isNullOrEmpty()) return matches.first()
        }
        return null
    }

    private fun collectByIds(root: AccessibilityNodeInfo, ids: List<String>, out: MutableList<AccessibilityNodeInfo>) {
        for (id in ids) {
            root.findAccessibilityNodeInfosByViewId(id)?.let { out.addAll(it) }
        }
    }

    private fun findByIdOrLabel(
        root: AccessibilityNodeInfo,
        ids: List<String>,
        labels: List<String>,
        requireClickable: Boolean = false,
        requireEditable: Boolean = false
    ): AccessibilityNodeInfo? {
        findByAnyId(root, ids, requireClickable, requireEditable)?.let { return it }
        if (labels.isEmpty()) return null
        return findByLabelRecursive(root, labels, requireClickable, requireEditable)
    }

    private fun findByLabelRecursive(
        node: AccessibilityNodeInfo,
        labels: List<String>,
        requireClickable: Boolean,
        requireEditable: Boolean
    ): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()
        val text = node.text?.toString()
        val matchesLabel = labels.any { label ->
            desc?.startsWith(label, ignoreCase = true) == true ||
                text?.startsWith(label, ignoreCase = true) == true
        }
        if (matchesLabel && (!requireClickable || node.isClickable) && (!requireEditable || node.isEditable)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findByLabelRecursive(child, labels, requireClickable, requireEditable)?.let { return it }
        }
        return null
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findFirstEditable(child)?.let { return it }
        }
        return null
    }

    private fun AccessibilityNodeInfo.ancestorWithAnyId(ids: List<String>): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = this
        repeat(MAX_PARENT_SEARCH_DEPTH) {
            val node = current ?: return null
            if (node.viewIdResourceName in ids && node.isClickable) return node
            current = node.parent
        }
        return null
    }

    private fun findByAnyId(
        root: AccessibilityNodeInfo,
        ids: List<String>,
        requireClickable: Boolean,
        requireEditable: Boolean
    ): AccessibilityNodeInfo? {
        for (id in ids) {
            val matches = root.findAccessibilityNodeInfosByViewId(id).orEmpty()
            matches.firstOrNull { node ->
                (!requireClickable || node.isClickable) && (!requireEditable || node.isEditable)
            }?.let { return it }
        }
        return null
    }

    init {
        Logger.d(LogComponent.WHATSAPP_ADAPTER, "WhatsAppUiAdapterImpl initialized")
    }

    companion object {
        private const val MAX_PARENT_SEARCH_DEPTH = 6
    }
}
