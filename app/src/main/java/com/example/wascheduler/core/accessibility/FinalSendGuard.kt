package com.example.wascheduler.core.accessibility

import com.example.wascheduler.core.automation.ExecutionWindowPolicy
import com.example.wascheduler.domain.model.ErrorCode
import java.time.LocalDateTime

data class FinalSendGuardInput(
    val activePackageName: String?,
    val expectedPackageName: String,
    val actualChatTitle: String?,
    val expectedChatTitle: String,
    val inputText: String?,
    val expectedMessage: String,
    val sendButtonPresent: Boolean,
    val state: AutomationState,
    val scheduledAt: LocalDateTime?,
    val allowedDelayMinutes: Int?,
    val now: LocalDateTime?
)

sealed class FinalSendGuardDecision {
    data object Allow : FinalSendGuardDecision()
    data class Block(val errorCode: ErrorCode) : FinalSendGuardDecision()
}

object FinalSendGuard {
    fun evaluate(input: FinalSendGuardInput): FinalSendGuardDecision {
        if (input.state != AutomationState.FINDING_SEND_BUTTON) {
            return FinalSendGuardDecision.Block(ErrorCode.UNKNOWN_UI_STATE)
        }
        if (input.activePackageName != input.expectedPackageName) {
            return FinalSendGuardDecision.Block(ErrorCode.UNKNOWN_UI_STATE)
        }
        if (input.actualChatTitle?.trim() != input.expectedChatTitle.trim()) {
            return FinalSendGuardDecision.Block(ErrorCode.WRONG_CHAT)
        }
        if (input.inputText != input.expectedMessage) {
            return FinalSendGuardDecision.Block(ErrorCode.INPUT_MISMATCH)
        }
        if (!input.sendButtonPresent) {
            return FinalSendGuardDecision.Block(ErrorCode.SEND_BUTTON_NOT_FOUND)
        }

        val scheduledAt = input.scheduledAt
        val allowedDelayMinutes = input.allowedDelayMinutes
        val now = input.now
        if (scheduledAt != null && allowedDelayMinutes != null && now != null) {
            if (!ExecutionWindowPolicy.isWithinWindow(scheduledAt, now, allowedDelayMinutes)) {
                return FinalSendGuardDecision.Block(ErrorCode.MISSED_WINDOW)
            }
        }

        return FinalSendGuardDecision.Allow
    }
}
