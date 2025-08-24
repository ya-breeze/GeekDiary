package com.example.geekdiary.data.mapper

import com.example.geekdiary.data.remote.dto.SyncChangeResponseDto
import com.example.geekdiary.data.remote.dto.SyncResponseDto
import com.example.geekdiary.domain.model.OperationType
import com.example.geekdiary.domain.model.SyncChange
import com.example.geekdiary.domain.model.SyncResult
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

fun SyncChangeResponseDto.toDomain(): SyncChange {
    return SyncChange(
        id = id,
        userId = userId,
        date = LocalDate.parse(date, dateFormatter),
        operationType = when (operationType.lowercase()) {
            "created" -> OperationType.CREATED
            "updated" -> OperationType.UPDATED
            "deleted" -> OperationType.DELETED
            else -> OperationType.UPDATED
        },
        timestamp = LocalDateTime.parse(timestamp, dateTimeFormatter),
        itemSnapshot = itemSnapshot?.toDomain(),
        metadata = metadata
    )
}

fun SyncResponseDto.toDomain(): SyncResult {
    return SyncResult(
        changes = changes.map { it.toDomain() },
        hasMore = hasMore,
        nextId = nextId
    )
}
