package com.example.geekdiary.data.sync

import com.example.geekdiary.data.local.dao.DiaryEntryDao
import com.example.geekdiary.data.local.dao.SyncDao
import com.example.geekdiary.data.local.dao.UserDao
import com.example.geekdiary.data.local.entity.SyncStateEntity
import com.example.geekdiary.data.mapper.toDomain
import com.example.geekdiary.data.mapper.toEntity
import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.domain.model.OperationType
import com.example.geekdiary.domain.model.SyncChange
import com.example.geekdiary.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val pendingChanges: Int = 0,
    val error: String? = null
)

@Singleton
class SyncManager @Inject constructor(
    private val syncRepository: SyncRepository,
    private val syncDao: SyncDao,
    private val userDao: UserDao,
    private val diaryEntryDao: DiaryEntryDao
) {
    
    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    suspend fun performIncrementalSync(): NetworkResult<Unit> {
        val user = userDao.getCurrentUser() ?: return NetworkResult.Error(
            com.example.geekdiary.data.remote.NetworkException.Unauthorized
        )
        
        _syncStatus.value = _syncStatus.value.copy(isSyncing = true, error = null)
        
        try {
            // Get current sync state
            val syncState = syncDao.getSyncState(user.id) ?: SyncStateEntity(
                userId = user.id,
                lastSyncId = 0,
                lastSyncTimestamp = null,
                syncInProgress = false
            )
            
            // Fetch changes from server
            when (val result = syncRepository.getChangesSince(syncState.lastSyncId)) {
                is NetworkResult.Success -> {
                    val syncResult = result.data
                    
                    // Apply changes to local database
                    for (change in syncResult.changes) {
                        applyRemoteChange(change)
                    }
                    
                    // Update sync state
                    val newSyncState = syncState.copy(
                        lastSyncId = syncResult.changes.maxOfOrNull { it.id } ?: syncState.lastSyncId,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        syncInProgress = false
                    )
                    syncDao.insertSyncState(newSyncState)
                    
                    _syncStatus.value = _syncStatus.value.copy(
                        isSyncing = false,
                        lastSyncTime = System.currentTimeMillis(),
                        pendingChanges = syncDao.getPendingChangesCount(user.id)
                    )
                    
                    return NetworkResult.Success(Unit)
                }
                is NetworkResult.Error -> {
                    _syncStatus.value = _syncStatus.value.copy(
                        isSyncing = false,
                        error = result.exception.message
                    )
                    return NetworkResult.Error(result.exception)
                }
                is NetworkResult.Loading -> {
                    return NetworkResult.Loading()
                }
            }
        } catch (e: Exception) {
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                error = e.message
            )
            return NetworkResult.Error(
                com.example.geekdiary.data.remote.NetworkException.UnknownError(e)
            )
        }
    }
    
    private suspend fun applyRemoteChange(change: SyncChange) {
        val user = userDao.getCurrentUser() ?: return
        
        when (change.operationType) {
            OperationType.CREATED, OperationType.UPDATED -> {
                change.itemSnapshot?.let { entry ->
                    val entity = entry.toEntity(user.id).copy(
                        isSynced = true,
                        needsSync = false,
                        lastSyncId = change.id
                    )
                    diaryEntryDao.insertEntry(entity)
                }
            }
            OperationType.DELETED -> {
                diaryEntryDao.deleteEntryByDate(user.id, change.date.toString())
            }
        }
    }
    
    suspend fun getSyncStatus(): SyncStatus {
        val user = userDao.getCurrentUser() ?: return _syncStatus.value
        
        val pendingChanges = syncDao.getPendingChangesCount(user.id)
        val syncState = syncDao.getSyncState(user.id)
        
        return _syncStatus.value.copy(
            pendingChanges = pendingChanges,
            lastSyncTime = syncState?.lastSyncTimestamp
        )
    }
    
    suspend fun hasPendingChanges(): Boolean {
        return syncRepository.hasPendingChanges()
    }
}
