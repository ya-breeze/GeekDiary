package com.example.geekdiary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_entries",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "date"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["needsSync"])
    ]
)
data class DiaryEntryEntity(
    @PrimaryKey
    val id: String, // Generated UUID
    val userId: String,
    val date: String, // ISO date string (YYYY-MM-DD)
    val title: String,
    val body: String,
    val tags: String, // JSON array as string
    val previousDate: String? = null,
    val nextDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val needsSync: Boolean = false,
    val lastSyncId: Int? = null
)
