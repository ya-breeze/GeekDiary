package com.example.geekdiary.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncChangeResponseDto(
    @Json(name = "id")
    val id: Int,
    @Json(name = "userId")
    val userId: String,
    @Json(name = "date")
    val date: String,
    @Json(name = "operationType")
    val operationType: String, // "created", "updated", "deleted"
    @Json(name = "timestamp")
    val timestamp: String,
    @Json(name = "itemSnapshot")
    val itemSnapshot: ItemsResponseDto? = null,
    @Json(name = "metadata")
    val metadata: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SyncResponseDto(
    @Json(name = "changes")
    val changes: List<SyncChangeResponseDto>,
    @Json(name = "hasMore")
    val hasMore: Boolean,
    @Json(name = "nextId")
    val nextId: Int? = null
)
