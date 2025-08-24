package com.example.geekdiary.domain.model

import java.time.LocalDateTime

data class Asset(
    val id: String,
    val filename: String,
    val localFilePath: String? = null,
    val diaryEntryId: String,
    val isDownloaded: Boolean = false,
    val downloadStatus: AssetDownloadStatus = AssetDownloadStatus.PENDING,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class AssetDownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class AssetUpload(
    val localFilePath: String,
    val diaryEntryId: String,
    val uploadStatus: AssetUploadStatus = AssetUploadStatus.PENDING,
    val retryCount: Int = 0,
    val errorMessage: String? = null
)

enum class AssetUploadStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED
}
