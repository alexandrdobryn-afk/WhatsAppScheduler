package com.example.wascheduler.domain.repository

import com.example.wascheduler.domain.model.Execution
import com.example.wascheduler.domain.model.ExecutionStatus
import com.example.wascheduler.domain.model.Rule
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
    fun observeRules(): Flow<List<Rule>>
    fun observeRule(ruleId: Long): Flow<Rule?>
    suspend fun getRule(ruleId: Long): Rule?
    suspend fun getAllEnabledRules(): List<Rule>
    /** Returns the persisted rule id (existing id when updating, new id on insert). */
    suspend fun upsertRule(rule: Rule): Long
    suspend fun duplicateRule(ruleId: Long): Long
    suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean)
    suspend fun deleteRule(ruleId: Long)
}

interface ExecutionRepository {
    fun observeRecent(limit: Int = 200): Flow<List<Execution>>
    fun observeLatest(): Flow<Execution?>

    /**
     * Attempts to atomically claim an occurrence for execution. Returns true only
     * if this call transitioned the occurrence from "not present / not terminal"
     * into RUNNING — i.e. only one caller across the whole app can ever win this
     * race (spec sections 39-40). All callers (AlarmReceiver, retry worker, boot
     * rescheduler) must go through this before touching WhatsApp.
     */
    suspend fun tryClaim(execution: Execution): Boolean

    suspend fun update(execution: Execution)
    suspend fun getByOccurrenceId(occurrenceId: String): Execution?
    suspend fun hasTerminalOrRunning(occurrenceId: String): Boolean
    suspend fun countByStatus(status: ExecutionStatus): Int
}
