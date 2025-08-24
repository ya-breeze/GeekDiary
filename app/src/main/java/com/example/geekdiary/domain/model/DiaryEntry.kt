package com.example.geekdiary.domain.model

import java.time.LocalDate

data class DiaryEntry(
    val date: LocalDate,
    val title: String,
    val body: String,
    val tags: List<String> = emptyList(),
    val previousDate: LocalDate? = null,
    val nextDate: LocalDate? = null,
    val isLocal: Boolean = false,
    val needsSync: Boolean = false
)

data class DiaryEntryList(
    val entries: List<DiaryEntry>,
    val totalCount: Int
)
