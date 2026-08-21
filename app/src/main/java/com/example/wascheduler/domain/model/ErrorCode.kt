package com.example.wascheduler.domain.model

/**
 * Structured error codes. Every failure in the system must map to exactly one
 * of these — free-form exception messages are never surfaced to the user or
 * stored as the primary failure reason.
 */
enum class ErrorCode {
    WHATSAPP_NOT_INSTALLED,
    ACCESSIBILITY_DISABLED,
    ACCESSIBILITY_NOT_CONNECTED,
    EXACT_ALARM_DISABLED,
    DEVICE_LOCKED,
    WHATSAPP_LAUNCH_FAILED,
    CHAT_NOT_FOUND,
    AMBIGUOUS_CHAT,
    WRONG_CHAT,
    INPUT_NOT_FOUND,
    SEND_BUTTON_NOT_FOUND,
    SET_TEXT_FAILED,
    CLICK_SEND_FAILED,
    AUTOMATION_TIMEOUT,
    MISSED_WINDOW,
    DUPLICATE_EXECUTION,
    NO_NETWORK,
    UNKNOWN_UI_STATE,
    UNKNOWN_ERROR;

    /** Resource-name style key used to look up a localized string, e.g. "error_CHAT_NOT_FOUND". */
    val stringResKey: String get() = "error_$name"
}
