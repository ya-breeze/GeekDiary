package com.example.geekdiary.presentation.auth

import com.example.geekdiary.domain.model.User

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User, val isFirstLogin: Boolean = false) : AuthState()
    data class Error(val message: String) : AuthState()
}
