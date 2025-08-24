package com.example.geekdiary.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.domain.model.AuthData
import com.example.geekdiary.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoginSuccessful: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    fun login(email: String, password: String) {
        // Clear previous errors
        _uiState.value = _uiState.value.copy(
            emailError = null,
            passwordError = null,
            errorMessage = null
        )
        
        // Validate input
        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)
        
        if (emailError != null || passwordError != null) {
            _uiState.value = _uiState.value.copy(
                emailError = emailError,
                passwordError = passwordError
            )
            return
        }
        
        // Perform login
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            val authData = AuthData(email.trim(), password)
            when (val result = authRepository.login(authData)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoginSuccessful = true
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exception.message
                    )
                }
                is NetworkResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
    
    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email/Username is required"
            email.length < 2 -> "Email/Username must be at least 2 characters"
            else -> null
        }
    }
    
    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password is required"
            password.length < 3 -> "Password must be at least 3 characters"
            else -> null
        }
    }
}
