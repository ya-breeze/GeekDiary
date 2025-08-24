package com.example.geekdiary.data.sync

import com.example.geekdiary.data.local.dao.AssetDao
import com.example.geekdiary.data.local.dao.PendingAssetDownloadDao
import com.example.geekdiary.data.local.dao.PendingAssetUploadDao
import com.example.geekdiary.data.local.entity.AssetEntity
import com.example.geekdiary.data.util.MarkdownAssetParser
import com.example.geekdiary.domain.model.DiaryEntry
import com.example.geekdiary.domain.repository.AssetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetReferenceTracker @Inject constructor(
    private val assetDao: AssetDao,
    private val assetRepository: AssetRepository,
    private val pendingAssetDownloadDao: PendingAssetDownloadDao,
    private val pendingAssetUploadDao: PendingAssetUploadDao,
    private val markdownAssetParser: MarkdownAssetParser
) {
    
    /**
     * Track asset references when a diary entry is created or updated
     * @param diaryEntry The diary entry to track assets for
     */
    suspend fun trackAssetReferences(diaryEntry: DiaryEntry) = withContext(Dispatchers.IO) {
        try {
            val entryId = generateEntryId(diaryEntry)
            val assetFilenames = markdownAssetParser.extractAssetFilenames(diaryEntry.body)
            
            // Create asset entities for tracking (even if not downloaded yet)
            for (filename in assetFilenames) {
                val existingAsset = assetDao.getAssetByFilename(filename)
                
                if (existingAsset == null) {
                    // Create new asset entity
                    val assetEntity = AssetEntity(
                        id = UUID.randomUUID().toString(),
                        filename = filename,
                        diaryEntryId = entryId,
                        isDownloaded = false,
                        downloadStatus = "PENDING"
                    )
                    assetDao.insertAsset(assetEntity)
                } else if (existingAsset.diaryEntryId != entryId) {
                    // Update the diary entry reference
                    val updatedAsset = existingAsset.copy(
                        diaryEntryId = entryId,
                        updatedAt = System.currentTimeMillis()
                    )
                    assetDao.updateAsset(updatedAsset)
                }
            }
        } catch (e: Exception) {
            println("Error tracking asset references: ${e.message}")
        }
    }
    
    /**
     * Update asset references when a diary entry is modified
     * @param oldDiaryEntry The old diary entry
     * @param newDiaryEntry The new diary entry
     */
    suspend fun updateAssetReferences(
        oldDiaryEntry: DiaryEntry, 
        newDiaryEntry: DiaryEntry
    ) = withContext(Dispatchers.IO) {
        try {
            val entryId = generateEntryId(newDiaryEntry)
            val oldAssets = markdownAssetParser.extractAssetFilenames(oldDiaryEntry.body).toSet()
            val newAssets = markdownAssetParser.extractAssetFilenames(newDiaryEntry.body).toSet()
            
            // Assets that were removed - delete them since no sharing between entries
            val removedAssets = oldAssets - newAssets
            for (filename in removedAssets) {
                cleanupAsset(filename, entryId)
            }
            
            // Assets that were added - track them
            val addedAssets = newAssets - oldAssets
            for (filename in addedAssets) {
                val existingAsset = assetDao.getAssetByFilename(filename)
                
                if (existingAsset == null) {
                    // Create new asset entity
                    val assetEntity = AssetEntity(
                        id = UUID.randomUUID().toString(),
                        filename = filename,
                        diaryEntryId = entryId,
                        isDownloaded = false,
                        downloadStatus = "PENDING"
                    )
                    assetDao.insertAsset(assetEntity)
                } else {
                    // Update the diary entry reference
                    val updatedAsset = existingAsset.copy(
                        diaryEntryId = entryId,
                        updatedAt = System.currentTimeMillis()
                    )
                    assetDao.updateAsset(updatedAsset)
                }
            }
        } catch (e: Exception) {
            println("Error updating asset references: ${e.message}")
        }
    }
    
    /**
     * Clean up asset references when a diary entry is deleted
     * @param diaryEntry The diary entry that was deleted
     */
    suspend fun cleanupAssetReferences(diaryEntry: DiaryEntry) = withContext(Dispatchers.IO) {
        try {
            val entryId = generateEntryId(diaryEntry)
            val assetFilenames = markdownAssetParser.extractAssetFilenames(diaryEntry.body)
            
            // Delete all assets referenced by this entry
            for (filename in assetFilenames) {
                cleanupAsset(filename, entryId)
            }
            
            // Also clean up any assets directly associated with this entry ID
            assetRepository.deleteAssetsByDiaryEntryId(entryId)
            
            // Clean up pending operations for this entry
            pendingAssetDownloadDao.deletePendingDownloadsByDiaryEntryId(entryId)
            pendingAssetUploadDao.deletePendingUploadsByDiaryEntryId(entryId)
        } catch (e: Exception) {
            println("Error cleaning up asset references: ${e.message}")
        }
    }
    
    /**
     * Clean up asset references by entry ID
     * @param entryId The ID of the diary entry
     */
    suspend fun cleanupAssetReferencesByEntryId(entryId: String) = withContext(Dispatchers.IO) {
        try {
            // Get all assets for this entry
            val assets = assetDao.getAssetsByDiaryEntryId(entryId)
            
            // Delete each asset
            for (asset in assets) {
                cleanupAsset(asset.filename, entryId)
            }
            
            // Clean up pending operations for this entry
            pendingAssetDownloadDao.deletePendingDownloadsByDiaryEntryId(entryId)
            pendingAssetUploadDao.deletePendingUploadsByDiaryEntryId(entryId)
        } catch (e: Exception) {
            println("Error cleaning up asset references by entry ID: ${e.message}")
        }
    }
    
    /**
     * Get all assets referenced by a diary entry
     * @param diaryEntry The diary entry
     * @return List of asset filenames
     */
    suspend fun getAssetReferences(diaryEntry: DiaryEntry): List<String> = withContext(Dispatchers.IO) {
        try {
            markdownAssetParser.extractAssetFilenames(diaryEntry.body)
        } catch (e: Exception) {
            println("Error getting asset references: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Check if an asset is referenced by any diary entry
     * @param filename The asset filename
     * @return True if the asset is referenced
     */
    suspend fun isAssetReferenced(filename: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val asset = assetDao.getAssetByFilename(filename)
            asset != null
        } catch (e: Exception) {
            println("Error checking if asset is referenced: ${e.message}")
            false
        }
    }
    
    /**
     * Get the diary entry ID that references an asset
     * @param filename The asset filename
     * @return The diary entry ID or null if not found
     */
    suspend fun getAssetReferencingEntry(filename: String): String? = withContext(Dispatchers.IO) {
        try {
            val asset = assetDao.getAssetByFilename(filename)
            asset?.diaryEntryId
        } catch (e: Exception) {
            println("Error getting asset referencing entry: ${e.message}")
            null
        }
    }
    
    /**
     * Validate asset references in a diary entry
     * @param diaryEntry The diary entry to validate
     * @return List of missing asset filenames
     */
    suspend fun validateAssetReferences(diaryEntry: DiaryEntry): List<String> = withContext(Dispatchers.IO) {
        try {
            val assetFilenames = markdownAssetParser.extractAssetFilenames(diaryEntry.body)
            val missingAssets = mutableListOf<String>()
            
            for (filename in assetFilenames) {
                if (!assetRepository.isAssetAvailableLocally(filename)) {
                    missingAssets.add(filename)
                }
            }
            
            missingAssets
        } catch (e: Exception) {
            println("Error validating asset references: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Clean up a specific asset
     * @param filename The asset filename
     * @param entryId The diary entry ID
     */
    private suspend fun cleanupAsset(filename: String, entryId: String) {
        try {
            // Delete the asset from repository (handles both local file and database)
            assetRepository.deleteAsset(filename)
            
            // Clean up any pending downloads for this asset
            pendingAssetDownloadDao.deletePendingDownloadByFilename(filename)
        } catch (e: Exception) {
            println("Error cleaning up asset $filename: ${e.message}")
        }
    }
    
    /**
     * Generate a consistent entry ID from a diary entry
     * @param diaryEntry The diary entry
     * @return The entry ID
     */
    private fun generateEntryId(diaryEntry: DiaryEntry): String {
        // Use the date as the entry ID for consistency
        return diaryEntry.date.toString()
    }
}
