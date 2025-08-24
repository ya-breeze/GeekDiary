package com.example.geekdiary.data.remote.api

import com.example.geekdiary.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.GET

interface UserApiService {
    
    @GET("v1/user")
    suspend fun getUser(): Response<UserDto>
}
