package com.example.geekdiary.data.mapper

import com.example.geekdiary.data.local.entity.DiaryEntryEntity
import com.example.geekdiary.data.local.entity.UserEntity
import com.example.geekdiary.domain.model.DiaryEntry
import com.example.geekdiary.domain.model.User
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

// JSON adapter for tags
private val moshi = Moshi.Builder().build()
private val listStringType = Types.newParameterizedType(List::class.java, String::class.java)
private val tagsAdapter: JsonAdapter<List<String>> = moshi.adapter(listStringType)

// User Entity Mappers
fun UserEntity.toDomain(): User {
    return User(
        id = id,
        email = email,
        startDate = LocalDateTime.parse(startDate, dateTimeFormatter)
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        email = email,
        startDate = startDate.format(dateTimeFormatter),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

// Diary Entry Entity Mappers
fun DiaryEntryEntity.toDomain(): DiaryEntry {
    val tagsList = try {
        tagsAdapter.fromJson(tags) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
    
    return DiaryEntry(
        date = LocalDate.parse(date, dateFormatter),
        title = title,
        body = body,
        tags = tagsList,
        previousDate = previousDate?.let { LocalDate.parse(it, dateFormatter) },
        nextDate = nextDate?.let { LocalDate.parse(it, dateFormatter) },
        isLocal = !isSynced,
        needsSync = needsSync
    )
}

fun DiaryEntry.toEntity(userId: String): DiaryEntryEntity {
    val tagsJson = tagsAdapter.toJson(tags)
    
    return DiaryEntryEntity(
        id = UUID.randomUUID().toString(),
        userId = userId,
        date = date.format(dateFormatter),
        title = title,
        body = body,
        tags = tagsJson,
        previousDate = previousDate?.format(dateFormatter),
        nextDate = nextDate?.format(dateFormatter),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        isSynced = !isLocal,
        needsSync = needsSync
    )
}
