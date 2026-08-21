package com.example.wascheduler.data.repository

import com.example.wascheduler.data.dao.RuleDao
import com.example.wascheduler.domain.model.Rule
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
            rules.map { rule -> rule.toDomain(ruleDao.timesForRule(rule.id)) }
        }

    override fun observeRule(ruleId: Long): Flow<Rule?> =
        ruleDao.observeRule(ruleId).map { entity ->
            entity?.let { it.toDomain(ruleDao.timesForRule(it.id)) }
        }

    override suspend fun getRule(ruleId: Long): Rule? =
        ruleDao.getRule(ruleId)?.let { it.toDomain(ruleDao.timesForRule(it.id)) }

    override suspend fun getAllEnabledRules(): List<Rule> =
        ruleDao.getEnabledRules().map { it.toDomain(ruleDao.timesForRule(it.id)) }

    override suspend fun upsertRule(rule: Rule): Long =
        ruleDao.upsertRuleWithTimes(rule.toEntity(), rule.times.map { it.toEntity() })

    override suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean) {
        ruleDao.setEnabled(ruleId, enabled, System.currentTimeMillis())
    }

    override suspend fun deleteRule(ruleId: Long) {
        ruleDao.deleteRuleWithTimes(ruleId)
    }
}
