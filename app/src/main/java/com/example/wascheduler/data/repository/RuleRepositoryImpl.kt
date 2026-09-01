package com.example.wascheduler.data.repository

import com.example.wascheduler.data.dao.RuleDao
import com.example.wascheduler.data.entity.RuleDateEntity
import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.model.ScheduleType
import com.example.wascheduler.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepositoryImpl @Inject constructor(
    private val ruleDao: RuleDao
) : RuleRepository {

    override fun observeRules(): Flow<List<Rule>> =
        ruleDao.observeAllWithTimesRaw().map { rules ->
            rules.map { rule -> rule.toDomain(ruleDao.timesForRule(rule.id), ruleDao.datesForRule(rule.id)) }
        }

    override fun observeRule(ruleId: Long): Flow<Rule?> =
        ruleDao.observeRule(ruleId).map { entity ->
            entity?.let { it.toDomain(ruleDao.timesForRule(it.id), ruleDao.datesForRule(it.id)) }
        }

    override suspend fun getRule(ruleId: Long): Rule? =
        ruleDao.getRule(ruleId)?.let { it.toDomain(ruleDao.timesForRule(it.id), ruleDao.datesForRule(it.id)) }

    override suspend fun getAllEnabledRules(): List<Rule> =
        ruleDao.getEnabledRules().map { it.toDomain(ruleDao.timesForRule(it.id), ruleDao.datesForRule(it.id)) }

    override suspend fun upsertRule(rule: Rule): Long =
        ruleDao.upsertRuleWithSchedule(
            rule.toEntity(),
            rule.times.map { it.toEntity() },
            rule.persistedDateEntities()
        )

    override suspend fun duplicateRule(ruleId: Long): Long {
        val source = getRule(ruleId) ?: return 0L
        val now = System.currentTimeMillis()
        val copy = source.copy(
            id = 0,
            name = "${source.name.ifBlank { source.chatName }} (copy)",
            enabled = false,
            times = source.times.map { it.copy(id = 0, ruleId = 0) },
            createdAt = now,
            updatedAt = now
        )
        return upsertRule(copy)
    }

    override suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean) {
        ruleDao.setEnabled(ruleId, enabled, System.currentTimeMillis())
    }

    override suspend fun deleteRule(ruleId: Long) {
        ruleDao.deleteRuleWithSchedule(ruleId)
    }

    private fun Rule.persistedDateEntities(): List<RuleDateEntity> =
        when (scheduleType) {
            ScheduleType.WEEKLY -> emptyList()
            ScheduleType.SPECIFIC_DATE -> dates.distinct().sorted().map { RuleDateEntity(ruleId = id, localDate = it) }
            ScheduleType.MULTIPLE_DATES -> times.mapNotNull { it.localDate }.distinct().sorted().map { RuleDateEntity(ruleId = id, localDate = it) }
        }
}
