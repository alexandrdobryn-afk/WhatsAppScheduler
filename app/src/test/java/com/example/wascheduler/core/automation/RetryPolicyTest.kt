package com.example.wascheduler.core.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest

class RetryPolicyTest {

    @Test
    fun `default policy allows exactly 3 attempts`() = runTest {
        val policy = RetryPolicy()
        assertTrue(policy.hasMoreAttempts(1))
        assertTrue(policy.hasMoreAttempts(2))
        assertFalse(policy.hasMoreAttempts(3))
    }

    @Test
    fun `delay before attempt 2 is 30s and before attempt 3 is 120s`() = runTest {
        val policy = RetryPolicy()
        assertEquals(30_000L, policy.delayBeforeAttempt(2))
        assertEquals(120_000L, policy.delayBeforeAttempt(3))
    }

    @Test
    fun `no delay defined beyond configured attempts`() = runTest {
        val policy = RetryPolicy()
        assertNull(policy.delayBeforeAttempt(4))
    }

    @Test
    fun `configured zero retry attempts disables retry scheduling`() = runTest {
        val policy = RetryPolicy(maxAttemptsProvider = { 1 })
        assertFalse(policy.hasMoreAttempts(1))
        assertNull(policy.delayBeforeAttempt(2))
    }
}
