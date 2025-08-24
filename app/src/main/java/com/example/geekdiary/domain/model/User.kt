package com.example.geekdiary.domain.model

import java.time.LocalDateTime

data class User(
    val id: String,
    val email: String,
    val startDate: LocalDateTime
)
