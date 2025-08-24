package com.example.geekdiary.data.sync

import com.example.geekdiary.data.local.SettingsManager
import com.example.geekdiary.data.local.dao.PendingAssetDownloadDao
import com.example.geekdiary.data.local.dao.PendingAssetUploadDao
import com.example.geekdiary.data.local.entity.PendingAssetDownloadEntity
import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.data.util.MarkdownAssetParser
import com.example.geekdiary.domain.model.DiaryEntry
import com.example.geekdiary.domain.repository.AssetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetSyncManager @Inject constructor(
    private val assetRepository: AssetRepository,
    private val pendingAssetDownloadDao: PendingAssetDownloadDao,
    private val pendingAssetUploadDao: PendingAssetUploadDao,
    private val markdownAssetParser: MarkdownAssetParser,
    private val settingsManager: SettingsManager
) {
    
    /**
     * Process diary entry for asset synchronization
     * Extracts assets from markdown content and queues them for download
     * @param diaryEntry The diary entry to process
     */
    suspend fun processDiaryEntryAssets(diaryEntry: DiaryEntry) = withContext(Dispatchers.IO) {
        try {
            val baseUrl = settingsManager.getServerUrl()
            
            // Extract asset filenames from markdown content
            val assetFilenames = markdownAssetParser.extractAssetFilenames(diaryEntry.body)
            
            for (filename in assetFilenames) {
                // Check if asset is already available locally
                if (!assetRepository.isAssetAvailableLocally(filename)) {
                    // Check if it's already queued for download
                    val existingPendingDownload = pendingAssetDownloadDao.getPendingDownloadByFilename(filename)
                    
                    if (existingPendingDownload == null) {
                        // Queue for download
                        val pendingDownload = PendingAssetDownloadEntity(
                            id = UUID.randomUUID().toString(),
                            filename = filename,
                            diaryEntryId = diaryEntry.date.toString(), // Using date as entry ID
                            downloadStatus = "PENDING"
                        )
                        pendingAssetDownloadDao.insertPendingDownload(pendingDownload)
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't fail the sync
            println("Error processing diary entry assets: ${e.message}")
        }
    }
    
    /**
     * Process multiple diary entries for asset synchronization
     * @param diaryEntries List of diary entries to process
     */
    suspend fun processDiaryEntriesAssets(diaryEntries: List<DiaryEntry>) = withContext(Dispatchers.IO) {
        for (diaryEntry in diaryEntries) {
            processDiaryEntryAssets(diaryEntry)
        }
    }
    
    /**
     * Download pending assets in background
     * @param maxConcurrentDownloads Maximum number of concurrent downloads
     * @return Number of successfully downloaded assets
     */
    suspend fun downloadPendingAssets(maxConcurrentDownloads: Int = 3): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        
        try {
            val pendingDownloads = pendingAssetDownloadDao.getPendingDownloads()
                .take(maxConcurrentDownloads)
            
            for (pendingDownload in pendingDownloads) {
                // Update status to downloading
                pendingAssetDownloadDao.updateDownloadStatus(
                    pendingDownload.id, 
                    "DOWNLOADING"
                )
                
                // Attempt to download
                when (val result = assetRepository.downloadAsset(
                    pendingDownload.filename, 
                    pendingDownload.diaryEntryId
                )) {
                    is NetworkResult.Success -> {
                        // Mark as completed and remove from pending
                        pendingAssetDownloadDao.updateDownloadStatus(
                            pendingDownload.id, 
                            "COMPLETED"
                        )
                        pendingAssetDownloadDao.deletePendingDownload(pendingDownload)
                        successCount++
                    }
                    is NetworkResult.Error -> {
                        // Mark as failed and increment retry count
                        val newRetryCount = pendingDownload.retryCount + 1
                        pendingAssetDownloadDao.updateDownloadFailure(
                            pendingDownload.id,
                            "FAILED",
                            newRetryCount,
                            result.exception.message
                        )
                        
                        // Remove from pending if max retries reached
                        if (newRetryCount >= 3) {
                            pendingAssetDownloadDao.deletePendingDownload(pendingDownload)
                        }
                    }
                    is NetworkResult.Loading -> {
                        // Keep as downloading
                    }
                }
            }
        } catch (e: Exception) {
            println("Error downloading pending assets: ${e.message}")
        }
        
        successCount
    }
    
    /**
     * Retry failed asset downloads
     * @return Number of successfully retried downloads
     */
    suspend fun retryFailedDownloads(): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        
        try {
            val failedDownloads = pendingAssetDownloadDao.getFailedDownloadsForRetry()
            
            for (failedDownload in failedDownloads) {
                // Update status to downloading
                pendingAssetDownloadDao.updateDownloadStatus(
                    failedDownload.id, 
                    "DOWNLOADING"
                )
                
                // Attempt to download
                when (val result = assetRepository.downloadAsset(
                    failedDownload.filename, 
                    failedDownload.diaryEntryId
                )) {
                    is NetworkResult.Success -> {
                        // Mark as completed and remove from pending
                        pendingAssetDownloadDao.updateDownloadStatus(
                            failedDownload.id, 
                            "COMPLETED"
                        )
                        pendingAssetDownloadDao.deletePendingDownload(failedDownload)
                        successCount++
                    }
                    is NetworkResult.Error -> {
                        // Mark as failed and increment retry count
                        val newRetryCount = failedDownload.retryCount + 1
                        pendingAssetDownloadDao.updateDownloadFailure(
                            failedDownload.id,
                            "FAILED",
                            newRetryCount,
                            result.exception.message
                        )
                        
                        // Remove from pending if max retries reached
                        if (newRetryCount >= 3) {
                            pendingAssetDownloadDao.deletePendingDownload(failedDownload)
                        }
                    }
                    is NetworkResult.Loading -> {
                        // Keep as downloading
                    }
                }
            }
        } catch (e: Exception) {
            println("Error retrying failed downloads: ${e.message}")
        }
        
        successCount
    }
    
    /**
     * Get pending download statistics
     * @return Triple of (pending count, downloading count, failed count)
     */
    suspend fun getPendingDownloadStats(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        try {
            val pendingDownloads = pendingAssetDownloadDao.getPendingDownloads()
            val pendingCount = pendingDownloads.count { it.downloadStatus == "PENDING" }
            val downloadingCount = pendingDownloads.count { it.downloadStatus == "DOWNLOADING" }
            val failedCount = pendingDownloads.count { it.downloadStatus == "FAILED" }
            
            Triple(pendingCount, downloadingCount, failedCount)
        } catch (e: Exception) {
            Triple(0, 0, 0)
        }
    }
    
    /**
     * Clean up completed downloads from pending table
     */
    suspend fun cleanupCompletedDownloads() = withContext(Dispatchers.IO) {
        try {
            pendingAssetDownloadDao.deleteCompletedDownloads()
        } catch (e: Exception) {
            println("Error cleaning up completed downloads: ${e.message}")
        }
    }
    
    /**
     * Process asset cleanup when diary entry is deleted
     * @param diaryEntryId The ID of the deleted diary entry
     */
    suspend fun cleanupAssetsForDeletedEntry(diaryEntryId: String) = withContext(Dispatchers.IO) {
        try {
            // Delete assets associated with the diary entry
            assetRepository.deleteAssetsByDiaryEntryId(diaryEntryId)
            
            // Remove pending downloads for this entry
            pendingAssetDownloadDao.deletePendingDownloadsByDiaryEntryId(diaryEntryId)
            
            // Remove pending uploads for this entry
            pendingAssetUploadDao.deletePendingUploadsByDiaryEntryId(diaryEntryId)
        } catch (e: Exception) {
            println("Error cleaning up assets for deleted entry: ${e.message}")
        }
    }
    
    /**
     * Process asset changes when diary entry is updated
     * @param oldDiaryEntry The old diary entry
     * @param newDiaryEntry The new diary entry
     */
    suspend fun processAssetChangesForUpdatedEntry(
        oldDiaryEntry: DiaryEntry, 
        newDiaryEntry: DiaryEntry
    ) = withContext(Dispatchers.IO) {
        try {
            val oldAssets = markdownAssetParser.extractAssetFilenames(oldDiaryEntry.body).toSet()
            val newAssets = markdownAssetParser.extractAssetFilenames(newDiaryEntry.body).toSet()
            
            // Assets that were removed
            val removedAssets = oldAssets - newAssets
            for (filename in removedAssets) {
                assetRepository.deleteAsset(filename)
            }
            
            // Assets that were added
            val addedAssets = newAssets - oldAssets
            for (filename in addedAssets) {
                if (!assetRepository.isAssetAvailableLocally(filename)) {
                    val pendingDownload = PendingAssetDownloadEntity(
                        id = UUID.randomUUID().toString(),
                        filename = filename,
                        diaryEntryId = newDiaryEntry.date.toString(),
                        downloadStatus = "PENDING"
                    )
                    pendingAssetDownloadDao.insertPendingDownload(pendingDownload)
                }
            }
        } catch (e: Exception) {
            println("Error processing asset changes for updated entry: ${e.message}")
        }
    }
}
