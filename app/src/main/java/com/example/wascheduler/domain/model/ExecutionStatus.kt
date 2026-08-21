package com.example.wascheduler.domain.model

/**
 * Minimal status set per spec section 34.
 *
 * IMPORTANT: SENT means only "the app successfully performed the send action in
 * WhatsApp's UI". It never means delivered, read, or received by anyone. Do not
 * rename this to "Delivered" anywhere in the UI or logs — see spec section 33.
 */
enum class ExecutionStatus {
    SCHEDULED,
    RUNNING,
    SENT,
    FAILED,
    SKIPPED
}
