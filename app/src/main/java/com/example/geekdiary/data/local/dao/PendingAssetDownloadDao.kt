package com.example.geekdiary.data.local.dao

import androidx.room.*
import com.example.geekdiary.data.local.entity.PendingAssetDownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingAssetDownloadDao {
    
    @Query("SELECT * FROM pending_asset_downloads WHERE downloadStatus = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingDownloads(): List<PendingAssetDownloadEntity>
    
    @Query("SELECT * FROM pending_asset_downloads WHERE downloadStatus = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingDownloadsFlow(): Flow<List<PendingAssetDownloadEntity>>
    
    @Query("SELECT * FROM pending_asset_downloads WHERE downloadStatus = 'FAILED' AND retryCount < 3 ORDER BY updatedAt ASC")
    suspend fun getFailedDownloadsForRetry(): List<PendingAssetDownloadEntity>
    
    @Query("SELECT * FROM pending_asset_downloads WHERE diaryEntryId = :diaryEntryId")
    suspend fun getPendingDownloadsByDiaryEntryId(diaryEntryId: String): List<PendingAssetDownloadEntity>
    
    @Query("SELECT * FROM pending_asset_downloads WHERE filename = :filename LIMIT 1")
    suspend fun getPendingDownloadByFilename(filename: String): PendingAssetDownloadEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingDownload(pendingDownload: PendingAssetDownloadEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingDownloads(pendingDownloads: List<PendingAssetDownloadEntity>)
    
    @Update
    suspend fun updatePendingDownload(pendingDownload: PendingAssetDownloadEntity)
    
    @Delete
    suspend fun deletePendingDownload(pendingDownload: PendingAssetDownloadEntity)
    
    @Query("DELETE FROM pending_asset_downloads WHERE diaryEntryId = :diaryEntryId")
    suspend fun deletePendingDownloadsByDiaryEntryId(diaryEntryId: String)
    
    @Query("DELETE FROM pending_asset_downloads WHERE filename = :filename")
    suspend fun deletePendingDownloadByFilename(filename: String)
    
    @Query("DELETE FROM pending_asset_downloads WHERE downloadStatus = 'COMPLETED'")
    suspend fun deleteCompletedDownloads()
    
    @Query("UPDATE pending_asset_downloads SET downloadStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE pending_asset_downloads SET downloadStatus = :status, retryCount = :retryCount, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDownloadFailure(
        id: String, 
        status: String, 
        retryCount: Int, 
        errorMessage: String?, 
        updatedAt: Long = System.currentTimeMillis()
    )
    
    @Query("SELECT COUNT(*) FROM pending_asset_downloads WHERE downloadStatus IN ('PENDING', 'DOWNLOADING')")
    suspend fun getActiveDownloadCount(): Int
}
