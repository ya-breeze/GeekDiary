# Synchronization API Design Analysis

## Overview
Comprehensive analysis of the synchronization API design, change tracking mechanism, incremental sync implementation, and mobile offline sync considerations.

## Synchronization Architecture

### 1. Change Tracking System
**Implementation**: `pkg/database/models/item_change.go`

```go
type ItemChange struct {
    ID            uint          `gorm:"primaryKey;autoIncrement"`
    UserID        string        `gorm:"index;not null"`
    Date          string        `gorm:"index;not null"`
    OperationType OperationType `gorm:"type:varchar(10);not null"`
    Timestamp     time.Time     `gorm:"index;not null;default:CURRENT_TIMESTAMP"`
    ItemSnapshot  *Item         `gorm:"embedded;embeddedPrefix:item_"`
    Metadata      StringList    `gorm:"type:json"`
}
```

**Key Design Features**:
- **Sequential ID**: Auto-incrementing primary key for ordered change tracking
- **User Isolation**: Changes are isolated per user via UserID index
- **Operation Types**: "created", "updated", "deleted" for comprehensive change tracking
- **Timestamp**: Server-side timestamp for change ordering and conflict resolution
- **Item Snapshot**: Complete item state after the change (null for deletions)
- **Metadata**: Extensible metadata array for change context (app version, device info)

### 2. Change Recording Mechanism
**Implementation**: `pkg/database/storage.go`

```go
func (s *storage) CreateChangeRecord(userID, date string, operationType models.OperationType,
    itemSnapshot *models.Item, metadata []string) error {
    
    change := &models.ItemChange{
        UserID:        userID,
        Date:          date,
        OperationType: operationType,
        Timestamp:     time.Now(),
        ItemSnapshot:  itemSnapshot,
        Metadata:      models.StringList(metadata),
    }
    
    return s.db.Create(change).Error
}
```

**Change Recording Strategy**:
- Changes are recorded automatically on every item modification
- Each change includes complete item snapshot for conflict resolution
- Metadata can include app version, device ID, sync source
- Changes are immutable once created (append-only log)

### 3. Incremental Sync API
**Endpoint**: `GET /v1/sync/changes`
**Implementation**: `pkg/server/api/api_sync_service.go`

```go
func (s *SyncAPIServiceImpl) GetChanges(ctx context.Context, since int32, limit int32) (goserver.ImplResponse, error) {
    // Validate parameters
    if limit <= 0 || limit > 1000 {
        limit = 100 // default limit
    }
    
    // Get changes from database
    sinceUint := uint(since)
    if since < 0 {
        sinceUint = 0
    }
    
    changes, err := s.db.GetChangesSince(userID, sinceUint, int(limit))
    
    // Determine pagination
    hasMore := len(changes) == int(limit)
    var nextID int32
    if hasMore && len(changes) > 0 {
        nextID = int32(changes[len(changes)-1].ID)
    }
    
    return goserver.Response(200, goserver.SyncResponse{
        Changes: responseChanges,
        HasMore: hasMore,
        NextId:  nextID,
    }), nil
}
```

**API Parameters**:
- `since` (optional): Get changes after this change ID (exclusive)
- `limit` (optional): Maximum changes to return (default: 100, max: 1000)

**Response Structure**:
```json
{
  "changes": [
    {
      "id": 123,
      "userId": "user-123",
      "date": "2024-01-15",
      "operationType": "updated",
      "timestamp": "2024-01-15T10:30:00Z",
      "itemSnapshot": {
        "date": "2024-01-15",
        "title": "Updated entry",
        "body": "Content...",
        "tags": ["personal"]
      },
      "metadata": ["mobile-app", "v1.2.3"]
    }
  ],
  "hasMore": true,
  "nextId": 456
}
```

## Mobile Sync Implementation Strategy

