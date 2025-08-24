package com.example.geekdiary.data.sync

import com.example.geekdiary.data.local.dao.PendingAssetDownloadDao
import com.example.geekdiary.data.local.dao.PendingAssetUploadDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetRetryManager @Inject constructor(
    private val assetSyncManager: AssetSyncManager,
    private val assetUploadManager: AssetUploadManager,
    private val pendingAssetDownloadDao: PendingAssetDownloadDao,
    private val pendingAssetUploadDao: PendingAssetUploadDao
) {
    
    companion object {
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_BASE = 1000L // 1 second
        private const val RETRY_DELAY_MULTIPLIER = 2L // Exponential backoff
    }
    
    /**
     * Perform intelligent retry of failed operations
     * Uses exponential backoff and respects retry limits
     * @return Pair of (successful downloads, successful uploads)
     */
    suspend fun performIntelligentRetry(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var successfulDownloads = 0
        var successfulUploads = 0
        
        try {
            // Retry failed downloads with exponential backoff
            val failedDownloads = pendingAssetDownloadDao.getFailedDownloadsForRetry()
            for (failedDownload in failedDownloads) {
                if (failedDownload.retryCount < MAX_RETRY_COUNT) {
                    // Calculate delay based on retry count
                    val delay = RETRY_DELAY_BASE * (RETRY_DELAY_MULTIPLIER * failedDownload.retryCount)
                    delay(delay)
                    
                    // Attempt retry
                    val result = assetSyncManager.downloadPendingAssets(maxConcurrentDownloads = 1)
                    if (result > 0) {
                        successfulDownloads += result
                    }
                }
            }
            
            // Retry failed uploads with exponential backoff
            val failedUploads = pendingAssetUploadDao.getFailedUploadsForRetry()
            for (failedUpload in failedUploads) {
                if (failedUpload.retryCount < MAX_RETRY_COUNT) {
                    // Calculate delay based on retry count
                    val delay = RETRY_DELAY_BASE * (RETRY_DELAY_MULTIPLIER * failedUpload.retryCount)
                    delay(delay)
                    
                    // Attempt retry
                    val result = assetUploadManager.processPendingUploads(maxConcurrentUploads = 1)
                    if (result > 0) {
                        successfulUploads += result
                    }
                }
            }
        } catch (e: Exception) {
            println("Error during intelligent retry: ${e.message}")
        }
        
        Pair(successfulDownloads, successfulUploads)
    }
    
    /**
     * Get retry statistics
     * @return Map containing retry statistics
     */
    suspend fun getRetryStats(): Map<String, Int> = withContext(Dispatchers.IO) {
        try {
            val failedDownloads = pendingAssetDownloadDao.getFailedDownloadsForRetry()
            val failedUploads = pendingAssetUploadDao.getFailedUploadsForRetry()
            
            val downloadRetryStats = failedDownloads.groupBy { it.retryCount }
                .mapValues { it.value.size }
            
            val uploadRetryStats = failedUploads.groupBy { it.retryCount }
                .mapValues { it.value.size }
            
            mapOf(
                "failedDownloads" to failedDownloads.size,
                "failedUploads" to failedUploads.size,
                "downloadsRetry1" to (downloadRetryStats[1] ?: 0),
                "downloadsRetry2" to (downloadRetryStats[2] ?: 0),
                "downloadsRetry3" to (downloadRetryStats[3] ?: 0),
                "uploadsRetry1" to (uploadRetryStats[1] ?: 0),
                "uploadsRetry2" to (uploadRetryStats[2] ?: 0),
                "uploadsRetry3" to (uploadRetryStats[3] ?: 0)
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Reset retry count for a specific download
     * @param downloadId The download ID to reset
     */
    suspend fun resetDownloadRetryCount(downloadId: String) = withContext(Dispatchers.IO) {
        try {
            pendingAssetDownloadDao.updateDownloadFailure(
                downloadId,
                "PENDING",
                0,
                null
            )
        } catch (e: Exception) {
            println("Error resetting download retry count: ${e.message}")
        }
    }
    
    /**
     * Reset retry count for a specific upload
     * @param uploadId The upload ID to reset
     */
    suspend fun resetUploadRetryCount(uploadId: String) = withContext(Dispatchers.IO) {
        try {
            pendingAssetUploadDao.updateUploadFailure(
                uploadId,
                "PENDING",
                0,
                null
            )
        } catch (e: Exception) {
            println("Error resetting upload retry count: ${e.message}")
        }
    }
    
    /**
     * Clean up operations that have exceeded maximum retry attempts
     * @return Number of operations cleaned up
     */
    suspend fun cleanupExceededRetries(): Int = withContext(Dispatchers.IO) {
        var cleanedCount = 0
        
        try {
            // Clean up downloads that exceeded retry limit
            val allPendingDownloads = pendingAssetDownloadDao.getPendingDownloads()
            val exceededDownloads = allPendingDownloads.filter { 
                it.downloadStatus == "FAILED" && it.retryCount >= MAX_RETRY_COUNT 
            }
            
            for (exceededDownload in exceededDownloads) {
                pendingAssetDownloadDao.deletePendingDownload(exceededDownload)
                cleanedCount++
            }
            
            // Clean up uploads that exceeded retry limit
            val allPendingUploads = pendingAssetUploadDao.getPendingUploads()
            val exceededUploads = allPendingUploads.filter { 
                it.uploadStatus == "FAILED" && it.retryCount >= MAX_RETRY_COUNT 
            }
            
            for (exceededUpload in exceededUploads) {
                pendingAssetUploadDao.deletePendingUpload(exceededUpload)
                cleanedCount++
            }
        } catch (e: Exception) {
            println("Error cleaning up exceeded retries: ${e.message}")
        }
        
        cleanedCount
    }
    
    /**
     * Check if an operation should be retried based on error type
     * @param errorMessage The error message from the failed operation
     * @param retryCount Current retry count
     * @return True if the operation should be retried
     */
    fun shouldRetryOperation(errorMessage: String?, retryCount: Int): Boolean {
        if (retryCount >= MAX_RETRY_COUNT) {
            return false
        }
        
        // Don't retry certain types of errors
        val nonRetryableErrors = listOf(
            "unauthorized",
            "forbidden",
            "not found",
            "file not found",
            "invalid file",
            "file too large"
        )
        
        val lowerErrorMessage = errorMessage?.lowercase() ?: ""
        return !nonRetryableErrors.any { lowerErrorMessage.contains(it) }
    }
    
    /**
     * Calculate retry delay based on retry count
     * @param retryCount The current retry count
     * @return Delay in milliseconds
     */
    fun calculateRetryDelay(retryCount: Int): Long {
        return RETRY_DELAY_BASE * (RETRY_DELAY_MULTIPLIER * retryCount)
    }
}
