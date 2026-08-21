package com.example.wascheduler.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.wascheduler.data.entity.RuleEntity
import com.example.wascheduler.data.entity.RuleTimeEntity
import kotlinx.coroutines.flow.Flow

data class RuleWithTimes(
    val rule: RuleEntity,
    val times: List<RuleTimeEntity>
)

@Dao
interface RuleDao {

    @Query("SELECT * FROM rules ORDER BY name")
    fun observeAllWithTimesRaw(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rule_times WHERE ruleId = :ruleId")
    suspend fun timesForRule(ruleId: Long): List<RuleTimeEntity>

    @Query("SELECT * FROM rule_times")
    suspend fun allTimes(): List<RuleTimeEntity>

    @Query("SELECT * FROM rules WHERE id = :ruleId")
    suspend fun getRule(ruleId: Long): RuleEntity?

    @Query("SELECT * FROM rules WHERE id = :ruleId")
    fun observeRule(ruleId: Long): Flow<RuleEntity?>

    @Query("SELECT * FROM rules WHERE enabled = 1")
    suspend fun getEnabledRules(): List<RuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Query("UPDATE rules SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :ruleId")
    suspend fun setEnabled(ruleId: Long, enabled: Boolean, updatedAt: Long)

    @Query("DELETE FROM rules WHERE id = :ruleId")
    suspend fun deleteRule(ruleId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimes(times: List<RuleTimeEntity>): List<Long>

    @Query("DELETE FROM rule_times WHERE ruleId = :ruleId")
    suspend fun deleteTimesForRule(ruleId: Long)

    @Delete
    suspend fun deleteTime(time: RuleTimeEntity)

    /** Replaces a rule's time entries atomically so partial updates are never visible. */
    @Transaction
    suspend fun replaceRuleTimes(ruleId: Long, times: List<RuleTimeEntity>) {
        deleteTimesForRule(ruleId)
        insertTimes(times)
    }

    @Transaction
    suspend fun upsertRuleWithTimes(rule: RuleEntity, times: List<RuleTimeEntity>): Long {
        val ruleId = if (rule.id == 0L) insertRule(rule) else {
            updateRule(rule)
            rule.id
        }
        replaceRuleTimes(ruleId, times.map { it.copy(ruleId = ruleId) })
        return ruleId
    }

    @Transaction
    suspend fun deleteRuleWithTimes(ruleId: Long) {
        deleteTimesForRule(ruleId)
        deleteRule(ruleId)
    }
}
