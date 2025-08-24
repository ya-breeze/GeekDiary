package com.example.geekdiary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_state",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SyncStateEntity(
    @PrimaryKey
    val userId: String,
    val lastSyncId: Int = 0,
    val lastSyncTimestamp: Long? = null,
    val syncInProgress: Boolean = false
)

@Entity(
    tableName = "pending_changes",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PendingChangeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val entryDate: String, // ISO date string
    val operationType: String, // "create", "update", "delete"
    val data: String, // JSON payload
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val nextRetryAt: Long? = null,
    val errorMessage: String? = null
)
