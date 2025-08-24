package com.example.geekdiary.data.local.dao

import androidx.room.*
import com.example.geekdiary.data.local.entity.PendingChangeEntity
import com.example.geekdiary.data.local.entity.SyncStateEntity

@Dao
interface SyncDao {
    
    // Sync State operations
    @Query("SELECT * FROM sync_state WHERE userId = :userId")
    suspend fun getSyncState(userId: String): SyncStateEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncState(syncState: SyncStateEntity)
    
    @Update
    suspend fun updateSyncState(syncState: SyncStateEntity)
    
    // Pending Changes operations
    @Query("SELECT * FROM pending_changes WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getPendingChanges(userId: String): List<PendingChangeEntity>
    
    @Query("""
        SELECT * FROM pending_changes 
        WHERE userId = :userId 
        AND retryCount < maxRetries 
        AND (nextRetryAt IS NULL OR nextRetryAt <= :currentTime)
        ORDER BY createdAt ASC
    """)
    suspend fun getPendingChangesReadyForRetry(userId: String, currentTime: Long): List<PendingChangeEntity>
    
    @Insert
    suspend fun insertPendingChange(change: PendingChangeEntity): Long
    
    @Update
    suspend fun updatePendingChange(change: PendingChangeEntity)
    
    @Delete
    suspend fun deletePendingChange(change: PendingChangeEntity)
    
    @Query("DELETE FROM pending_changes WHERE userId = :userId")
    suspend fun deleteAllPendingChanges(userId: String)
    
    @Query("SELECT COUNT(*) FROM pending_changes WHERE userId = :userId")
    suspend fun getPendingChangesCount(userId: String): Int
}
