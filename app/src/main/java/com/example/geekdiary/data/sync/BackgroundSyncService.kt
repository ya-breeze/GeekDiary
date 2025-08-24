package com.example.geekdiary.data.sync

import com.example.geekdiary.data.network.NetworkMonitor
import com.example.geekdiary.data.network.NetworkStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundSyncService @Inject constructor(
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor,
    private val assetSyncManager: AssetSyncManager
) {
    
    private var syncJob: Job? = null
    private var assetSyncJob: Job? = null
    private var networkMonitorJob: Job? = null
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isRunning = false
    private val syncInterval = 5 * 60 * 1000L // 5 minutes
    private val assetSyncInterval = 2 * 60 * 1000L // 2 minutes for asset sync
    
    fun startBackgroundSync() {
        if (isRunning) return
        
        isRunning = true
        
        // Start periodic sync
        syncJob = syncScope.launch {
            while (isActive) {
                try {
                    if (networkMonitor.isNetworkAvailable() && syncManager.hasPendingChanges()) {
                        syncManager.performIncrementalSync()
                    }
                } catch (e: Exception) {
                    // Log error but continue
                    println("Background sync error: ${e.message}")
                }

                delay(syncInterval)
            }
        }

        // Start periodic asset sync
        assetSyncJob = syncScope.launch {
            while (isActive) {
                try {
                    if (networkMonitor.isNetworkAvailable()) {
                        // Perform background asset sync
                        syncManager.performBackgroundAssetSync()

                        // Clean up completed downloads periodically
                        assetSyncManager.cleanupCompletedDownloads()
                    }
                } catch (e: Exception) {
                    // Log error but continue
                    println("Background asset sync error: ${e.message}")
                }

                delay(assetSyncInterval)
            }
        }
        
        // Monitor network changes
        networkMonitorJob = syncScope.launch {
            networkMonitor.networkStatus.collectLatest { status ->
                when (status) {
                    NetworkStatus.AVAILABLE -> {
                        // Network became available, trigger sync if needed
                        if (syncManager.hasPendingChanges()) {
                            delay(1000) // Small delay to ensure connection is stable
                            syncManager.performIncrementalSync()
                        }

                        // Also trigger asset sync when network becomes available
                        delay(2000) // Additional delay for asset sync
                        syncManager.performBackgroundAssetSync()
                    }
                    else -> {
                        // Network unavailable, sync will be retried when available
                    }
                }
            }
        }
    }
    
    fun stopBackgroundSync() {
        isRunning = false
        syncJob?.cancel()
        assetSyncJob?.cancel()
        networkMonitorJob?.cancel()
    }
    
    suspend fun performImmediateSync() {
        if (networkMonitor.isNetworkAvailable()) {
            syncManager.performIncrementalSync()
        }
    }

    suspend fun performImmediateAssetSync() {
        if (networkMonitor.isNetworkAvailable()) {
            syncManager.performBackgroundAssetSync()
        }
    }

    suspend fun retryFailedAssetDownloads() {
        if (networkMonitor.isNetworkAvailable()) {
            assetSyncManager.retryFailedDownloads()
        }
    }

    suspend fun getAssetSyncStats(): Triple<Int, Int, Int> {
        return assetSyncManager.getPendingDownloadStats()
    }

    fun isRunning(): Boolean = isRunning
}
