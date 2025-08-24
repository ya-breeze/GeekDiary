package com.example.geekdiary.domain.repository

import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.domain.model.Asset
import com.example.geekdiary.domain.model.AssetUpload
import kotlinx.coroutines.flow.Flow

interface AssetRepository {
    
    /**
     * Get assets for a specific diary entry
     * @param diaryEntryId The ID of the diary entry
     * @return Flow of assets list
     */
    fun getAssetsByDiaryEntryId(diaryEntryId: String): Flow<List<Asset>>
    
    /**
     * Get an asset by filename
     * @param filename The filename of the asset
     * @return The asset if found, null otherwise
     */
    suspend fun getAssetByFilename(filename: String): Asset?
    
    /**
     * Download and cache an asset
     * @param filename The filename of the asset to download
     * @param diaryEntryId The ID of the diary entry that references this asset
     * @return NetworkResult indicating success or failure
     */
    suspend fun downloadAsset(filename: String, diaryEntryId: String): NetworkResult<Asset>
    
    /**
     * Upload an asset to the backend
     * @param localFilePath The local file path of the asset to upload
     * @param diaryEntryId The ID of the diary entry that will reference this asset
     * @return NetworkResult containing the backend filename
     */
    suspend fun uploadAsset(localFilePath: String, diaryEntryId: String): NetworkResult<String>
    
    /**
     * Check if an asset is available locally
     * @param filename The filename of the asset
     * @return True if the asset is available locally
     */
    suspend fun isAssetAvailableLocally(filename: String): Boolean
    
    /**
     * Get the local file path for an asset
     * @param filename The filename of the asset
     * @return The local file path if available, null otherwise
     */
    suspend fun getLocalFilePath(filename: String): String?
    
    /**
     * Delete assets associated with a diary entry
     * @param diaryEntryId The ID of the diary entry
     */
    suspend fun deleteAssetsByDiaryEntryId(diaryEntryId: String)
    
    /**
     * Delete a specific asset
     * @param filename The filename of the asset to delete
     */
    suspend fun deleteAsset(filename: String)
    
    /**
     * Get pending asset downloads
     * @return List of assets that need to be downloaded
     */
    suspend fun getPendingDownloads(): List<Asset>
    
    /**
     * Get failed asset downloads that can be retried
     * @return List of assets that failed to download
     */
    suspend fun getFailedDownloads(): List<Asset>
    
    /**
     * Retry downloading a failed asset
     * @param assetId The ID of the asset to retry
     * @return NetworkResult indicating success or failure
     */
    suspend fun retryAssetDownload(assetId: String): NetworkResult<Asset>
    
    /**
     * Get storage statistics
     * @return Pair of (total size in bytes, number of assets)
     */
    suspend fun getStorageStats(): Pair<Long, Int>
}
