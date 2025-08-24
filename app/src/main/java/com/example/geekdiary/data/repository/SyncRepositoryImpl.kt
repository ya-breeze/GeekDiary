package com.example.geekdiary.data.repository

import com.example.geekdiary.data.local.dao.SyncDao
import com.example.geekdiary.data.local.dao.UserDao
import com.example.geekdiary.data.mapper.toDomain
import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.data.remote.api.SyncApiService
import com.example.geekdiary.data.remote.safeApiCall
import com.example.geekdiary.domain.model.SyncResult
import com.example.geekdiary.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val syncApiService: SyncApiService,
    private val syncDao: SyncDao,
    private val userDao: UserDao
) : SyncRepository {
    
    override suspend fun performSync(): NetworkResult<SyncResult> {
        val user = userDao.getCurrentUser() ?: return NetworkResult.Error(
            com.example.geekdiary.data.remote.NetworkException.Unauthorized
        )
        
        val syncState = syncDao.getSyncState(user.id)
        val lastSyncId = syncState?.lastSyncId ?: 0
        
        return getChangesSince(lastSyncId)
    }
    
    override suspend fun getChangesSince(sinceId: Int): NetworkResult<SyncResult> {
        return when (val result = safeApiCall { 
            syncApiService.getChanges(since = sinceId, limit = 100) 
        }) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> NetworkResult.Error(result.exception)
            is NetworkResult.Loading -> NetworkResult.Loading()
        }
    }
    
    override suspend fun hasPendingChanges(): Boolean {
        val user = userDao.getCurrentUser() ?: return false
        return syncDao.getPendingChangesCount(user.id) > 0
    }
    
    override suspend fun getPendingChangesCount(): Int {
        val user = userDao.getCurrentUser() ?: return 0
        return syncDao.getPendingChangesCount(user.id)
    }
}