### 1. Sync State Management
```typescript
interface SyncState {
  lastSyncId: number;
  lastSyncTimestamp: Date;
  isSyncing: boolean;
  pendingChanges: PendingChange[];
  syncError: string | null;
}

class SyncManager {
  private syncState: SyncState;
  private apiClient: APIClient;
  private localDB: LocalDatabase;
  
  async performIncrementalSync(): Promise<SyncResult> {
    if (this.syncState.isSyncing) {
      return { success: false, error: 'Sync already in progress' };
    }
    
    this.syncState.isSyncing = true;
    
    try {
      // 1. Get remote changes since last sync
      const remoteChanges = await this.fetchRemoteChanges();
      
      // 2. Apply remote changes to local database
      await this.applyRemoteChanges(remoteChanges);
      
      // 3. Push local changes to server
      await this.pushLocalChanges();
      
      // 4. Update sync state
      this.updateSyncState();
      
      return { success: true };
    } catch (error) {
      this.syncState.syncError = error.message;
      return { success: false, error: error.message };
    } finally {
      this.syncState.isSyncing = false;
    }
  }
  
  private async fetchRemoteChanges(): Promise<SyncChangeResponse[]> {
    let allChanges: SyncChangeResponse[] = [];
    let since = this.syncState.lastSyncId;
    let hasMore = true;
    
    while (hasMore) {
      const response = await this.apiClient.syncChanges(since, 100);
      allChanges = allChanges.concat(response.changes);
      
      hasMore = response.hasMore;
      if (hasMore) {
        since = response.nextId;
      }
    }
    
    return allChanges;
  }
}
```

### 2. Conflict Resolution Strategy
```typescript
enum ConflictResolutionStrategy {
  LAST_WRITE_WINS = 'last-write-wins',
  USER_CHOICE = 'user-choice',
  MERGE = 'merge'
}

interface ConflictResolution {
  strategy: ConflictResolutionStrategy;
  localVersion: ItemsResponse;
  remoteVersion: ItemsResponse;
  conflictTimestamp: Date;
}

class ConflictResolver {
  async resolveConflict(conflict: ConflictResolution): Promise<ItemsResponse> {
    switch (conflict.strategy) {
      case ConflictResolutionStrategy.LAST_WRITE_WINS:
        return this.lastWriteWins(conflict);
      
      case ConflictResolutionStrategy.USER_CHOICE:
        return await this.promptUserChoice(conflict);
      
      case ConflictResolutionStrategy.MERGE:
        return this.mergeEntries(conflict);
    }
  }
  
  private lastWriteWins(conflict: ConflictResolution): ItemsResponse {
    // Server timestamp wins by default
    return conflict.remoteVersion;
  }
  
  private async promptUserChoice(conflict: ConflictResolution): Promise<ItemsResponse> {
    return new Promise((resolve) => {
      showConflictResolutionModal({
        localVersion: conflict.localVersion,
        remoteVersion: conflict.remoteVersion,
        onResolve: (chosenVersion) => resolve(chosenVersion)
      });
    });
  }
  
  private mergeEntries(conflict: ConflictResolution): ItemsResponse {
    // Simple merge strategy - combine non-conflicting fields
    return {
      date: conflict.remoteVersion.date,
      title: conflict.remoteVersion.title, // Use remote title
      body: this.mergeContent(conflict.localVersion.body, conflict.remoteVersion.body),
      tags: this.mergeTags(conflict.localVersion.tags, conflict.remoteVersion.tags)
    };
  }
  
  private mergeContent(localBody: string, remoteBody: string): string {
    // Simple merge - append local changes if different
    if (localBody === remoteBody) return remoteBody;
    
    return `${remoteBody}\n\n--- Local Changes ---\n${localBody}`;
  }
  
  private mergeTags(localTags: string[], remoteTags: string[]): string[] {
    // Merge tags by combining unique values
    const combined = [...new Set([...localTags, ...remoteTags])];
    return combined.sort();
  }
}
```

