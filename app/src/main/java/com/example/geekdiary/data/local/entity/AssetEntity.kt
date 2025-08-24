package com.example.geekdiary.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "assets",
    indices = [
        Index(value = ["diaryEntryId"]),
        Index(value = ["filename"], unique = true)
    ]
)
data class AssetEntity(
    @PrimaryKey
    val id: String,
    val filename: String,
    val localFilePath: String? = null,
    val diaryEntryId: String,
    val isDownloaded: Boolean = false,
    val downloadStatus: String = "PENDING", // PENDING, DOWNLOADING, COMPLETED, FAILED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "pending_asset_downloads",
    indices = [
        Index(value = ["diaryEntryId"]),
        Index(value = ["filename"])
    ]
)
data class PendingAssetDownloadEntity(
    @PrimaryKey
    val id: String,
    val filename: String,
    val diaryEntryId: String,
    val downloadStatus: String = "PENDING", // PENDING, DOWNLOADING, COMPLETED, FAILED
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "pending_asset_uploads",
    indices = [
        Index(value = ["diaryEntryId"])
    ]
)
data class PendingAssetUploadEntity(
    @PrimaryKey
    val id: String,
    val localFilePath: String,
    val diaryEntryId: String,
    val uploadStatus: String = "PENDING", // PENDING, UPLOADING, COMPLETED, FAILED
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val backendFilename: String? = null, // Set after successful upload
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
