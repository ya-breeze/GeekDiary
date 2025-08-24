package com.example.geekdiary.data.local.dao

import androidx.room.*
import com.example.geekdiary.data.local.entity.PendingAssetUploadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingAssetUploadDao {
    
    @Query("SELECT * FROM pending_asset_uploads WHERE uploadStatus = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingUploads(): List<PendingAssetUploadEntity>
    
    @Query("SELECT * FROM pending_asset_uploads WHERE uploadStatus = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingUploadsFlow(): Flow<List<PendingAssetUploadEntity>>
    
    @Query("SELECT * FROM pending_asset_uploads WHERE uploadStatus = 'FAILED' AND retryCount < 3 ORDER BY updatedAt ASC")
    suspend fun getFailedUploadsForRetry(): List<PendingAssetUploadEntity>
    
    @Query("SELECT * FROM pending_asset_uploads WHERE diaryEntryId = :diaryEntryId")
    suspend fun getPendingUploadsByDiaryEntryId(diaryEntryId: String): List<PendingAssetUploadEntity>
    
    @Query("SELECT * FROM pending_asset_uploads WHERE localFilePath = :localFilePath LIMIT 1")
    suspend fun getPendingUploadByLocalPath(localFilePath: String): PendingAssetUploadEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingUpload(pendingUpload: PendingAssetUploadEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingUploads(pendingUploads: List<PendingAssetUploadEntity>)
    
    @Update
    suspend fun updatePendingUpload(pendingUpload: PendingAssetUploadEntity)
    
    @Delete
    suspend fun deletePendingUpload(pendingUpload: PendingAssetUploadEntity)
    
    @Query("DELETE FROM pending_asset_uploads WHERE diaryEntryId = :diaryEntryId")
    suspend fun deletePendingUploadsByDiaryEntryId(diaryEntryId: String)
    
    @Query("DELETE FROM pending_asset_uploads WHERE localFilePath = :localFilePath")
    suspend fun deletePendingUploadByLocalPath(localFilePath: String)
    
    @Query("DELETE FROM pending_asset_uploads WHERE uploadStatus = 'COMPLETED'")
    suspend fun deleteCompletedUploads()
    
    @Query("UPDATE pending_asset_uploads SET uploadStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateUploadStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE pending_asset_uploads SET uploadStatus = :status, retryCount = :retryCount, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateUploadFailure(
        id: String, 
        status: String, 
        retryCount: Int, 
        errorMessage: String?, 
        updatedAt: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE pending_asset_uploads SET uploadStatus = :status, backendFilename = :backendFilename, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateUploadComplete(
        id: String, 
        status: String, 
        backendFilename: String, 
        updatedAt: Long = System.currentTimeMillis()
    )
    
    @Query("SELECT COUNT(*) FROM pending_asset_uploads WHERE uploadStatus IN ('PENDING', 'UPLOADING')")
    suspend fun getActiveUploadCount(): Int
}
