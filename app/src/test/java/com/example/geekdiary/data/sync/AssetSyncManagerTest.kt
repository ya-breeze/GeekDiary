package com.example.geekdiary.data.sync

import com.example.geekdiary.data.local.SettingsManager
import com.example.geekdiary.data.local.dao.PendingAssetDownloadDao
import com.example.geekdiary.data.local.dao.PendingAssetUploadDao
import com.example.geekdiary.data.local.entity.PendingAssetDownloadEntity
import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.data.util.MarkdownAssetParser
import com.example.geekdiary.domain.model.Asset
import com.example.geekdiary.domain.model.AssetDownloadStatus
import com.example.geekdiary.domain.model.DiaryEntry
import com.example.geekdiary.domain.repository.AssetRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AssetSyncManagerTest {
    
    private lateinit var assetSyncManager: AssetSyncManager
    private val assetRepository = mockk<AssetRepository>()
    private val pendingAssetDownloadDao = mockk<PendingAssetDownloadDao>()
    private val pendingAssetUploadDao = mockk<PendingAssetUploadDao>()
    private val markdownAssetParser = mockk<MarkdownAssetParser>()
    private val settingsManager = mockk<SettingsManager>()
    
    @Before
    fun setUp() {
        assetSyncManager = AssetSyncManager(
            assetRepository,
            pendingAssetDownloadDao,
            pendingAssetUploadDao,
            markdownAssetParser,
            settingsManager
        )
    }
    
    @Test
    fun `processDiaryEntryAssets should queue assets for download`() = runTest {
        // Given
        val diaryEntry = DiaryEntry(
            date = LocalDate.now(),
            title = "Test Entry",
            body = "![](test.jpg) and ![](video.mp4)"
        )
        val assetFilenames = listOf("test.jpg", "video.mp4")
        
        coEvery { settingsManager.getServerUrl() } returns "http://localhost:8080"
        coEvery { markdownAssetParser.extractAssetFilenames(diaryEntry.body) } returns assetFilenames
        coEvery { assetRepository.isAssetAvailableLocally("test.jpg") } returns false
        coEvery { assetRepository.isAssetAvailableLocally("video.mp4") } returns true
        coEvery { pendingAssetDownloadDao.getPendingDownloadByFilename("test.jpg") } returns null
        coEvery { pendingAssetDownloadDao.insertPendingDownload(any()) } just Runs
        
        // When
        assetSyncManager.processDiaryEntryAssets(diaryEntry)
        
        // Then
        coVerify { pendingAssetDownloadDao.insertPendingDownload(any()) }
        coVerify(exactly = 0) { pendingAssetDownloadDao.getPendingDownloadByFilename("video.mp4") }
    }
    
    @Test
    fun `downloadPendingAssets should download queued assets`() = runTest {
        // Given
        val pendingDownloads = listOf(
            PendingAssetDownloadEntity(
                id = "pending-1",
                filename = "test.jpg",
                diaryEntryId = "entry-1",
                downloadStatus = "PENDING"
            )
        )
        val downloadedAsset = Asset(
            id = "asset-1",
            filename = "test.jpg",
            localFilePath = "/path/to/test.jpg",
            diaryEntryId = "entry-1",
            isDownloaded = true,
            downloadStatus = AssetDownloadStatus.COMPLETED
        )
        
        coEvery { pendingAssetDownloadDao.getPendingDownloads() } returns pendingDownloads
        coEvery { pendingAssetDownloadDao.updateDownloadStatus(any(), any(), any()) } just Runs
        coEvery { assetRepository.downloadAsset("test.jpg", "entry-1") } returns NetworkResult.Success(downloadedAsset)
        coEvery { pendingAssetDownloadDao.deletePendingDownload(any()) } just Runs

        // When
        val result = assetSyncManager.downloadPendingAssets(maxConcurrentDownloads = 1)

        // Then
        assertEquals(1, result)
        coVerify { assetRepository.downloadAsset("test.jpg", "entry-1") }
        coVerify { pendingAssetDownloadDao.updateDownloadStatus("pending-1", "DOWNLOADING", any()) }
        coVerify { pendingAssetDownloadDao.updateDownloadStatus("pending-1", "COMPLETED", any()) }
        coVerify { pendingAssetDownloadDao.deletePendingDownload(any()) }
    }
    
    @Test
    fun `downloadPendingAssets should handle download failures`() = runTest {
        // Given
        val pendingDownloads = listOf(
            PendingAssetDownloadEntity(
                id = "pending-1",
                filename = "test.jpg",
                diaryEntryId = "entry-1",
                downloadStatus = "PENDING",
                retryCount = 0
            )
        )
        
        coEvery { pendingAssetDownloadDao.getPendingDownloads() } returns pendingDownloads
        coEvery { pendingAssetDownloadDao.updateDownloadStatus(any(), any(), any()) } just Runs
        coEvery { assetRepository.downloadAsset("test.jpg", "entry-1") } returns NetworkResult.Error(
            com.example.geekdiary.data.remote.NetworkException.UnknownError(Exception("Network error"))
        )
        coEvery { pendingAssetDownloadDao.updateDownloadFailure(any(), any(), any(), any(), any()) } just Runs

        // When
        val result = assetSyncManager.downloadPendingAssets(maxConcurrentDownloads = 1)

        // Then
        assertEquals(0, result)
        coVerify { pendingAssetDownloadDao.updateDownloadFailure("pending-1", "FAILED", 1, "Unknown error", any()) }
    }
    
    @Test
    fun `retryFailedDownloads should retry failed downloads`() = runTest {
        // Given
        val failedDownloads = listOf(
            PendingAssetDownloadEntity(
                id = "failed-1",
                filename = "test.jpg",
                diaryEntryId = "entry-1",
                downloadStatus = "FAILED",
                retryCount = 1
            )
        )
        val downloadedAsset = Asset(
            id = "asset-1",
            filename = "test.jpg",
            localFilePath = "/path/to/test.jpg",
            diaryEntryId = "entry-1",
            isDownloaded = true,
            downloadStatus = AssetDownloadStatus.COMPLETED
        )
        
        coEvery { pendingAssetDownloadDao.getFailedDownloadsForRetry() } returns failedDownloads
        coEvery { pendingAssetDownloadDao.updateDownloadStatus(any(), any(), any()) } just Runs
        coEvery { assetRepository.downloadAsset("test.jpg", "entry-1") } returns NetworkResult.Success(downloadedAsset)
        coEvery { pendingAssetDownloadDao.deletePendingDownload(any()) } just Runs

        // When
        val result = assetSyncManager.retryFailedDownloads()

        // Then
        assertEquals(1, result)
        coVerify { assetRepository.downloadAsset("test.jpg", "entry-1") }
        coVerify { pendingAssetDownloadDao.updateDownloadStatus("failed-1", "DOWNLOADING", any()) }
        coVerify { pendingAssetDownloadDao.updateDownloadStatus("failed-1", "COMPLETED", any()) }
        coVerify { pendingAssetDownloadDao.deletePendingDownload(any()) }
    }
    
    @Test
    fun `getPendingDownloadStats should return correct statistics`() = runTest {
        // Given
        val pendingDownloads = listOf(
            PendingAssetDownloadEntity(
                id = "pending-1",
                filename = "test1.jpg",
                diaryEntryId = "entry-1",
                downloadStatus = "PENDING"
            ),
            PendingAssetDownloadEntity(
                id = "downloading-1",
                filename = "test2.jpg",
                diaryEntryId = "entry-1",
                downloadStatus = "DOWNLOADING"
            ),
            PendingAssetDownloadEntity(
                id = "failed-1",
                filename = "test3.jpg",
                diaryEntryId = "entry-1",
                downloadStatus = "FAILED"
            )
        )
        
        coEvery { pendingAssetDownloadDao.getPendingDownloads() } returns pendingDownloads
        
        // When
        val result = assetSyncManager.getPendingDownloadStats()
        
        // Then
        assertEquals(1, result.first) // pending count
        assertEquals(1, result.second) // downloading count
        assertEquals(1, result.third) // failed count
    }
    
    @Test
    fun `cleanupAssetsForDeletedEntry should clean up assets and pending operations`() = runTest {
        // Given
        val diaryEntryId = "entry-1"
        
        coEvery { assetRepository.deleteAssetsByDiaryEntryId(diaryEntryId) } just Runs
        coEvery { pendingAssetDownloadDao.deletePendingDownloadsByDiaryEntryId(diaryEntryId) } just Runs
        coEvery { pendingAssetUploadDao.deletePendingUploadsByDiaryEntryId(diaryEntryId) } just Runs
        
        // When
        assetSyncManager.cleanupAssetsForDeletedEntry(diaryEntryId)
        
        // Then
        coVerify { assetRepository.deleteAssetsByDiaryEntryId(diaryEntryId) }
        coVerify { pendingAssetDownloadDao.deletePendingDownloadsByDiaryEntryId(diaryEntryId) }
        coVerify { pendingAssetUploadDao.deletePendingUploadsByDiaryEntryId(diaryEntryId) }
    }
}
