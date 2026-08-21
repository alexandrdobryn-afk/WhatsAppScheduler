package com.example.wascheduler.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.wascheduler.data.entity.ExecutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionDao {

    @Query("SELECT * FROM execution_logs ORDER BY scheduledAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ExecutionEntity>>

    @Query("SELECT * FROM execution_logs ORDER BY scheduledAt DESC LIMIT 1")
    fun observeLatest(): Flow<ExecutionEntity?>

    @Query("SELECT * FROM execution_logs WHERE occurrenceId = :occurrenceId LIMIT 1")
    suspend fun getByOccurrenceId(occurrenceId: String): ExecutionEntity?

    @Query("SELECT COUNT(*) FROM execution_logs WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(execution: ExecutionEntity): Long

    @Update
    suspend fun update(execution: ExecutionEntity)

    /**
     * Atomically claims an occurrence: succeeds (returns true) only if no row for
     * this occurrenceId exists yet, or the existing row is not RUNNING/SENT. This
     * is the single choke point that prevents AlarmReceiver, a retry worker, and
     * the boot rescheduler from ever double-executing the same occurrence
     * (spec sections 39-40). Room runs this whole function in one transaction,
     * so the read-then-write is atomic with respect to other DB transactions.
     */
    @Transaction
    suspend fun tryClaim(candidate: ExecutionEntity): Long? {
        val existing = getByOccurrenceId(candidate.occurrenceId)
        if (existing != null && (existing.status == "RUNNING" || existing.status == "SENT")) {
            return null
        }
        return if (existing == null) {
            insert(candidate)
        } else {
            val claimed = candidate.copy(id = existing.id)
            update(claimed)
            existing.id
        }
    }
}
