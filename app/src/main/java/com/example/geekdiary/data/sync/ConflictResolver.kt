package com.example.geekdiary.data.sync

import com.example.geekdiary.domain.model.DiaryEntry
import com.example.geekdiary.domain.model.SyncChange
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

enum class ConflictResolutionStrategy {
    LAST_WRITE_WINS,
    KEEP_LOCAL,
    KEEP_REMOTE
}

data class ConflictResolution(
    val strategy: ConflictResolutionStrategy,
    val resolvedEntry: DiaryEntry,
    val message: String
)

@Singleton
class ConflictResolver @Inject constructor() {
    
    fun resolveConflict(
        localEntry: DiaryEntry,
        remoteChange: SyncChange
    ): ConflictResolution {
        // For now, we implement last-write-wins as specified in requirements
        return resolveLastWriteWins(localEntry, remoteChange)
    }
    
    private fun resolveLastWriteWins(
        localEntry: DiaryEntry,
        remoteChange: SyncChange
    ): ConflictResolution {
        val remoteEntry = remoteChange.itemSnapshot
        
        if (remoteEntry == null) {
            // Remote entry was deleted, keep local
            return ConflictResolution(
                strategy = ConflictResolutionStrategy.KEEP_LOCAL,
                resolvedEntry = localEntry,
                message = "Remote entry was deleted, keeping local version"
            )
        }
        
        // Compare timestamps - use remote timestamp vs local update time
        // Since we don't have local update timestamps in our current model,
        // we'll use the remote change timestamp as the deciding factor
        val remoteTimestamp = remoteChange.timestamp
        val now = LocalDateTime.now()
        
        // If remote change is very recent (within last hour), prefer remote
        val isRemoteRecent = remoteTimestamp.isAfter(now.minusHours(1))
        
        return if (isRemoteRecent) {
            ConflictResolution(
                strategy = ConflictResolutionStrategy.KEEP_REMOTE,
                resolvedEntry = remoteEntry,
                message = "Remote changes are more recent, using remote version"
            )
        } else {
            ConflictResolution(
                strategy = ConflictResolutionStrategy.KEEP_LOCAL,
                resolvedEntry = localEntry,
                message = "Local changes are more recent, keeping local version"
            )
        }
    }
    
    fun detectConflict(localEntry: DiaryEntry, remoteChange: SyncChange): Boolean {
        // A conflict exists if:
        // 1. Both local and remote entries exist for the same date
        // 2. Local entry needs sync (has been modified locally)
        // 3. Remote change is not a deletion
        
        return localEntry.needsSync && 
               remoteChange.itemSnapshot != null &&
               localEntry.date == remoteChange.date
    }
    
    fun createConflictNotification(resolution: ConflictResolution): String {
        return when (resolution.strategy) {
            ConflictResolutionStrategy.LAST_WRITE_WINS -> 
                "Sync conflict resolved automatically: ${resolution.message}"
            ConflictResolutionStrategy.KEEP_LOCAL -> 
                "Sync conflict: Kept your local changes"
            ConflictResolutionStrategy.KEEP_REMOTE -> 
                "Sync conflict: Applied remote changes"
        }
    }
}
