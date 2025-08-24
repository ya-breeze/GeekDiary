package com.example.geekdiary.data.mapper

import com.example.geekdiary.data.remote.dto.AuthDataDto
import com.example.geekdiary.domain.model.AuthData

fun AuthData.toDto(): AuthDataDto {
    return AuthDataDto(
        email = email,
        password = password
    )
}
