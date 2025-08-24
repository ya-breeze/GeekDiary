package com.example.geekdiary.data.sync

import com.example.geekdiary.data.local.AssetStorageManager
import com.example.geekdiary.data.local.dao.AssetDao
import com.example.geekdiary.data.local.dao.DiaryEntryDao
import com.example.geekdiary.data.local.dao.PendingAssetDownloadDao
import com.example.geekdiary.data.local.dao.PendingAssetUploadDao
import com.example.geekdiary.data.util.MarkdownAssetParser
import com.example.geekdiary.domain.repository.AssetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetCleanupManager @Inject constructor(
    private val assetDao: AssetDao,
    private val assetRepository: AssetRepository,
    private val assetStorageManager: AssetStorageManager,
    private val diaryEntryDao: DiaryEntryDao,
    private val pendingAssetDownloadDao: PendingAssetDownloadDao,
    private val pendingAssetUploadDao: PendingAssetUploadDao,
    private val markdownAssetParser: MarkdownAssetParser
) {
    
    /**
     * Clean up assets when a diary entry is deleted
     * @param diaryEntryId The ID of the deleted diary entry
     */
    suspend fun cleanupAssetsForDeletedEntry(diaryEntryId: String) = withContext(Dispatchers.IO) {
        try {
            // Get all assets associated with this diary entry
            val assets = assetDao.getAssetsByDiaryEntryId(diaryEntryId)
            
            // Delete each asset (local file and database record)
            for (asset in assets) {
                if (asset.localFilePath != null) {
                    assetStorageManager.deleteAssetByPath(asset.localFilePath)
                }
                assetDao.deleteAsset(asset)
            }
            
            // Clean up pending downloads for this entry
            pendingAssetDownloadDao.deletePendingDownloadsByDiaryEntryId(diaryEntryId)
            
            // Clean up pending uploads for this entry
            pendingAssetUploadDao.deletePendingUploadsByDiaryEntryId(diaryEntryId)
            
            println("Cleaned up ${assets.size} assets for deleted entry: $diaryEntryId")
        } catch (e: Exception) {
            println("Error cleaning up assets for deleted entry $diaryEntryId: ${e.message}")
        }
    }
    
    /**
     * Clean up assets when a diary entry is updated
     * @param diaryEntryId The ID of the updated diary entry
     * @param oldContent The old markdown content
     * @param newContent The new markdown content
     */
    suspend fun cleanupAssetsForUpdatedEntry(
        diaryEntryId: String,
        oldContent: String,
        newContent: String
    ) = withContext(Dispatchers.IO) {
        try {
            val oldAssets = markdownAssetParser.extractAssetFilenames(oldContent).toSet()
            val newAssets = markdownAssetParser.extractAssetFilenames(newContent).toSet()
            
            // Assets that were removed from the content
            val removedAssets = oldAssets - newAssets
            
            // Delete removed assets
            for (filename in removedAssets) {
                val asset = assetDao.getAssetByFilename(filename)
                if (asset != null && asset.diaryEntryId == diaryEntryId) {
                    // Delete local file
                    if (asset.localFilePath != null) {
                        assetStorageManager.deleteAssetByPath(asset.localFilePath)
                    }
                    // Delete database record
                    assetDao.deleteAsset(asset)
                    
                    // Clean up pending downloads for this asset
                    pendingAssetDownloadDao.deletePendingDownloadByFilename(filename)
                }
            }
            
            println("Cleaned up ${removedAssets.size} removed assets for updated entry: $diaryEntryId")
        } catch (e: Exception) {
            println("Error cleaning up assets for updated entry $diaryEntryId: ${e.message}")
        }
    }
    
    /**
     * Find and clean up orphaned assets
     * Assets are considered orphaned if they exist in storage but are not referenced by any diary entry
     * @return Number of orphaned assets cleaned up
     */
    suspend fun cleanupOrphanedAssets(): Int = withContext(Dispatchers.IO) {
        var cleanedCount = 0
        
        try {
            // Get all assets from database
            val allAssets = assetDao.getPendingDownloads() // This gets all assets, not just pending
            
            for (asset in allAssets) {
                // Check if the diary entry still exists
                val diaryEntry = diaryEntryDao.getEntryByDate(
                    asset.diaryEntryId.substringBefore("_"), // Extract user ID if needed
                    asset.diaryEntryId
                )
                
                if (diaryEntry == null) {
                    // Diary entry doesn't exist, this asset is orphaned
                    if (asset.localFilePath != null) {
                        assetStorageManager.deleteAssetByPath(asset.localFilePath)
                    }
                    assetDao.deleteAsset(asset)
                    cleanedCount++
                } else {
                    // Check if the asset is still referenced in the diary entry content
                    val referencedAssets = markdownAssetParser.extractAssetFilenames(diaryEntry.body)
                    if (!referencedAssets.contains(asset.filename)) {
                        // Asset is not referenced in content, it's orphaned
                        if (asset.localFilePath != null) {
                            assetStorageManager.deleteAssetByPath(asset.localFilePath)
                        }
                        assetDao.deleteAsset(asset)
                        cleanedCount++
                    }
                }
            }
            
            println("Cleaned up $cleanedCount orphaned assets")
        } catch (e: Exception) {
            println("Error cleaning up orphaned assets: ${e.message}")
        }
        
        cleanedCount
    }
    
    /**
     * Clean up failed downloads that have exceeded retry limits
     * @return Number of failed downloads cleaned up
     */
    suspend fun cleanupFailedDownloads(): Int = withContext(Dispatchers.IO) {
        var cleanedCount = 0
        
        try {
            // Get all pending downloads
            val pendingDownloads = pendingAssetDownloadDao.getPendingDownloads()
            
            for (pendingDownload in pendingDownloads) {
                // Remove downloads that have failed too many times
                if (pendingDownload.downloadStatus == "FAILED" && pendingDownload.retryCount >= 3) {
                    pendingAssetDownloadDao.deletePendingDownload(pendingDownload)
                    cleanedCount++
                }
            }
            
            // Also clean up completed downloads
            pendingAssetDownloadDao.deleteCompletedDownloads()
            
            println("Cleaned up $cleanedCount failed downloads")
        } catch (e: Exception) {
            println("Error cleaning up failed downloads: ${e.message}")
        }
        
        cleanedCount
    }
    
    /**
     * Clean up failed uploads that have exceeded retry limits
     * @return Number of failed uploads cleaned up
     */
    suspend fun cleanupFailedUploads(): Int = withContext(Dispatchers.IO) {
        var cleanedCount = 0
        
        try {
            // Get all pending uploads
            val pendingUploads = pendingAssetUploadDao.getPendingUploads()
            
            for (pendingUpload in pendingUploads) {
                // Remove uploads that have failed too many times
                if (pendingUpload.uploadStatus == "FAILED" && pendingUpload.retryCount >= 3) {
                    pendingAssetUploadDao.deletePendingUpload(pendingUpload)
                    cleanedCount++
                }
            }
            
            // Also clean up completed uploads
            pendingAssetUploadDao.deleteCompletedUploads()
            
            println("Cleaned up $cleanedCount failed uploads")
        } catch (e: Exception) {
            println("Error cleaning up failed uploads: ${e.message}")
        }
        
        cleanedCount
    }
    
    /**
     * Perform comprehensive asset cleanup
     * This includes orphaned assets, failed downloads, and failed uploads
     * @return Triple of (orphaned assets, failed downloads, failed uploads) cleaned up
     */
    suspend fun performComprehensiveCleanup(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        val orphanedCount = cleanupOrphanedAssets()
        val failedDownloadsCount = cleanupFailedDownloads()
        val failedUploadsCount = cleanupFailedUploads()
        
        println("Comprehensive cleanup completed: $orphanedCount orphaned, $failedDownloadsCount failed downloads, $failedUploadsCount failed uploads")
        
        Triple(orphanedCount, failedDownloadsCount, failedUploadsCount)
    }
    
    /**
     * Get cleanup statistics
     * @return Map of cleanup statistics
     */
    suspend fun getCleanupStats(): Map<String, Int> = withContext(Dispatchers.IO) {
        try {
            val totalAssets = assetDao.getTotalPendingDownloadCount()
            val pendingDownloads = pendingAssetDownloadDao.getActiveDownloadCount()
            val pendingUploads = pendingAssetUploadDao.getActiveUploadCount()
            val failedDownloads = pendingAssetDownloadDao.getFailedDownloadsForRetry().size
            val failedUploads = pendingAssetUploadDao.getFailedUploadsForRetry().size
            
            mapOf(
                "totalAssets" to totalAssets,
                "pendingDownloads" to pendingDownloads,
                "pendingUploads" to pendingUploads,
                "failedDownloads" to failedDownloads,
                "failedUploads" to failedUploads
            )
        } catch (e: Exception) {
            println("Error getting cleanup stats: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * Schedule periodic cleanup
     * This should be called periodically to maintain asset hygiene
     */
    suspend fun schedulePeriodicCleanup() = withContext(Dispatchers.IO) {
        try {
            // Clean up completed operations
            pendingAssetDownloadDao.deleteCompletedDownloads()
            pendingAssetUploadDao.deleteCompletedUploads()
            
            // Clean up old failed operations (older than 24 hours)
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            
            // Note: This would require additional DAO methods to filter by timestamp
            // For now, we'll just clean up based on retry count
            cleanupFailedDownloads()
            cleanupFailedUploads()
            
            println("Periodic cleanup completed")
        } catch (e: Exception) {
            println("Error during periodic cleanup: ${e.message}")
        }
    }
}
