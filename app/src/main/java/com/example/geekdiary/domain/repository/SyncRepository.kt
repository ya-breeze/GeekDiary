package com.example.geekdiary.domain.repository

import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.domain.model.SyncResult

interface SyncRepository {
    suspend fun performSync(): NetworkResult<SyncResult>
    suspend fun getChangesSince(sinceId: Int): NetworkResult<SyncResult>
    suspend fun hasPendingChanges(): Boolean
    suspend fun getPendingChangesCount(): Int
}
