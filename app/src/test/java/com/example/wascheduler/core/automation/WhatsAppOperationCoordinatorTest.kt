package com.example.wascheduler.core.automation

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class WhatsAppOperationCoordinatorTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `serializes overlapping WhatsApp operations`() = runTest {
        val coordinator = WhatsAppOperationCoordinator()
        coordinator.replaceLoggerForTests {}
        val active = AtomicInteger(0)
        val maxActive = AtomicInteger(0)

        val first = async {
            coordinator.runExclusive("scheduled") {
                val current = active.incrementAndGet()
                maxActive.updateAndGet { maxOf(it, current) }
                delay(100)
                active.decrementAndGet()
            }
        }
        val second = async {
            coordinator.runExclusive("dry_run") {
                val current = active.incrementAndGet()
                maxActive.updateAndGet { maxOf(it, current) }
                delay(100)
                active.decrementAndGet()
            }
        }

        advanceUntilIdle()
        first.await()
        second.await()

        assertEquals(1, maxActive.get())
        assertEquals(0, active.get())
    }

    @Test
    fun `releases mutex after exception`() = runTest {
        val coordinator = WhatsAppOperationCoordinator()
        coordinator.replaceLoggerForTests {}
        val failed = runCatching {
            coordinator.runExclusive("failing") {
                error("boom")
            }
        }

        var secondOperationRan = false
        coordinator.runExclusive("second") {
            secondOperationRan = true
        }

        assertFalse(failed.isSuccess)
        assertTrue(secondOperationRan)
    }
}
