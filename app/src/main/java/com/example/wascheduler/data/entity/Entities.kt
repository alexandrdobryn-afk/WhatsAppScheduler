package com.example.wascheduler.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val chatName: String,
    val message: String,
    val enabled: Boolean,
    @ColumnInfo(defaultValue = "'WEEKLY'") val scheduleType: String,
    @ColumnInfo(defaultValue = "'1970-01-01'") val startDate: LocalDate,
    val allowedDelayMinutes: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "rule_times",
    foreignKeys = [
        ForeignKey(
            entity = RuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ruleId")]
)
data class RuleTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Long,
    val localTime: LocalTime,
    val monday: Boolean,
    val tuesday: Boolean,
    val wednesday: Boolean,
    val thursday: Boolean,
    val friday: Boolean,
    val saturday: Boolean,
    val sunday: Boolean,
    val enabled: Boolean
)

@Entity(
    tableName = "rule_dates",
    foreignKeys = [
        ForeignKey(
            entity = RuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ruleId"), Index(value = ["ruleId", "localDate"], unique = true)]
)
data class RuleDateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Long,
    val localDate: LocalDate
)

/**
 * execution_logs table. occurrenceId (ruleId + scheduledAt) carries a UNIQUE
 * index so a second insert attempt for the same occurrence fails fast at the
 * DB layer as a second line of defense against double-sends, in addition to
 * the explicit tryClaim() transaction (spec sections 39-40).
 */
@Entity(
    tableName = "execution_logs",
    indices = [Index(value = ["occurrenceId"], unique = true), Index("ruleId")]
)
data class ExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val occurrenceId: String,
    val ruleId: Long,
    val scheduledAt: LocalDateTime,
    val startedAt: LocalDateTime?,
    val finishedAt: LocalDateTime?,
    val status: String,
    val attemptNumber: Int,
    val targetChat: String,
    val messagePreview: String,
    val errorCode: String?,
    val errorMessage: String?
)
