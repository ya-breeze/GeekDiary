package com.example.geekdiary.data.mapper

import com.example.geekdiary.data.remote.dto.UserDto
import com.example.geekdiary.domain.model.User
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun UserDto.toDomain(): User {
    return User(
        id = id,
        email = email,
        startDate = LocalDateTime.parse(startDate, DateTimeFormatter.ISO_DATE_TIME)
    )
}

fun User.toDto(): UserDto {
    return UserDto(
        id = id,
        email = email,
        startDate = startDate.format(DateTimeFormatter.ISO_DATE_TIME)
    )
}
