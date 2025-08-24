package com.example.geekdiary.data.remote.api

import com.example.geekdiary.data.remote.dto.SyncResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SyncApiService {
    
    @GET("v1/sync/changes")
    suspend fun getChanges(
        @Query("since") since: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<SyncResponseDto>
}
