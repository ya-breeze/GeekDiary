package com.example.geekdiary.data.repository

import com.example.geekdiary.data.local.dao.DiaryEntryDao
import com.example.geekdiary.data.local.dao.UserDao
import com.example.geekdiary.data.mapper.toDomain
import com.example.geekdiary.data.mapper.toEntity
import com.example.geekdiary.data.mapper.toRequestDto
import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.data.remote.api.DiaryApiService
import com.example.geekdiary.data.remote.safeApiCall
import com.example.geekdiary.domain.model.DiaryEntry
import com.example.geekdiary.domain.model.DiaryEntryList
import com.example.geekdiary.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepositoryImpl @Inject constructor(
    private val diaryApiService: DiaryApiService,
    private val diaryEntryDao: DiaryEntryDao,
    private val userDao: UserDao,
    private val assetReferenceTracker: com.example.geekdiary.data.sync.AssetReferenceTracker,
    private val markdownLinkTransformer: com.example.geekdiary.data.util.MarkdownLinkTransformer,
    private val settingsManager: com.example.geekdiary.data.local.SettingsManager
) : DiaryRepository {
    
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    override suspend fun getEntryByDate(date: LocalDate): DiaryEntry? {
        val user = userDao.getCurrentUser() ?: return null
        val entity = diaryEntryDao.getEntryByDate(user.id, date.format(dateFormatter))
        return entity?.let {
            val domainEntry = it.toDomain()
            // Transform asset URLs for display
            transformEntryForDisplay(domainEntry)
        }
    }
    
    override fun getEntryByDateFlow(date: LocalDate): Flow<DiaryEntry?> {
        return userDao.getCurrentUserFlow().map { user ->
            if (user != null) {
                val entity = diaryEntryDao.getEntryByDate(user.id, date.format(dateFormatter))
                entity?.let {
                    val domainEntry = it.toDomain()
                    // Transform asset URLs for display
                    transformEntryForDisplay(domainEntry)
                }
            } else {
                null
            }
        }
    }
    
    override suspend fun getAllEntries(): List<DiaryEntry> {
        val user = userDao.getCurrentUser() ?: return emptyList()
        return diaryEntryDao.getAllEntries(user.id).map { it.toDomain() }
    }
    
    override fun getAllEntriesFlow(): Flow<List<DiaryEntry>> {
        return userDao.getCurrentUserFlow().map { user ->
            if (user != null) {
                diaryEntryDao.getAllEntries(user.id).map { it.toDomain() }
            } else {
                emptyList()
            }
        }
    }
    
    override suspend fun searchEntries(query: String): List<DiaryEntry> {
        val user = userDao.getCurrentUser() ?: return emptyList()
        return diaryEntryDao.searchEntries(user.id, query).map { it.toDomain() }
    }
    
    override suspend fun saveEntry(entry: DiaryEntry): NetworkResult<DiaryEntry> {
        val user = userDao.getCurrentUser() ?: return NetworkResult.Error(
            com.example.geekdiary.data.remote.NetworkException.Unauthorized
        )

        try {
            // Get existing entry for asset comparison
            val existingEntry = diaryEntryDao.getEntryByDate(user.id, entry.date.format(dateFormatter))?.toDomain()

            // Transform entry content for sync (convert local URLs to backend URLs)
            val baseUrl = settingsManager.getServerUrl()
            val entryForSync = entry.copy(
                body = markdownLinkTransformer.transformForSync(entry.body, baseUrl)
            )

            // Save locally first (offline-first approach)
            val localEntry = entry.copy(isLocal = true, needsSync = true)
            diaryEntryDao.insertEntry(localEntry.toEntity(user.id))

            // Process asset references
            if (existingEntry != null) {
                assetReferenceTracker.updateAssetReferences(existingEntry, entry)
            } else {
                assetReferenceTracker.trackAssetReferences(entry)
            }

            // Try to sync with remote
            return when (val result = safeApiCall { diaryApiService.putItem(entryForSync.toRequestDto()) }) {
                is NetworkResult.Success -> {
                    val syncedEntry = result.data.toDomain().copy(isLocal = false, needsSync = false)
                    diaryEntryDao.insertEntry(syncedEntry.toEntity(user.id))

                    // Transform for display and return
                    val displayEntry = transformEntryForDisplay(syncedEntry)
                    NetworkResult.Success(displayEntry)
                }
                is NetworkResult.Error -> {
                    // Entry is saved locally, return success but indicate sync needed
                    val displayEntry = transformEntryForDisplay(localEntry)
                    NetworkResult.Success(displayEntry)
                }
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        } catch (e: Exception) {
            return NetworkResult.Error(
                com.example.geekdiary.data.remote.NetworkException.UnknownError(e)
            )
        }
    }
    
    override suspend fun deleteEntry(date: LocalDate): NetworkResult<Unit> {
        val user = userDao.getCurrentUser() ?: return NetworkResult.Error(
            com.example.geekdiary.data.remote.NetworkException.Unauthorized
        )

        try {
            // Get the entry before deleting for asset cleanup
            val existingEntry = diaryEntryDao.getEntryByDate(user.id, date.format(dateFormatter))?.toDomain()

            // Delete locally first
            diaryEntryDao.deleteEntryByDate(user.id, date.format(dateFormatter))

            // Clean up asset references
            if (existingEntry != null) {
                assetReferenceTracker.cleanupAssetReferences(existingEntry)
            }

            // TODO: Add to pending changes for sync
            return NetworkResult.Success(Unit)
        } catch (e: Exception) {
            return NetworkResult.Error(
                com.example.geekdiary.data.remote.NetworkException.UnknownError(e)
            )
        }
    }
    
    override suspend fun syncEntries(): NetworkResult<Unit> {
        // TODO: Implement full sync logic
        return NetworkResult.Success(Unit)
    }
    
    override suspend fun getEntriesFromRemote(
        date: LocalDate?,
        search: String?,
        tags: String?
    ): NetworkResult<DiaryEntryList> {
        val dateString = date?.format(dateFormatter)
        return when (val result = safeApiCall { 
            diaryApiService.getItems(dateString, search, tags) 
        }) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> NetworkResult.Error(result.exception)
            is NetworkResult.Loading -> NetworkResult.Loading()
        }
    }

    /**
     * Transform diary entry for display by converting backend URLs to local paths
     * @param entry The diary entry to transform
     * @return Transformed diary entry with local asset paths where available
     */
    private suspend fun transformEntryForDisplay(entry: DiaryEntry): DiaryEntry {
        return try {
            val baseUrl = settingsManager.getServerUrl()
            val transformedBody = markdownLinkTransformer.transformForDisplay(entry.body, baseUrl)
            entry.copy(body = transformedBody)
        } catch (e: Exception) {
            println("Error transforming entry for display: ${e.message}")
            entry
        }
    }
}
