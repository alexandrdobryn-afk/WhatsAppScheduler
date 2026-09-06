package com.example.wascheduler.core.accessibility

import com.example.wascheduler.domain.model.ErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class FinalSendGuardTest {
    private val validInput = FinalSendGuardInput(
        activePackageName = "com.whatsapp",
        expectedPackageName = "com.whatsapp",
        actualChatTitle = "Project Team",
        expectedChatTitle = "Project Team",
        inputText = "Status update",
        expectedMessage = "Status update",
        sendButtonPresent = true,
        state = AutomationState.FINDING_SEND_BUTTON,
        scheduledAt = LocalDateTime.of(2026, 9, 5, 14, 0),
        allowedDelayMinutes = 10,
        now = LocalDateTime.of(2026, 9, 5, 14, 5)
    )

    @Test
    fun `allows only correct package chat message send button and deadline`() {
        assertTrue(FinalSendGuard.evaluate(validInput) is FinalSendGuardDecision.Allow)
    }

    @Test
    fun `wrong package blocks send`() {
        assertBlocked(validInput.copy(activePackageName = "com.android.settings"), ErrorCode.UNKNOWN_UI_STATE)
    }

    @Test
    fun `wrong chat blocks send`() {
        assertBlocked(validInput.copy(actualChatTitle = "Other Team"), ErrorCode.WRONG_CHAT)
    }

    @Test
    fun `input mismatch blocks send`() {
        assertBlocked(validInput.copy(inputText = "Different text"), ErrorCode.INPUT_MISMATCH)
    }

    @Test
    fun `missing refreshed send button blocks send`() {
        assertBlocked(validInput.copy(sendButtonPresent = false), ErrorCode.SEND_BUTTON_NOT_FOUND)
    }

    @Test
    fun `expired allowed delay blocks send`() {
        assertBlocked(validInput.copy(now = LocalDateTime.of(2026, 9, 5, 14, 11)), ErrorCode.MISSED_WINDOW)
    }

    private fun assertBlocked(input: FinalSendGuardInput, expected: ErrorCode) {
        val decision = FinalSendGuard.evaluate(input)
        assertTrue(decision is FinalSendGuardDecision.Block)
        assertEquals(expected, (decision as FinalSendGuardDecision.Block).errorCode)
    }
}
