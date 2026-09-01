package com.example.wascheduler.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.wascheduler.data.database.AppDatabase
import com.example.wascheduler.data.entity.RuleDateEntity
import com.example.wascheduler.data.entity.RuleEntity
import com.example.wascheduler.data.entity.RuleTimeEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class RuleDaoSchedulePersistenceTest {

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

    @Test
    fun upsertRuleWithSchedule_persistsRuleTimesAndDatesAtomically() = runBlocking {
        val dao = db.ruleDao()
        val ruleId = dao.upsertRuleWithSchedule(
            rule = sampleRule(scheduleType = "MULTIPLE_DATES"),
            times = listOf(
                sampleTime(LocalDate.of(2026, 8, 25), LocalTime.of(8, 0)),
                sampleTime(LocalDate.of(2026, 8, 28), LocalTime.of(12, 30))
            ),
            dates = listOf(sampleDate(LocalDate.of(2026, 8, 25)), sampleDate(LocalDate.of(2026, 8, 28)))
        )

        val persisted = dao.getRule(ruleId)
        val times = dao.timesForRule(ruleId)
        val dates = dao.datesForRule(ruleId)

        assertEquals("MULTIPLE_DATES", persisted?.scheduleType)
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(12, 30)), times.map { it.localTime })
        assertEquals(listOf(LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 28)), times.map { it.localDate })
        assertEquals(listOf(LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 28)), dates.map { it.localDate })
    }

    @Test
    fun deleteRuleWithSchedule_removesChildTimesAndDates() = runBlocking {
        val dao = db.ruleDao()
        val ruleId = dao.upsertRuleWithSchedule(
            rule = sampleRule(scheduleType = "SPECIFIC_DATE"),
            times = listOf(sampleTime(null, LocalTime.of(9, 0))),
            dates = listOf(sampleDate(LocalDate.of(2026, 8, 25)))
        )

        dao.deleteRuleWithSchedule(ruleId)

        assertEquals(null, dao.getRule(ruleId))
        assertTrue(dao.timesForRule(ruleId).isEmpty())
        assertTrue(dao.datesForRule(ruleId).isEmpty())
    }

    private fun sampleRule(scheduleType: String) = RuleEntity(
        name = "Test",
        chatName = "Test group",
        message = "WA_TEST",
        enabled = true,
        scheduleType = scheduleType,
        startDate = LocalDate.of(2026, 8, 25),
        allowedDelayMinutes = 10,
        createdAt = 1L,
        updatedAt = 1L
    )

    private fun sampleTime(localDate: LocalDate?, localTime: LocalTime) = RuleTimeEntity(
        ruleId = 0,
        localTime = localTime,
        localDate = localDate,
        monday = false,
        tuesday = false,
        wednesday = false,
        thursday = false,
        friday = false,
        saturday = false,
        sunday = false,
        enabled = true
    )

    private fun sampleDate(localDate: LocalDate) = RuleDateEntity(ruleId = 0, localDate = localDate)
}
