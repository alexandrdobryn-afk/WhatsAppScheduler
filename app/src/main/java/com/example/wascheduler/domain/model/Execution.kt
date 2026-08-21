package com.example.wascheduler.domain.model

import java.time.LocalDateTime

/**
 * Deterministic ID for one scheduled occurrence: ruleId + the exact local
 * date-time it was due. Used as the deduplication key so the same occurrence
 * can never be executed twice, no matter which component (AlarmReceiver, a
 * retry worker, or the boot rescheduler) tries to run it (spec section 39).
 */
data class OccurrenceId(val ruleId: Long, val scheduledAt: LocalDateTime) {
    /** Stable string form suitable as a DB primary key / WorkManager unique name. */
    override fun toString(): String = "rule_${ruleId}_$scheduledAt"

    companion object {
        fun parse(raw: String): OccurrenceId? {
            val body = raw.removePrefix("rule_")
            val sepIndex = body.indexOf('_')
            if (sepIndex <= 0) return null
            val ruleId = body.substring(0, sepIndex).toLongOrNull() ?: return null
            val dateTime = runCatching { LocalDateTime.parse(body.substring(sepIndex + 1)) }
                .getOrNull() ?: return null
            return OccurrenceId(ruleId, dateTime)
        }
    }
}

data class Execution(
    val id: Long = 0,
    val occurrenceId: String,
    val ruleId: Long,
    val scheduledAt: LocalDateTime,
    val startedAt: LocalDateTime? = null,
    val finishedAt: LocalDateTime? = null,
    val status: ExecutionStatus,
    val attemptNumber: Int = 1,
    val targetChat: String,
    val messagePreview: String,
    val errorCode: ErrorCode? = null,
    val errorMessage: String? = null
)
