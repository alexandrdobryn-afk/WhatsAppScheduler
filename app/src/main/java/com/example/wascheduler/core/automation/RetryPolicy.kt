package com.example.wascheduler.core.automation

/**
 * Single source of truth for retry behavior. Nothing else in the codebase should
 * hardcode attempt counts or backoff delays (spec section 99).
 */
class RetryPolicy(
    private val maxAttemptsProvider: suspend () -> Int = { DEFAULT_MAX_ATTEMPTS },
    private val delaysMillis: List<Long> = DEFAULT_DELAYS_MILLIS
) {
    /** Delay before the given (1-indexed) attempt number, or null if no more retries. */
    suspend fun delayBeforeAttempt(attemptNumber: Int): Long? {
        if (attemptNumber > maxAttemptsProvider()) return null
        val index = attemptNumber - 2 // attempt 2 uses delaysMillis[0], attempt 3 uses [1], ...
        return delaysMillis.getOrNull(index)
    }

    suspend fun hasMoreAttempts(attemptNumber: Int): Boolean = attemptNumber < maxAttemptsProvider()

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
        val DEFAULT_DELAYS_MILLIS = listOf(30_000L, 120_000L)
    }
}
