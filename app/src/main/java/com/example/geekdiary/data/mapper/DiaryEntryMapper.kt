package com.example.geekdiary.data.mapper

import com.example.geekdiary.data.remote.dto.ItemsRequestDto
import com.example.geekdiary.data.remote.dto.ItemsResponseDto
import com.example.geekdiary.data.remote.dto.ItemsListResponseDto
import com.example.geekdiary.domain.model.DiaryEntry
import com.example.geekdiary.domain.model.DiaryEntryList
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun ItemsResponseDto.toDomain(): DiaryEntry {
    return DiaryEntry(
        date = LocalDate.parse(date, dateFormatter),
        title = title,
        body = body,
        tags = tags,
        previousDate = previousDate?.let { LocalDate.parse(it, dateFormatter) },
        nextDate = nextDate?.let { LocalDate.parse(it, dateFormatter) },
        isLocal = false,
        needsSync = false
    )
}

fun DiaryEntry.toRequestDto(): ItemsRequestDto {
    return ItemsRequestDto(
        date = date.format(dateFormatter),
        title = title,
        body = body,
        tags = tags
    )
}

fun DiaryEntry.toResponseDto(): ItemsResponseDto {
    return ItemsResponseDto(
        date = date.format(dateFormatter),
        title = title,
        body = body,
        tags = tags,
        previousDate = previousDate?.format(dateFormatter),
        nextDate = nextDate?.format(dateFormatter)
    )
}

fun ItemsListResponseDto.toDomain(): DiaryEntryList {
    return DiaryEntryList(
        entries = items.map { it.toDomain() },
        totalCount = totalCount
    )
}
