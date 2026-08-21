package com.example.wascheduler.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.wascheduler.data.database.AppDatabase
import com.example.wascheduler.data.entity.ExecutionEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * Covers spec section 85 "Deduplication" test list: one alarm, two identical
 * alarms, concurrent trigger. Reboot/retry duplication is covered indirectly —
 * both paths go through the same [tryClaim] choke point exercised here.
 */
@RunWith(AndroidJUnit4::class)
class ExecutionDaoDedupTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sampleExecution(occurrenceId: String, status: String = "RUNNING") = ExecutionEntity(
        occurrenceId = occurrenceId,
        ruleId = 1,
        scheduledAt = LocalDateTime.of(2026, 8, 14, 9, 0),
        startedAt = LocalDateTime.of(2026, 8, 14, 9, 0),
        finishedAt = null,
        status = status,
        attemptNumber = 1,
        targetChat = "Test group",
        messagePreview = "+",
        errorCode = null,
        errorMessage = null
    )

    @Test
    fun singleClaim_succeeds() = runBlocking {
        val id = db.executionDao().tryClaim(sampleExecution("occ_1"))
        assertNotNull(id)
    }

    @Test
    fun secondClaimAttempt_whileRunning_isRejected() = runBlocking {
        val first = db.executionDao().tryClaim(sampleExecution("occ_2"))
        assertNotNull(first)
        val second = db.executionDao().tryClaim(sampleExecution("occ_2"))
        assertNull("A second claim attempt while the first is RUNNING must be rejected", second)
    }

    @Test
    fun secondClaimAttempt_afterSent_isRejected() = runBlocking {
        db.executionDao().tryClaim(sampleExecution("occ_3", status = "SENT"))
        val second = db.executionDao().tryClaim(sampleExecution("occ_3"))
        assertNull("A second claim attempt after SENT must be rejected", second)
    }

    @Test
    fun reclaimAfterFailed_isAllowed_forRetry() = runBlocking {
        db.executionDao().tryClaim(sampleExecution("occ_4", status = "FAILED"))
        val retryClaim = db.executionDao().tryClaim(sampleExecution("occ_4", status = "RUNNING"))
        assertNotNull("A FAILED occurrence must be re-claimable for a retry attempt", retryClaim)
    }

    @Test
    fun concurrentClaimAttempts_onlyOneWins() = runBlocking {
        val attempts = 20
        val results = (1..attempts).map {
            async { db.executionDao().tryClaim(sampleExecution("occ_concurrent")) }
        }.awaitAll()
        val successes = results.count { it != null }
        assertEquals("Exactly one concurrent claim must win, never zero or more than one", 1, successes)
    }
}
