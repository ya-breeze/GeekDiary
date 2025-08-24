package com.example.geekdiary.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class OperationType {
    CREATED, UPDATED, DELETED
}

data class SyncChange(
    val id: Int,
    val userId: String,
    val date: LocalDate,
    val operationType: OperationType,
    val timestamp: LocalDateTime,
    val itemSnapshot: DiaryEntry? = null,
    val metadata: List<String> = emptyList()
)

data class SyncResult(
    val changes: List<SyncChange>,
    val hasMore: Boolean,
    val nextId: Int? = null
)
