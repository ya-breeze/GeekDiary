package com.example.geekdiary.data.repository

import com.example.geekdiary.data.local.AssetStorageManager
import com.example.geekdiary.data.local.dao.AssetDao
import com.example.geekdiary.data.local.dao.UserDao
import com.example.geekdiary.data.local.entity.AssetEntity
import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.data.remote.api.AssetApiService
import com.example.geekdiary.domain.model.AssetDownloadStatus
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.util.*

class AssetRepositoryImplTest {
    
    private lateinit var assetRepository: AssetRepositoryImpl
    private val assetApiService = mockk<AssetApiService>()
    private val assetDao = mockk<AssetDao>()
    private val assetStorageManager = mockk<AssetStorageManager>()
    private val userDao = mockk<UserDao>()
    
    @Before
    fun setUp() {
        assetRepository = AssetRepositoryImpl(
            assetApiService,
            assetDao,
            assetStorageManager,
            userDao
        )
    }
    
    @Test
    fun `downloadAsset should return existing asset if already downloaded`() = runTest {
        // Given
        val filename = "test.jpg"
        val diaryEntryId = "entry-1"
        val existingAsset = AssetEntity(
            id = "asset-1",
            filename = filename,
            localFilePath = "/path/to/test.jpg",
            diaryEntryId = diaryEntryId,
            isDownloaded = true,
            downloadStatus = AssetDownloadStatus.COMPLETED.name
        )
        
        coEvery { assetDao.getAssetByFilename(filename) } returns existingAsset
        
        // When
        val result = assetRepository.downloadAsset(filename, diaryEntryId)
        
        // Then
        assertTrue(result is NetworkResult.Success)
        val asset = (result as NetworkResult.Success).data
        assertEquals(filename, asset.filename)
        assertTrue(asset.isDownloaded)
        
        coVerify(exactly = 0) { assetApiService.downloadAsset(any()) }
    }
    
    @Test
    fun `downloadAsset should download and save new asset`() = runTest {
        // Given
        val filename = "test.jpg"
        val diaryEntryId = "entry-1"
        val responseBody = mockk<ResponseBody>()
        val localPath = "/storage/test.jpg"
        
        coEvery { assetDao.getAssetByFilename(filename) } returns null
        coEvery { assetApiService.downloadAsset(filename) } returns Response.success(responseBody)
        coEvery { assetStorageManager.saveAsset(filename, responseBody) } returns localPath
        coEvery { assetDao.insertAsset(any()) } just Runs
        
        // When
        val result = assetRepository.downloadAsset(filename, diaryEntryId)
        
        // Then
        assertTrue(result is NetworkResult.Success)
        val asset = (result as NetworkResult.Success).data
        assertEquals(filename, asset.filename)
        assertEquals(localPath, asset.localFilePath)
        assertTrue(asset.isDownloaded)
        
        coVerify { assetApiService.downloadAsset(filename) }
        coVerify { assetStorageManager.saveAsset(filename, responseBody) }
        coVerify(exactly = 2) { assetDao.insertAsset(any()) } // Once for downloading, once for completed
    }
    
    @Test
    fun `downloadAsset should handle API error`() = runTest {
        // Given
        val filename = "test.jpg"
        val diaryEntryId = "entry-1"
        
        coEvery { assetDao.getAssetByFilename(filename) } returns null
        coEvery { assetApiService.downloadAsset(filename) } throws Exception("Network error")
        coEvery { assetDao.insertAsset(any()) } just Runs
        coEvery { assetDao.updateDownloadStatus(any(), any()) } just Runs
        
        // When
        val result = assetRepository.downloadAsset(filename, diaryEntryId)
        
        // Then
        assertTrue(result is NetworkResult.Error)
        
        coVerify { assetDao.updateDownloadStatus(any(), AssetDownloadStatus.FAILED.name) }
    }
    
    @Test
    fun `isAssetAvailableLocally should return true for downloaded asset`() = runTest {
        // Given
        val filename = "test.jpg"
        val asset = AssetEntity(
            id = "asset-1",
            filename = filename,
            localFilePath = "/path/to/test.jpg",
            diaryEntryId = "entry-1",
            isDownloaded = true
        )
        
        coEvery { assetDao.getAssetByFilename(filename) } returns asset
        coEvery { assetStorageManager.assetExists(filename) } returns true
        
        // When
        val result = assetRepository.isAssetAvailableLocally(filename)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isAssetAvailableLocally should return false for non-downloaded asset`() = runTest {
        // Given
        val filename = "test.jpg"
        val asset = AssetEntity(
            id = "asset-1",
            filename = filename,
            localFilePath = null,
            diaryEntryId = "entry-1",
            isDownloaded = false
        )
        
        coEvery { assetDao.getAssetByFilename(filename) } returns asset
        
        // When
        val result = assetRepository.isAssetAvailableLocally(filename)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `deleteAssetsByDiaryEntryId should delete all assets for entry`() = runTest {
        // Given
        val diaryEntryId = "entry-1"
        val assets = listOf(
            AssetEntity(
                id = "asset-1",
                filename = "test1.jpg",
                localFilePath = "/path/to/test1.jpg",
                diaryEntryId = diaryEntryId,
                isDownloaded = true
            ),
            AssetEntity(
                id = "asset-2",
                filename = "test2.jpg",
                localFilePath = "/path/to/test2.jpg",
                diaryEntryId = diaryEntryId,
                isDownloaded = true
            )
        )
        
        coEvery { assetDao.getAssetsByDiaryEntryId(diaryEntryId) } returns assets
        coEvery { assetStorageManager.deleteAssetByPath(any()) } returns true
        coEvery { assetDao.deleteAssetsByDiaryEntryId(diaryEntryId) } just Runs
        
        // When
        assetRepository.deleteAssetsByDiaryEntryId(diaryEntryId)
        
        // Then
        coVerify { assetStorageManager.deleteAssetByPath("/path/to/test1.jpg") }
        coVerify { assetStorageManager.deleteAssetByPath("/path/to/test2.jpg") }
        coVerify { assetDao.deleteAssetsByDiaryEntryId(diaryEntryId) }
    }
    
    @Test
    fun `getStorageStats should return correct statistics`() = runTest {
        // Given
        val totalSize = 1024L * 1024L // 1MB
        val assetCount = 5
        
        coEvery { assetStorageManager.getTotalStorageSize() } returns totalSize
        coEvery { assetStorageManager.getAssetCount() } returns assetCount
        
        // When
        val result = assetRepository.getStorageStats()
        
        // Then
        assertEquals(totalSize, result.first)
        assertEquals(assetCount, result.second)
    }
}
