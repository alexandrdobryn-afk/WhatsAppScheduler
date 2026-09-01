package com.example.wascheduler.data.repository

import com.example.wascheduler.data.entity.ExecutionEntity
import com.example.wascheduler.data.entity.RuleDateEntity
import com.example.wascheduler.data.entity.RuleEntity
import com.example.wascheduler.data.entity.RuleTimeEntity
import com.example.wascheduler.domain.model.ErrorCode
import com.example.wascheduler.domain.model.Execution
import com.example.wascheduler.domain.model.ExecutionStatus
import com.example.wascheduler.domain.model.Rule
import com.example.wascheduler.domain.model.ScheduleType
import com.example.wascheduler.domain.model.RuleTime
import java.time.DayOfWeek

fun RuleEntity.toDomain(times: List<RuleTimeEntity>, dates: List<RuleDateEntity>): Rule = Rule(
    id = id,
    name = name,
    chatName = chatName,
    message = message,
    enabled = enabled,
    scheduleType = runCatching { ScheduleType.valueOf(scheduleType) }.getOrDefault(ScheduleType.WEEKLY),
    startDate = startDate,
    dates = dates.map { it.localDate }.distinct().sorted(),
    allowedDelayMinutes = allowedDelayMinutes,
    times = times.map { it.toDomain() },
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Rule.toEntity(): RuleEntity = RuleEntity(
    id = id,
    name = name,
    chatName = chatName,
    message = message,
    enabled = enabled,
    scheduleType = scheduleType.name,
    startDate = startDate,
    allowedDelayMinutes = allowedDelayMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun RuleTimeEntity.toDomain(): RuleTime = RuleTime(
    id = id,
    ruleId = ruleId,
    localTime = localTime,
    localDate = localDate,
    days = buildSet {
        if (monday) add(DayOfWeek.MONDAY)
        if (tuesday) add(DayOfWeek.TUESDAY)
        if (wednesday) add(DayOfWeek.WEDNESDAY)
        if (thursday) add(DayOfWeek.THURSDAY)
        if (friday) add(DayOfWeek.FRIDAY)
        if (saturday) add(DayOfWeek.SATURDAY)
        if (sunday) add(DayOfWeek.SUNDAY)
    },
    enabled = enabled
)


fun RuleTime.toEntity(): RuleTimeEntity = RuleTimeEntity(
    id = id,
    ruleId = ruleId,
    localTime = localTime,
    localDate = localDate,
    monday = DayOfWeek.MONDAY in days,
    tuesday = DayOfWeek.TUESDAY in days,
    wednesday = DayOfWeek.WEDNESDAY in days,
    thursday = DayOfWeek.THURSDAY in days,
    friday = DayOfWeek.FRIDAY in days,
    saturday = DayOfWeek.SATURDAY in days,
    sunday = DayOfWeek.SUNDAY in days,
    enabled = enabled
)

fun ExecutionEntity.toDomain(): Execution = Execution(
    id = id,
    occurrenceId = occurrenceId,
    ruleId = ruleId,
    scheduledAt = scheduledAt,
    startedAt = startedAt,
    finishedAt = finishedAt,
    status = ExecutionStatus.valueOf(status),
    attemptNumber = attemptNumber,
    targetChat = targetChat,
    messagePreview = messagePreview,
    errorCode = errorCode?.let { runCatching { ErrorCode.valueOf(it) }.getOrNull() },
    errorMessage = errorMessage
)

fun Execution.toEntity(): ExecutionEntity = ExecutionEntity(
    id = id,
    occurrenceId = occurrenceId,
    ruleId = ruleId,
    scheduledAt = scheduledAt,
    startedAt = startedAt,
    finishedAt = finishedAt,
    status = status.name,
    attemptNumber = attemptNumber,
    targetChat = targetChat,
    messagePreview = messagePreview,
    errorCode = errorCode?.name,
    errorMessage = errorMessage
)
