package com.example.geekdiary.data.repository

import com.example.geekdiary.data.local.AssetStorageManager
import com.example.geekdiary.data.local.dao.AssetDao
import com.example.geekdiary.data.local.dao.UserDao
import com.example.geekdiary.data.mapper.toDomain
import com.example.geekdiary.data.mapper.toEntity
import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.data.remote.api.AssetApiService
import com.example.geekdiary.data.remote.safeApiCall
import com.example.geekdiary.domain.model.Asset
import com.example.geekdiary.domain.model.AssetDownloadStatus
import com.example.geekdiary.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetRepositoryImpl @Inject constructor(
    private val assetApiService: AssetApiService,
    private val assetDao: AssetDao,
    private val assetStorageManager: AssetStorageManager,
    private val userDao: UserDao
) : AssetRepository {
    
    override fun getAssetsByDiaryEntryId(diaryEntryId: String): Flow<List<Asset>> {
        return assetDao.getAssetsByDiaryEntryIdFlow(diaryEntryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getAssetByFilename(filename: String): Asset? {
        return assetDao.getAssetByFilename(filename)?.toDomain()
    }
    
    override suspend fun downloadAsset(filename: String, diaryEntryId: String): NetworkResult<Asset> {
        try {
            // Check if asset already exists locally
            val existingAsset = assetDao.getAssetByFilename(filename)
            if (existingAsset != null && existingAsset.isDownloaded) {
                return NetworkResult.Success(existingAsset.toDomain())
            }
            
            // Create or update asset entity
            val assetId = existingAsset?.id ?: UUID.randomUUID().toString()
            val assetEntity = existingAsset?.copy(
                downloadStatus = AssetDownloadStatus.DOWNLOADING.name,
                updatedAt = System.currentTimeMillis()
            ) ?: com.example.geekdiary.data.local.entity.AssetEntity(
                id = assetId,
                filename = filename,
                diaryEntryId = diaryEntryId,
                downloadStatus = AssetDownloadStatus.DOWNLOADING.name
            )
            
            assetDao.insertAsset(assetEntity)
            
            // Download from backend
            return when (val result = safeApiCall { assetApiService.downloadAsset(filename) }) {
                is NetworkResult.Success -> {
                    val responseBody = result.data.body()
                    if (responseBody != null) {
                        // Save to local storage
                        val localFilePath = assetStorageManager.saveAsset(filename, responseBody)
                        if (localFilePath != null) {
                            // Update asset entity
                            val updatedAsset = assetEntity.copy(
                                localFilePath = localFilePath,
                                isDownloaded = true,
                                downloadStatus = AssetDownloadStatus.COMPLETED.name,
                                updatedAt = System.currentTimeMillis()
                            )
                            assetDao.insertAsset(updatedAsset)
                            NetworkResult.Success(updatedAsset.toDomain())
                        } else {
                            // Failed to save locally
                            assetDao.updateDownloadStatus(assetId, AssetDownloadStatus.FAILED.name)
                            NetworkResult.Error(
                                com.example.geekdiary.data.remote.NetworkException.UnknownError(
                                    Exception("Failed to save asset locally")
                                )
                            )
                        }
                    } else {
                        // Empty response body
                        assetDao.updateDownloadStatus(assetId, AssetDownloadStatus.FAILED.name)
                        NetworkResult.Error(
                            com.example.geekdiary.data.remote.NetworkException.UnknownError(
                                Exception("Empty response body")
                            )
                        )
                    }
                }
                is NetworkResult.Error -> {
                    // Update status to failed
                    assetDao.updateDownloadStatus(assetId, AssetDownloadStatus.FAILED.name)
                    NetworkResult.Error(result.exception)
                }
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        } catch (e: Exception) {
            return NetworkResult.Error(
                com.example.geekdiary.data.remote.NetworkException.UnknownError(e)
            )
        }
    }
    
    override suspend fun uploadAsset(localFilePath: String, diaryEntryId: String): NetworkResult<String> {
        try {
            val file = File(localFilePath)
            if (!file.exists()) {
                return NetworkResult.Error(
                    com.example.geekdiary.data.remote.NetworkException.UnknownError(
                        Exception("Local file does not exist: $localFilePath")
                    )
                )
            }
            
            // Determine media type based on file extension
            val extension = file.extension.lowercase()
            val mediaType = when (extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "mp4" -> "video/mp4"
                "avi" -> "video/avi"
                "mov" -> "video/quicktime"
                else -> "application/octet-stream"
            }.toMediaTypeOrNull()
            
            val requestBody = file.asRequestBody(mediaType)
            val multipartBody = MultipartBody.Part.createFormData("asset", file.name, requestBody)
            
            return when (val result = safeApiCall { assetApiService.uploadAsset(multipartBody) }) {
                is NetworkResult.Success -> {
                    val backendFilename = result.data.filename
                    NetworkResult.Success(backendFilename)
                }
                is NetworkResult.Error -> NetworkResult.Error(result.exception)
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        } catch (e: Exception) {
            return NetworkResult.Error(
                com.example.geekdiary.data.remote.NetworkException.UnknownError(e)
            )
        }
    }
    
    override suspend fun isAssetAvailableLocally(filename: String): Boolean {
        val asset = assetDao.getAssetByFilename(filename)
        return asset?.isDownloaded == true && asset.localFilePath != null &&
                assetStorageManager.assetExists(filename)
    }
    
    override suspend fun getLocalFilePath(filename: String): String? {
        val asset = assetDao.getAssetByFilename(filename)
        return if (asset?.isDownloaded == true && asset.localFilePath != null) {
            asset.localFilePath
        } else {
            null
        }
    }
    
    override suspend fun deleteAssetsByDiaryEntryId(diaryEntryId: String) {
        val assets = assetDao.getAssetsByDiaryEntryId(diaryEntryId)
        for (asset in assets) {
            if (asset.localFilePath != null) {
                assetStorageManager.deleteAssetByPath(asset.localFilePath)
            }
        }
        assetDao.deleteAssetsByDiaryEntryId(diaryEntryId)
    }
    
    override suspend fun deleteAsset(filename: String) {
        assetStorageManager.deleteAsset(filename)
        assetDao.deleteAssetByFilename(filename)
    }
    
    override suspend fun getPendingDownloads(): List<Asset> {
        return assetDao.getPendingDownloads().map { it.toDomain() }
    }
    
    override suspend fun getFailedDownloads(): List<Asset> {
        return assetDao.getFailedDownloads().map { it.toDomain() }
    }
    
    override suspend fun retryAssetDownload(assetId: String): NetworkResult<Asset> {
        val asset = assetDao.getAssetById(assetId)
        return if (asset != null) {
            downloadAsset(asset.filename, asset.diaryEntryId)
        } else {
            NetworkResult.Error(
                com.example.geekdiary.data.remote.NetworkException.UnknownError(
                    Exception("Asset not found: $assetId")
                )
            )
        }
    }
    
    override suspend fun getStorageStats(): Pair<Long, Int> {
        val totalSize = assetStorageManager.getTotalStorageSize()
        val assetCount = assetStorageManager.getAssetCount()
        return Pair(totalSize, assetCount)
    }
}
