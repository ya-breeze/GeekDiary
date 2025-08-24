package com.example.geekdiary.domain.repository

import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.domain.model.AuthData
import com.example.geekdiary.domain.model.AuthResult
import com.example.geekdiary.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(authData: AuthData): NetworkResult<AuthResult>
    suspend fun logout()
    suspend fun getCurrentUser(): User?
    fun getCurrentUserFlow(): Flow<User?>
    suspend fun isLoggedIn(): Boolean
    suspend fun refreshToken(): NetworkResult<String>
}