### 3. Offline-First Data Architecture
```typescript
interface LocalEntry {
  id: string;
  userId: string;
  date: string;
  title: string;
  body: string;
  tags: string; // JSON string
  createdAt: Date;
  updatedAt: Date;
  isSynced: boolean;
  lastSyncId?: number;
}

interface PendingChange {
  id: string;
  userId: string;
  entryDate: string;
  operationType: 'create' | 'update' | 'delete';
  data: any; // JSON payload
  createdAt: Date;
  retryCount: number;
}

class OfflineDataManager {
  private localDB: SQLiteDatabase;
  
  async saveEntry(entry: ItemsRequest): Promise<void> {
    // Always save to local database first
    const localEntry = this.transformToLocal(entry);
    await this.localDB.upsertEntry(localEntry);
    
    // Queue for sync
    await this.queueChange({
      userId: getCurrentUserId(),
      entryDate: entry.date,
      operationType: localEntry.id ? 'update' : 'create',
      data: entry,
      createdAt: new Date(),
      retryCount: 0
    });
    
    // Attempt immediate sync if online
    if (await this.isOnline()) {
      this.syncManager.performIncrementalSync();
    }
  }
  
  async getEntry(date: string): Promise<ItemsResponse | null> {
    // Always read from local database
    const localEntry = await this.localDB.getEntry(getCurrentUserId(), date);
    if (!localEntry) return null;
    
    return this.transformToAPI(localEntry);
  }
  
  async deleteEntry(date: string): Promise<void> {
    // Mark as deleted locally
    await this.localDB.markDeleted(getCurrentUserId(), date);
    
    // Queue deletion for sync
    await this.queueChange({
      userId: getCurrentUserId(),
      entryDate: date,
      operationType: 'delete',
      data: { date },
      createdAt: new Date(),
      retryCount: 0
    });
  }
}
```

### 4. Background Sync Service
```typescript
class BackgroundSyncService {
  private syncInterval: number = 5 * 60 * 1000; // 5 minutes
  private maxRetries: number = 3;
  private isRunning: boolean = false;
  
  start(): void {
    if (this.isRunning) return;
    
    this.isRunning = true;
    this.scheduleNextSync();
  }
  
  stop(): void {
    this.isRunning = false;
  }
  
  private scheduleNextSync(): void {
    if (!this.isRunning) return;
    
    setTimeout(async () => {
      try {
        await this.performBackgroundSync();
      } catch (error) {
        console.error('Background sync failed:', error);
      }
      
      this.scheduleNextSync();
    }, this.syncInterval);
  }
  
  private async performBackgroundSync(): Promise<void> {
    // Only sync if online and not already syncing
    if (!await this.isOnline() || this.syncManager.isSyncing) {
      return;
    }
    
    // Check if there are pending changes
    const pendingChanges = await this.localDB.getPendingChanges();
    if (pendingChanges.length === 0) {
      return;
    }
    
    // Perform sync
    const result = await this.syncManager.performIncrementalSync();
    
    if (!result.success) {
      // Increment retry count for failed changes
      await this.handleSyncFailure(pendingChanges);
    }
  }
  
  private async handleSyncFailure(pendingChanges: PendingChange[]): Promise<void> {
    for (const change of pendingChanges) {
      change.retryCount++;
      
      if (change.retryCount >= this.maxRetries) {
        // Mark as failed, require manual intervention
        await this.localDB.markChangeFailed(change.id);
        this.notifyUser(`Sync failed for entry ${change.entryDate}`);
      } else {
        // Update retry count
        await this.localDB.updateChange(change);
      }
    }
  }
}
```

## Sync Performance Optimizations

### 1. Batch Processing
```typescript
class BatchSyncProcessor {
  private batchSize: number = 50;
  
  async processSyncBatch(changes: SyncChangeResponse[]): Promise<void> {
    // Process changes in batches to avoid memory issues
    for (let i = 0; i < changes.length; i += this.batchSize) {
      const batch = changes.slice(i, i + this.batchSize);
      await this.processBatch(batch);
      
      // Allow UI to update between batches
      await this.delay(10);
    }
  }
  
  private async processBatch(changes: SyncChangeResponse[]): Promise<void> {
    const transaction = await this.localDB.beginTransaction();
    
    try {
      for (const change of changes) {
        await this.applyChange(change, transaction);
      }
      
      await transaction.commit();
    } catch (error) {
      await transaction.rollback();
      throw error;
    }
  }
}
```

