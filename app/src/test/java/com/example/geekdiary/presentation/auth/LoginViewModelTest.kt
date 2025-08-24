package com.example.geekdiary.presentation.auth

import app.cash.turbine.test
import com.example.geekdiary.data.remote.NetworkException
import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.domain.model.AuthData
import com.example.geekdiary.domain.model.AuthResult
import com.example.geekdiary.domain.model.User
import com.example.geekdiary.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    
    @Mock
    private lateinit var authRepository: AuthRepository
    
    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(authRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `login with valid credentials should succeed`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password"
        val user = User("user-id", email, LocalDateTime.now())
        val authResult = AuthResult("token", user)
        
        whenever(authRepository.login(any<AuthData>()))
            .thenReturn(NetworkResult.Success(authResult))
        
        // When & Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            assert(!initialState.isLoading)
            assert(!initialState.isLoginSuccessful)
            
            viewModel.login(email, password)
            
            val loadingState = awaitItem()
            assert(loadingState.isLoading)
            
            val successState = awaitItem()
            assert(!successState.isLoading)
            assert(successState.isLoginSuccessful)
            assert(successState.errorMessage == null)
        }
    }
    
    @Test
    fun `login with invalid credentials should show error`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "wrong-password"
        
        whenever(authRepository.login(any<AuthData>()))
            .thenReturn(NetworkResult.Error(NetworkException.Unauthorized))
        
        // When & Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            assert(!initialState.isLoading)
            
            viewModel.login(email, password)
            
            val loadingState = awaitItem()
            assert(loadingState.isLoading)
            
            val errorState = awaitItem()
            assert(!errorState.isLoading)
            assert(!errorState.isLoginSuccessful)
            assert(errorState.errorMessage != null)
        }
    }
    
    @Test
    fun `login with empty email should show validation error`() = runTest {
        // When & Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            assert(initialState.emailError == null)
            
            viewModel.login("", "password")
            
            val errorState = awaitItem()
            assert(errorState.emailError == "Email is required")
            assert(!errorState.isLoading)
        }
    }
    
    @Test
    fun `login with invalid email format should show validation error`() = runTest {
        // When & Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            assert(initialState.emailError == null)
            
            viewModel.login("invalid-email", "password")
            
            val errorState = awaitItem()
            assert(errorState.emailError == "Invalid email format")
            assert(!errorState.isLoading)
        }
    }
    
    @Test
    fun `login with empty password should show validation error`() = runTest {
        // When & Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            assert(initialState.passwordError == null)
            
            viewModel.login("test@example.com", "")
            
            val errorState = awaitItem()
            assert(errorState.passwordError == "Password is required")
            assert(!errorState.isLoading)
        }
    }
}
