package com.example.geekdiary.data.mapper

import com.example.geekdiary.data.local.entity.AssetEntity
import com.example.geekdiary.data.local.entity.PendingAssetDownloadEntity
import com.example.geekdiary.data.local.entity.PendingAssetUploadEntity
import com.example.geekdiary.domain.model.Asset
import com.example.geekdiary.domain.model.AssetDownloadStatus
import com.example.geekdiary.domain.model.AssetUpload
import com.example.geekdiary.domain.model.AssetUploadStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// Asset Entity Mappers
fun AssetEntity.toDomain(): Asset {
    return Asset(
        id = id,
        filename = filename,
        localFilePath = localFilePath,
        diaryEntryId = diaryEntryId,
        isDownloaded = isDownloaded,
        downloadStatus = when (downloadStatus.uppercase()) {
            "PENDING" -> AssetDownloadStatus.PENDING
            "DOWNLOADING" -> AssetDownloadStatus.DOWNLOADING
            "COMPLETED" -> AssetDownloadStatus.COMPLETED
            "FAILED" -> AssetDownloadStatus.FAILED
            else -> AssetDownloadStatus.PENDING
        },
        createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault()),
        updatedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAt), ZoneId.systemDefault())
    )
}

fun Asset.toEntity(): AssetEntity {
    return AssetEntity(
        id = id,
        filename = filename,
        localFilePath = localFilePath,
        diaryEntryId = diaryEntryId,
        isDownloaded = isDownloaded,
        downloadStatus = downloadStatus.name,
        createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updatedAt = updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
}

// Pending Asset Download Entity Mappers
fun PendingAssetDownloadEntity.toDomain(): Asset {
    return Asset(
        id = id,
        filename = filename,
        localFilePath = null,
        diaryEntryId = diaryEntryId,
        isDownloaded = false,
        downloadStatus = when (downloadStatus.uppercase()) {
            "PENDING" -> AssetDownloadStatus.PENDING
            "DOWNLOADING" -> AssetDownloadStatus.DOWNLOADING
            "COMPLETED" -> AssetDownloadStatus.COMPLETED
            "FAILED" -> AssetDownloadStatus.FAILED
            else -> AssetDownloadStatus.PENDING
        },
        createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault()),
        updatedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAt), ZoneId.systemDefault())
    )
}

// Pending Asset Upload Entity Mappers
fun PendingAssetUploadEntity.toDomain(): AssetUpload {
    return AssetUpload(
        localFilePath = localFilePath,
        diaryEntryId = diaryEntryId,
        uploadStatus = when (uploadStatus.uppercase()) {
            "PENDING" -> AssetUploadStatus.PENDING
            "UPLOADING" -> AssetUploadStatus.UPLOADING
            "COMPLETED" -> AssetUploadStatus.COMPLETED
            "FAILED" -> AssetUploadStatus.FAILED
            else -> AssetUploadStatus.PENDING
        },
        retryCount = retryCount,
        errorMessage = errorMessage
    )
}

fun AssetUpload.toEntity(id: String): PendingAssetUploadEntity {
    return PendingAssetUploadEntity(
        id = id,
        localFilePath = localFilePath,
        diaryEntryId = diaryEntryId,
        uploadStatus = uploadStatus.name,
        retryCount = retryCount,
        errorMessage = errorMessage,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
