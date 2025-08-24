package com.example.geekdiary.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AssetUploadResponseDto(
    @Json(name = "filename")
    val filename: String
)

@JsonClass(generateAdapter = true)
data class AssetErrorDto(
    @Json(name = "error")
    val error: String,
    @Json(name = "message")
    val message: String? = null
)
