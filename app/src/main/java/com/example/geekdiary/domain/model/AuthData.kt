package com.example.geekdiary.domain.model

data class AuthData(
    val email: String,
    val password: String
)

data class AuthResult(
    val token: String,
    val user: User
)