### 2. Delta Sync Optimization
```typescript
class DeltaSyncOptimizer {
  async optimizeSync(changes: SyncChangeResponse[]): Promise<SyncChangeResponse[]> {
    // Remove redundant changes (multiple updates to same entry)
    const latestChanges = new Map<string, SyncChangeResponse>();
    
    for (const change of changes) {
      const key = `${change.userId}-${change.date}`;
      const existing = latestChanges.get(key);
      
      if (!existing || change.id > existing.id) {
        latestChanges.set(key, change);
      }
    }
    
    return Array.from(latestChanges.values());
  }
}
```

### 3. Network-Aware Sync
```typescript
class NetworkAwareSyncManager {
  private networkType: 'wifi' | 'cellular' | 'none' = 'none';
  
  async performSyncBasedOnNetwork(): Promise<void> {
    this.networkType = await this.getNetworkType();
    
    switch (this.networkType) {
      case 'wifi':
        // Full sync with large batches
        await this.performFullSync({ batchSize: 100 });
        break;
        
      case 'cellular':
        // Conservative sync with small batches
        await this.performIncrementalSync({ batchSize: 20 });
        break;
        
      case 'none':
        // Queue for later sync
        await this.queueForLaterSync();
        break;
    }
  }
}
```

## Error Handling and Recovery

### 1. Sync Error Types
```typescript
enum SyncErrorType {
  NETWORK_ERROR = 'NETWORK_ERROR',
  AUTHENTICATION_ERROR = 'AUTHENTICATION_ERROR',
  CONFLICT_ERROR = 'CONFLICT_ERROR',
  SERVER_ERROR = 'SERVER_ERROR',
  DATA_CORRUPTION = 'DATA_CORRUPTION'
}

class SyncErrorHandler {
  handleSyncError(error: SyncError): SyncRecoveryAction {
    switch (error.type) {
      case SyncErrorType.NETWORK_ERROR:
        return { action: 'retry', delay: this.getExponentialBackoff(error.retryCount) };
        
      case SyncErrorType.AUTHENTICATION_ERROR:
        return { action: 'reauthenticate' };
        
      case SyncErrorType.CONFLICT_ERROR:
        return { action: 'resolve_conflict', conflictData: error.conflictData };
        
      case SyncErrorType.SERVER_ERROR:
        return { action: 'retry', delay: 60000 }; // 1 minute
        
      case SyncErrorType.DATA_CORRUPTION:
        return { action: 'full_resync' };
    }
  }
  
  private getExponentialBackoff(retryCount: number): number {
    return Math.min(1000 * Math.pow(2, retryCount), 30000); // Max 30 seconds
  }
}
```

### 2. Data Integrity Validation
```typescript
class DataIntegrityValidator {
  async validateSyncData(changes: SyncChangeResponse[]): Promise<ValidationResult> {
    const errors: string[] = [];
    
    for (const change of changes) {
      // Validate change structure
      if (!this.isValidChange(change)) {
        errors.push(`Invalid change structure: ${change.id}`);
        continue;
      }
      
      // Validate item snapshot
      if (change.itemSnapshot && !this.isValidItem(change.itemSnapshot)) {
        errors.push(`Invalid item snapshot: ${change.id}`);
      }
      
      // Validate operation type
      if (!['created', 'updated', 'deleted'].includes(change.operationType)) {
        errors.push(`Invalid operation type: ${change.operationType}`);
      }
    }
    
    return {
      isValid: errors.length === 0,
      errors: errors
    };
  }
}
```

## Mobile Sync Best Practices

### 1. Sync Scheduling
- Sync on app launch and resume
- Background sync every 5-15 minutes when online
- Immediate sync after local changes
- Respect battery optimization settings

### 2. Conflict Prevention
- Use optimistic locking where possible
- Implement last-modified timestamps
- Provide real-time collaboration indicators
- Cache frequently accessed data

### 3. User Experience
- Show sync status indicators
- Provide offline mode feedback
- Allow manual sync triggers
- Display conflict resolution options clearly

### 4. Performance Considerations
- Limit sync batch sizes based on network type
- Use compression for large payloads
- Implement progressive sync for initial setup
- Monitor memory usage during sync operations
