package com.example.geekdiary.data.remote.api

import com.example.geekdiary.data.remote.dto.ItemsListResponseDto
import com.example.geekdiary.data.remote.dto.ItemsRequestDto
import com.example.geekdiary.data.remote.dto.ItemsResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface DiaryApiService {
    
    @GET("v1/items")
    suspend fun getItems(
        @Query("date") date: String? = null,
        @Query("search") search: String? = null,
        @Query("tags") tags: String? = null
    ): Response<ItemsListResponseDto>
    
    @PUT("v1/items")
    suspend fun putItem(@Body item: ItemsRequestDto): Response<ItemsResponseDto>
}
