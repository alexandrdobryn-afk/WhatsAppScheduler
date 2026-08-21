package com.example.wascheduler.data.repository

import com.example.wascheduler.data.dao.ExecutionDao
import com.example.wascheduler.domain.model.Execution
import com.example.wascheduler.domain.model.ExecutionStatus
import com.example.wascheduler.domain.repository.ExecutionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecutionRepositoryImpl @Inject constructor(
    private val executionDao: ExecutionDao
) : ExecutionRepository {

    override fun observeRecent(limit: Int): Flow<List<Execution>> =
        executionDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun observeLatest(): Flow<Execution?> =
        executionDao.observeLatest().map { it?.toDomain() }

    override suspend fun tryClaim(execution: Execution): Boolean =
        executionDao.tryClaim(execution.toEntity()) != null

    override suspend fun update(execution: Execution) {
        executionDao.update(execution.toEntity())
    }

    override suspend fun getByOccurrenceId(occurrenceId: String): Execution? =
        executionDao.getByOccurrenceId(occurrenceId)?.toDomain()

    override suspend fun hasTerminalOrRunning(occurrenceId: String): Boolean {
        val existing = executionDao.getByOccurrenceId(occurrenceId) ?: return false
        return existing.status in setOf(
            ExecutionStatus.RUNNING.name,
            ExecutionStatus.SENT.name,
            ExecutionStatus.FAILED.name,
            ExecutionStatus.SKIPPED.name
        )
    }

    override suspend fun countByStatus(status: ExecutionStatus): Int =
        executionDao.countByStatus(status.name)
}
