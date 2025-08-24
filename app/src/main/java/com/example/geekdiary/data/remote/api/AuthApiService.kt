package com.example.geekdiary.data.remote.api

import com.example.geekdiary.data.remote.dto.AuthDataDto
import com.example.geekdiary.data.remote.dto.AuthResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    
    @POST("v1/authorize")
    suspend fun authorize(@Body authData: AuthDataDto): Response<AuthResponseDto>
}
