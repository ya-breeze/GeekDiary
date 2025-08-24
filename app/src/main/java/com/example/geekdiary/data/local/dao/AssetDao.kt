package com.example.geekdiary.data.local.dao

import androidx.room.*
import com.example.geekdiary.data.local.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    
    @Query("SELECT * FROM assets WHERE diaryEntryId = :diaryEntryId")
    suspend fun getAssetsByDiaryEntryId(diaryEntryId: String): List<AssetEntity>
    
    @Query("SELECT * FROM assets WHERE diaryEntryId = :diaryEntryId")
    fun getAssetsByDiaryEntryIdFlow(diaryEntryId: String): Flow<List<AssetEntity>>
    
    @Query("SELECT * FROM assets WHERE filename = :filename LIMIT 1")
    suspend fun getAssetByFilename(filename: String): AssetEntity?
    
    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun getAssetById(id: String): AssetEntity?
    
    @Query("SELECT * FROM assets WHERE isDownloaded = 0")
    suspend fun getPendingDownloads(): List<AssetEntity>
    
    @Query("SELECT * FROM assets WHERE downloadStatus = 'FAILED'")
    suspend fun getFailedDownloads(): List<AssetEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)
    
    @Update
    suspend fun updateAsset(asset: AssetEntity)
    
    @Delete
    suspend fun deleteAsset(asset: AssetEntity)
    
    @Query("DELETE FROM assets WHERE diaryEntryId = :diaryEntryId")
    suspend fun deleteAssetsByDiaryEntryId(diaryEntryId: String)
    
    @Query("DELETE FROM assets WHERE filename = :filename")
    suspend fun deleteAssetByFilename(filename: String)
    
    @Query("UPDATE assets SET downloadStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE assets SET isDownloaded = :isDownloaded, localFilePath = :localFilePath, downloadStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDownloadComplete(
        id: String, 
        isDownloaded: Boolean, 
        localFilePath: String?, 
        status: String, 
        updatedAt: Long = System.currentTimeMillis()
    )
    
    @Query("SELECT COUNT(*) FROM assets WHERE diaryEntryId = :diaryEntryId AND isDownloaded = 0")
    suspend fun getPendingDownloadCount(diaryEntryId: String): Int
    
    @Query("SELECT COUNT(*) FROM assets WHERE isDownloaded = 0")
    suspend fun getTotalPendingDownloadCount(): Int
}
