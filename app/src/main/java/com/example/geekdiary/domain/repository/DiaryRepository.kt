package com.example.geekdiary.domain.repository

import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.domain.model.DiaryEntry
import com.example.geekdiary.domain.model.DiaryEntryList
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DiaryRepository {
    suspend fun getEntryByDate(date: LocalDate): DiaryEntry?
    fun getEntryByDateFlow(date: LocalDate): Flow<DiaryEntry?>
    suspend fun getAllEntries(): List<DiaryEntry>
    fun getAllEntriesFlow(): Flow<List<DiaryEntry>>
    suspend fun searchEntries(query: String): List<DiaryEntry>
    suspend fun saveEntry(entry: DiaryEntry): NetworkResult<DiaryEntry>
    suspend fun deleteEntry(date: LocalDate): NetworkResult<Unit>
    suspend fun syncEntries(): NetworkResult<Unit>
    suspend fun getEntriesFromRemote(date: LocalDate? = null, search: String? = null, tags: String? = null): NetworkResult<DiaryEntryList>
}
