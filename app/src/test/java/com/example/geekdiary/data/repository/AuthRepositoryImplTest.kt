package com.example.geekdiary.data.repository

import com.example.geekdiary.data.local.TokenManager
import com.example.geekdiary.data.local.dao.UserDao
import com.example.geekdiary.data.local.entity.UserEntity
import com.example.geekdiary.data.remote.NetworkException
import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.data.remote.api.AuthApiService
import com.example.geekdiary.data.remote.api.UserApiService
import com.example.geekdiary.data.remote.dto.AuthDataDto
import com.example.geekdiary.data.remote.dto.AuthResponseDto
import com.example.geekdiary.data.remote.dto.UserDto
import com.example.geekdiary.domain.model.AuthData
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import io.mockk.mockk
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AuthRepositoryImplTest {
    
    @Mock
    private lateinit var authApiService: AuthApiService
    
    @Mock
    private lateinit var userApiService: UserApiService
    
    @Mock
    private lateinit var userDao: UserDao
    
    @Mock
    private lateinit var tokenManager: TokenManager
    
    private lateinit var authRepository: AuthRepositoryImpl
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        authRepository = AuthRepositoryImpl(
            authApiService = authApiService,
            userApiService = userApiService,
            userDao = userDao,
            tokenManager = tokenManager
        )
    }
    
    @Test
    fun `login success should save token and user`() = runTest {
        // Given
        val authData = AuthData("test@example.com", "password")
        val token = "jwt-token"
        val userDto = UserDto(
            id = "user-id",
            email = "test@example.com",
            startDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        )
        
        whenever(authApiService.authorize(any<AuthDataDto>()))
            .thenReturn(Response.success(AuthResponseDto(token)))
        whenever(userApiService.getUser())
            .thenReturn(Response.success(userDto))
        
        // When
        val result = authRepository.login(authData)
        
        // Then
        assert(result is NetworkResult.Success)
        verify(tokenManager).saveToken(token)
        verify(tokenManager).saveCredentials(authData.email, authData.password)
        verify(userDao).insertUser(any<UserEntity>())
    }
    
    @Test
    fun `login failure should return error`() = runTest {
        // Given
        val authData = AuthData("test@example.com", "wrong-password")
        
        whenever(authApiService.authorize(any<AuthDataDto>()))
            .thenReturn(Response.error(401, mockk<okhttp3.ResponseBody>(relaxed = true)))
        
        // When
        val result = authRepository.login(authData)
        
        // Then
        assert(result is NetworkResult.Error)
        verify(tokenManager, never()).saveToken(any())
        verify(userDao, never()).insertUser(any())
    }
    
    @Test
    fun `logout should clear token and user data`() = runTest {
        // When
        authRepository.logout()
        
        // Then
        verify(tokenManager).clearAll()
        verify(userDao).deleteAllUsers()
    }
    
    @Test
    fun `isLoggedIn should return true when token and user exist`() = runTest {
        // Given
        whenever(tokenManager.getToken()).thenReturn("jwt-token")
        whenever(userDao.getCurrentUser()).thenReturn(mock())
        
        // When
        val result = authRepository.isLoggedIn()
        
        // Then
        assert(result)
    }
    
    @Test
    fun `isLoggedIn should return false when token is missing`() = runTest {
        // Given
        whenever(tokenManager.getToken()).thenReturn(null)
        whenever(userDao.getCurrentUser()).thenReturn(mock())
        
        // When
        val result = authRepository.isLoggedIn()
        
        // Then
        assert(!result)
    }
}
