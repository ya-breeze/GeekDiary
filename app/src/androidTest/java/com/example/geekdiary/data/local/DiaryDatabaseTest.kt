package com.example.geekdiary.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.geekdiary.data.local.entity.DiaryEntryEntity
import com.example.geekdiary.data.local.entity.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiaryDatabaseTest {
    
    private lateinit var database: DiaryDatabase
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DiaryDatabase::class.java
        ).allowMainThreadQueries().build()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveUser() = runTest {
        // Given
        val user = UserEntity(
            id = "user-1",
            email = "test@example.com",
            startDate = "2024-01-01T00:00:00"
        )
        
        // When
        database.userDao().insertUser(user)
        val retrievedUser = database.userDao().getUserById("user-1")
        
        // Then
        assert(retrievedUser != null)
        assert(retrievedUser?.email == "test@example.com")
    }
    
    @Test
    fun insertAndRetrieveDiaryEntry() = runTest {
        // Given
        val user = UserEntity(
            id = "user-1",
            email = "test@example.com",
            startDate = "2024-01-01T00:00:00"
        )
        database.userDao().insertUser(user)
        
        val entry = DiaryEntryEntity(
            id = "entry-1",
            userId = "user-1",
            date = "2024-01-15",
            title = "Test Entry",
            body = "This is a test entry",
            tags = "[]"
        )
        
        // When
        database.diaryEntryDao().insertEntry(entry)
        val retrievedEntry = database.diaryEntryDao().getEntryByDate("user-1", "2024-01-15")
        
        // Then
        assert(retrievedEntry != null)
        assert(retrievedEntry?.title == "Test Entry")
        assert(retrievedEntry?.body == "This is a test entry")
    }
    
    @Test
    fun searchEntriesByContent() = runTest {
        // Given
        val user = UserEntity(
            id = "user-1",
            email = "test@example.com",
            startDate = "2024-01-01T00:00:00"
        )
        database.userDao().insertUser(user)
        
        val entries = listOf(
            DiaryEntryEntity(
                id = "entry-1",
                userId = "user-1",
                date = "2024-01-15",
                title = "Vacation Day",
                body = "Had a great vacation at the beach",
                tags = "[]"
            ),
            DiaryEntryEntity(
                id = "entry-2",
                userId = "user-1",
                date = "2024-01-16",
                title = "Work Meeting",
                body = "Important meeting with the team",
                tags = "[]"
            )
        )
        
        // When
        database.diaryEntryDao().insertEntries(entries)
        val searchResults = database.diaryEntryDao().searchEntries("user-1", "vacation")
        
        // Then
        assert(searchResults.size == 1)
        assert(searchResults[0].title == "Vacation Day")
    }
    
    @Test
    fun getEntriesNeedingSync() = runTest {
        // Given
        val user = UserEntity(
            id = "user-1",
            email = "test@example.com",
            startDate = "2024-01-01T00:00:00"
        )
        database.userDao().insertUser(user)
        
        val entries = listOf(
            DiaryEntryEntity(
                id = "entry-1",
                userId = "user-1",
                date = "2024-01-15",
                title = "Synced Entry",
                body = "This entry is synced",
                tags = "[]",
                needsSync = false
            ),
            DiaryEntryEntity(
                id = "entry-2",
                userId = "user-1",
                date = "2024-01-16",
                title = "Unsynced Entry",
                body = "This entry needs sync",
                tags = "[]",
                needsSync = true
            )
        )
        
        // When
        database.diaryEntryDao().insertEntries(entries)
        val unsyncedEntries = database.diaryEntryDao().getEntriesNeedingSync("user-1")
        
        // Then
        assert(unsyncedEntries.size == 1)
        assert(unsyncedEntries[0].title == "Unsynced Entry")
    }
}
