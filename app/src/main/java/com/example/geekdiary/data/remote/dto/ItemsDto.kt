package com.example.geekdiary.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ItemsRequestDto(
    @Json(name = "date")
    val date: String,
    @Json(name = "title")
    val title: String,
    @Json(name = "body")
    val body: String,
    @Json(name = "tags")
    val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ItemsResponseDto(
    @Json(name = "date")
    val date: String,
    @Json(name = "title")
    val title: String,
    @Json(name = "body")
    val body: String,
    @Json(name = "tags")
    val tags: List<String> = emptyList(),
    @Json(name = "previousDate")
    val previousDate: String? = null,
    @Json(name = "nextDate")
    val nextDate: String? = null
)

@JsonClass(generateAdapter = true)
data class ItemsListResponseDto(
    @Json(name = "items")
    val items: List<ItemsResponseDto>,
    @Json(name = "totalCount")
    val totalCount: Int
)
