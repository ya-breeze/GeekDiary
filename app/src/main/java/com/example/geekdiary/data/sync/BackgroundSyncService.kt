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
    private val networkMonitor: NetworkMonitor
) {
    
    private var syncJob: Job? = null
    private var networkMonitorJob: Job? = null
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var isRunning = false
    private val syncInterval = 5 * 60 * 1000L // 5 minutes
    
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
        networkMonitorJob?.cancel()
    }
    
    suspend fun performImmediateSync() {
        if (networkMonitor.isNetworkAvailable()) {
            syncManager.performIncrementalSync()
        }
    }
    
    fun isRunning(): Boolean = isRunning
}
