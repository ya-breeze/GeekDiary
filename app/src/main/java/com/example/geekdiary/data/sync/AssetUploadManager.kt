package com.example.geekdiary.data.sync

import com.example.geekdiary.data.local.dao.PendingAssetUploadDao
import com.example.geekdiary.data.local.entity.PendingAssetUploadEntity
import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.domain.repository.AssetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetUploadManager @Inject constructor(
    private val assetRepository: AssetRepository,
    private val pendingAssetUploadDao: PendingAssetUploadDao
) {
    
    /**
     * Queue an asset for upload
     * @param localFilePath The local file path of the asset
     * @param diaryEntryId The diary entry ID that will reference this asset
     * @return The ID of the queued upload
     */
    suspend fun queueAssetUpload(localFilePath: String, diaryEntryId: String): String = withContext(Dispatchers.IO) {
        val uploadId = UUID.randomUUID().toString()
        
        val pendingUpload = PendingAssetUploadEntity(
            id = uploadId,
            localFilePath = localFilePath,
            diaryEntryId = diaryEntryId,
            uploadStatus = "PENDING"
        )
        
        pendingAssetUploadDao.insertPendingUpload(pendingUpload)
        uploadId
    }
    
    /**
     * Process pending asset uploads
     * @param maxConcurrentUploads Maximum number of concurrent uploads
     * @return Number of successfully uploaded assets
     */
    suspend fun processPendingUploads(maxConcurrentUploads: Int = 2): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        
        try {
            val pendingUploads = pendingAssetUploadDao.getPendingUploads()
                .take(maxConcurrentUploads)
            
            for (pendingUpload in pendingUploads) {
                // Update status to uploading
                pendingAssetUploadDao.updateUploadStatus(
                    pendingUpload.id,
                    "UPLOADING"
                )
                
                // Attempt to upload
                when (val result = assetRepository.uploadAsset(
                    pendingUpload.localFilePath,
                    pendingUpload.diaryEntryId
                )) {
                    is NetworkResult.Success -> {
                        // Mark as completed with backend filename
                        pendingAssetUploadDao.updateUploadComplete(
                            pendingUpload.id,
                            "COMPLETED",
                            result.data
                        )
                        successCount++
                    }
                    is NetworkResult.Error -> {
                        // Mark as failed and increment retry count
                        val newRetryCount = pendingUpload.retryCount + 1
                        pendingAssetUploadDao.updateUploadFailure(
                            pendingUpload.id,
                            "FAILED",
                            newRetryCount,
                            result.exception.message
                        )
                        
                        // Remove from pending if max retries reached
                        if (newRetryCount >= 3) {
                            pendingAssetUploadDao.deletePendingUpload(pendingUpload)
                        }
                    }
                    is NetworkResult.Loading -> {
                        // Keep as uploading
                    }
                }
            }
        } catch (e: Exception) {
            println("Error processing pending uploads: ${e.message}")
        }
        
        successCount
    }
    
    /**
     * Retry failed asset uploads
     * @return Number of successfully retried uploads
     */
    suspend fun retryFailedUploads(): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        
        try {
            val failedUploads = pendingAssetUploadDao.getFailedUploadsForRetry()
            
            for (failedUpload in failedUploads) {
                // Update status to uploading
                pendingAssetUploadDao.updateUploadStatus(
                    failedUpload.id,
                    "UPLOADING"
                )
                
                // Attempt to upload
                when (val result = assetRepository.uploadAsset(
                    failedUpload.localFilePath,
                    failedUpload.diaryEntryId
                )) {
                    is NetworkResult.Success -> {
                        // Mark as completed with backend filename
                        pendingAssetUploadDao.updateUploadComplete(
                            failedUpload.id,
                            "COMPLETED",
                            result.data
                        )
                        successCount++
                    }
                    is NetworkResult.Error -> {
                        // Mark as failed and increment retry count
                        val newRetryCount = failedUpload.retryCount + 1
                        pendingAssetUploadDao.updateUploadFailure(
                            failedUpload.id,
                            "FAILED",
                            newRetryCount,
                            result.exception.message
                        )
                        
                        // Remove from pending if max retries reached
                        if (newRetryCount >= 3) {
                            pendingAssetUploadDao.deletePendingUpload(failedUpload)
                        }
                    }
                    is NetworkResult.Loading -> {
                        // Keep as uploading
                    }
                }
            }
        } catch (e: Exception) {
            println("Error retrying failed uploads: ${e.message}")
        }
        
        successCount
    }
    
    /**
     * Get pending upload statistics
     * @return Triple of (pending count, uploading count, failed count)
     */
    suspend fun getPendingUploadStats(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        try {
            val pendingUploads = pendingAssetUploadDao.getPendingUploads()
            val pendingCount = pendingUploads.count { it.uploadStatus == "PENDING" }
            val uploadingCount = pendingUploads.count { it.uploadStatus == "UPLOADING" }
            val failedCount = pendingUploads.count { it.uploadStatus == "FAILED" }
            
            Triple(pendingCount, uploadingCount, failedCount)
        } catch (e: Exception) {
            Triple(0, 0, 0)
        }
    }
    
    /**
     * Get completed uploads for a diary entry
     * @param diaryEntryId The diary entry ID
     * @return List of backend filenames for completed uploads
     */
    suspend fun getCompletedUploadsForEntry(diaryEntryId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val uploads = pendingAssetUploadDao.getPendingUploadsByDiaryEntryId(diaryEntryId)
            uploads.filter { it.uploadStatus == "COMPLETED" && it.backendFilename != null }
                .mapNotNull { it.backendFilename }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Clean up completed uploads from pending table
     */
    suspend fun cleanupCompletedUploads() = withContext(Dispatchers.IO) {
        try {
            pendingAssetUploadDao.deleteCompletedUploads()
        } catch (e: Exception) {
            println("Error cleaning up completed uploads: ${e.message}")
        }
    }
    
    /**
     * Cancel pending upload
     * @param uploadId The upload ID to cancel
     */
    suspend fun cancelUpload(uploadId: String) = withContext(Dispatchers.IO) {
        try {
            val upload = pendingAssetUploadDao.getPendingUploads()
                .find { it.id == uploadId && it.uploadStatus == "PENDING" }
            
            if (upload != null) {
                pendingAssetUploadDao.deletePendingUpload(upload)
            }
        } catch (e: Exception) {
            println("Error canceling upload $uploadId: ${e.message}")
        }
    }
    
    /**
     * Get upload progress for a diary entry
     * @param diaryEntryId The diary entry ID
     * @return Map of upload status counts
     */
    suspend fun getUploadProgressForEntry(diaryEntryId: String): Map<String, Int> = withContext(Dispatchers.IO) {
        try {
            val uploads = pendingAssetUploadDao.getPendingUploadsByDiaryEntryId(diaryEntryId)
            val statusCounts = uploads.groupBy { it.uploadStatus }
                .mapValues { it.value.size }
            
            mapOf(
                "pending" to (statusCounts["PENDING"] ?: 0),
                "uploading" to (statusCounts["UPLOADING"] ?: 0),
                "completed" to (statusCounts["COMPLETED"] ?: 0),
                "failed" to (statusCounts["FAILED"] ?: 0)
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
