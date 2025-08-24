package com.example.geekdiary.data.local.dao

import androidx.room.*
import com.example.geekdiary.data.local.entity.DiaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryEntryDao {
    
    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND date = :date")
    suspend fun getEntryByDate(userId: String, date: String): DiaryEntryEntity?
    
    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND date = :date")
    fun getEntryByDateFlow(userId: String, date: String): Flow<DiaryEntryEntity?>
    
    @Query("SELECT * FROM diary_entries WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllEntries(userId: String): List<DiaryEntryEntity>
    
    @Query("SELECT * FROM diary_entries WHERE userId = :userId ORDER BY date DESC")
    fun getAllEntriesFlow(userId: String): Flow<List<DiaryEntryEntity>>
    
    @Query("""
        SELECT * FROM diary_entries 
        WHERE userId = :userId 
        AND (title LIKE '%' || :searchQuery || '%' OR body LIKE '%' || :searchQuery || '%')
        ORDER BY date DESC
    """)
    suspend fun searchEntries(userId: String, searchQuery: String): List<DiaryEntryEntity>
    
    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND needsSync = 1")
    suspend fun getEntriesNeedingSync(userId: String): List<DiaryEntryEntity>
    
    @Query("SELECT COUNT(*) FROM diary_entries WHERE userId = :userId")
    suspend fun getEntryCount(userId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<DiaryEntryEntity>)
    
    @Update
    suspend fun updateEntry(entry: DiaryEntryEntity)
    
    @Delete
    suspend fun deleteEntry(entry: DiaryEntryEntity)
    
    @Query("DELETE FROM diary_entries WHERE userId = :userId AND date = :date")
    suspend fun deleteEntryByDate(userId: String, date: String)
    
    @Query("DELETE FROM diary_entries WHERE userId = :userId")
    suspend fun deleteAllEntries(userId: String)
}
