package com.example.geekdiary.data.repository

import com.example.geekdiary.data.local.TokenManager
import com.example.geekdiary.data.local.dao.UserDao
import com.example.geekdiary.data.mapper.toDomain
import com.example.geekdiary.data.mapper.toDto
import com.example.geekdiary.data.mapper.toEntity
import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.data.remote.api.AuthApiService
import com.example.geekdiary.data.remote.api.UserApiService
import com.example.geekdiary.data.remote.safeApiCall
import com.example.geekdiary.domain.model.AuthData
import com.example.geekdiary.domain.model.AuthResult
import com.example.geekdiary.domain.model.User
import com.example.geekdiary.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val userApiService: UserApiService,
    private val userDao: UserDao,
    private val tokenManager: TokenManager
) : AuthRepository {
    
    override suspend fun login(authData: AuthData): NetworkResult<AuthResult> {
        return when (val authResult = safeApiCall { authApiService.authorize(authData.toDto()) }) {
            is NetworkResult.Success -> {
                val token = authResult.data.token
                tokenManager.saveToken(token)
                tokenManager.saveCredentials(authData.email, authData.password)
                
                // Fetch user profile
                when (val userResult = safeApiCall { userApiService.getUser() }) {
                    is NetworkResult.Success -> {
                        val user = userResult.data.toDomain()
                        userDao.insertUser(user.toEntity())
                        NetworkResult.Success(AuthResult(token, user))
                    }
                    is NetworkResult.Error -> NetworkResult.Error(userResult.exception)
                    is NetworkResult.Loading -> NetworkResult.Loading()
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(authResult.exception)
            is NetworkResult.Loading -> NetworkResult.Loading()
        }
    }
    
    override suspend fun logout() {
        tokenManager.clearAll()
        userDao.deleteAllUsers()
    }
    
    override suspend fun getCurrentUser(): User? {
        return userDao.getCurrentUser()?.toDomain()
    }
    
    override fun getCurrentUserFlow(): Flow<User?> {
        return userDao.getCurrentUserFlow().map { it?.toDomain() }
    }
    
    override suspend fun isLoggedIn(): Boolean {
        return tokenManager.getToken() != null && userDao.getCurrentUser() != null
    }
    
    override suspend fun refreshToken(): NetworkResult<String> {
        val credentials = tokenManager.getCredentials()
        return if (credentials != null) {
            val authData = AuthData(credentials.first, credentials.second)
            when (val result = login(authData)) {
                is NetworkResult.Success -> NetworkResult.Success(result.data.token)
                is NetworkResult.Error -> NetworkResult.Error(result.exception)
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        } else {
            NetworkResult.Error(com.example.geekdiary.data.remote.NetworkException.Unauthorized)
        }
    }
}
