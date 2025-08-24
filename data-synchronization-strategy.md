# Data Synchronization Strategy

## Overview
Comprehensive synchronization strategy for the mobile diary application, ensuring data consistency between local storage and remote server while providing robust offline capabilities and conflict resolution.

## Synchronization Architecture

### 1. Offline-First Approach
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Mobile App    │    │  Local Database │    │  Remote Server  │
│                 │    │   (SQLite)      │    │   (Backend)     │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ • Always read   │◄──►│ • Single source │◄──►│ • Authoritative │
│   from local    │    │   of truth      │    │   data store    │
│ • Write local   │    │ • Immediate     │    │ • Change        │
│   first         │    │   consistency   │    │   tracking      │
│ • Sync in       │    │ • Sync queue    │    │ • Conflict      │
│   background    │    │ • Conflict      │    │   resolution    │
└─────────────────┘    │   resolution    │    └─────────────────┘
                       └─────────────────┘
```

### 2. Data Flow Patterns

#### Write Operations (Create/Update/Delete)
```
User Action → Local Database → Sync Queue → Background Sync → Remote Server
     ↓              ↓              ↓              ↓              ↓
Immediate UI    Immediate      Queued for     Network         Server
Update          Storage        Sync           Available       Update
```

#### Read Operations
```
User Request → Local Database → UI Update
                    ↓
            Background Sync Check
                    ↓
            Remote Changes → Local Update → UI Refresh
```

## Local Database Schema Design

### 1. Core Tables
```sql
-- Users table
CREATE TABLE users (
  id TEXT PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  start_date TEXT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Diary entries table
CREATE TABLE entries (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  date TEXT NOT NULL,
  title TEXT NOT NULL,
  body TEXT NOT NULL,
  tags TEXT, -- JSON array
  previous_date TEXT,
  next_date TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_synced BOOLEAN DEFAULT FALSE,
  needs_sync BOOLEAN DEFAULT FALSE,
  last_sync_id INTEGER,
  FOREIGN KEY (user_id) REFERENCES users (id),
  UNIQUE(user_id, date)
);

-- Assets table
CREATE TABLE assets (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  filename TEXT NOT NULL,
  local_path TEXT,
  remote_url TEXT,
  file_size INTEGER,
  mime_type TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_synced BOOLEAN DEFAULT FALSE,
  needs_sync BOOLEAN DEFAULT FALSE,
  FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Sync state tracking
CREATE TABLE sync_state (
  user_id TEXT PRIMARY KEY,
  last_sync_id INTEGER DEFAULT 0,
  last_sync_timestamp DATETIME,
  sync_in_progress BOOLEAN DEFAULT FALSE,
  FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Pending changes queue
CREATE TABLE pending_changes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id TEXT NOT NULL,
  entry_date TEXT NOT NULL,
  operation_type TEXT NOT NULL, -- 'create', 'update', 'delete'
  data TEXT, -- JSON payload
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  retry_count INTEGER DEFAULT 0,
  max_retries INTEGER DEFAULT 3,
  next_retry_at DATETIME,
  error_message TEXT,
  FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Conflict resolution table
CREATE TABLE sync_conflicts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id TEXT NOT NULL,
  entry_date TEXT NOT NULL,
  local_version TEXT, -- JSON
  remote_version TEXT, -- JSON
  conflict_type TEXT, -- 'concurrent_edit', 'delete_conflict'
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  resolved BOOLEAN DEFAULT FALSE,
  resolution TEXT, -- JSON of resolved version
  FOREIGN KEY (user_id) REFERENCES users (id)
);
```

### 2. Indexes for Performance
```sql
-- Performance indexes
CREATE INDEX idx_entries_user_date ON entries(user_id, date);
CREATE INDEX idx_entries_needs_sync ON entries(needs_sync) WHERE needs_sync = TRUE;
CREATE INDEX idx_pending_changes_user ON pending_changes(user_id);
CREATE INDEX idx_pending_changes_retry ON pending_changes(next_retry_at) WHERE retry_count < max_retries;
CREATE INDEX idx_assets_user ON assets(user_id);
CREATE INDEX idx_assets_needs_sync ON assets(needs_sync) WHERE needs_sync = TRUE;
```

## Synchronization Implementation

### 1. Sync Manager Architecture
```typescript
interface SyncManager {
  // Core sync operations
  performFullSync(): Promise<SyncResult>;
  performIncrementalSync(): Promise<SyncResult>;
  syncPendingChanges(): Promise<SyncResult>;
  
  // Sync scheduling
  startBackgroundSync(): void;
  stopBackgroundSync(): void;
  scheduleSyncCheck(): void;
  
  // Conflict resolution
  resolveConflicts(): Promise<ConflictResolution[]>;
  
  // Status and monitoring
  getSyncStatus(): SyncStatus;
  onSyncStatusChange(callback: (status: SyncStatus) => void): void;
}

class SyncManagerImpl implements SyncManager {
  private apiClient: APIClient;
  private database: Database;
  private conflictResolver: ConflictResolver;
  private syncInterval: number = 5 * 60 * 1000; // 5 minutes
  private isRunning: boolean = false;
  
  async performIncrementalSync(): Promise<SyncResult> {
    const userId = await this.getCurrentUserId();
    const syncState = await this.database.getSyncState(userId);
    
    try {
      // 1. Fetch remote changes since last sync
      const remoteChanges = await this.fetchRemoteChanges(syncState.lastSyncId);
      
      // 2. Apply remote changes to local database
      const conflicts = await this.applyRemoteChanges(remoteChanges);
      
      // 3. Push local changes to server
      await this.pushLocalChanges();
      
      // 4. Update sync state
      await this.updateSyncState(remoteChanges);
      
      return {
        success: true,
        changesApplied: remoteChanges.length,
        changesPushed: await this.getPendingChangesCount(),
        conflicts: conflicts.length
      };
    } catch (error) {
      return {
        success: false,
        error: error.message,
        changesApplied: 0,
        changesPushed: 0,
        conflicts: 0
      };
    }
  }
  
  private async fetchRemoteChanges(since: number): Promise<SyncChange[]> {
    let allChanges: SyncChange[] = [];
    let currentSince = since;
    let hasMore = true;
    
    while (hasMore) {
      const response = await this.apiClient.syncChanges(currentSince, 100);
      allChanges = allChanges.concat(response.changes);
      
      hasMore = response.hasMore;
      if (hasMore) {
        currentSince = response.nextId;
      }
    }
    
    return allChanges;
  }
  
  private async applyRemoteChanges(changes: SyncChange[]): Promise<Conflict[]> {
    const conflicts: Conflict[] = [];
    
    for (const change of changes) {
      try {
        const conflict = await this.applyChange(change);
        if (conflict) {
          conflicts.push(conflict);
        }
      } catch (error) {
        console.error(`Failed to apply change ${change.id}:`, error);
      }
    }
    
    return conflicts;
  }
  
  private async applyChange(change: SyncChange): Promise<Conflict | null> {
    const localEntry = await this.database.getEntry(change.userId, change.date);
    
    // Check for conflicts
    if (localEntry && localEntry.needsSync && change.operationType !== 'deleted') {
      // Concurrent modification detected
      return await this.handleConflict(localEntry, change);
    }
    
    // Apply change based on operation type
    switch (change.operationType) {
      case 'created':
      case 'updated':
        await this.database.upsertEntry({
          ...change.itemSnapshot,
          isSynced: true,
          needsSync: false,
          lastSyncId: change.id
        });
        break;
        
      case 'deleted':
        await this.database.deleteEntry(change.userId, change.date);
        break;
    }
    
    return null;
  }
}
```

### 2. Conflict Resolution Strategy
```typescript
enum ConflictResolutionStrategy {
  LAST_WRITE_WINS = 'last_write_wins',
  USER_CHOICE = 'user_choice',
  MERGE_CONTENT = 'merge_content',
  KEEP_BOTH = 'keep_both'
}

interface ConflictResolver {
  resolveConflict(conflict: Conflict): Promise<ConflictResolution>;
  getResolutionStrategy(conflict: Conflict): ConflictResolutionStrategy;
}

class ConflictResolverImpl implements ConflictResolver {
  async resolveConflict(conflict: Conflict): Promise<ConflictResolution> {
    const strategy = this.getResolutionStrategy(conflict);
    
    switch (strategy) {
      case ConflictResolutionStrategy.LAST_WRITE_WINS:
        return this.lastWriteWins(conflict);
        
      case ConflictResolutionStrategy.USER_CHOICE:
        return await this.promptUserChoice(conflict);
        
      case ConflictResolutionStrategy.MERGE_CONTENT:
        return this.mergeContent(conflict);
        
      case ConflictResolutionStrategy.KEEP_BOTH:
        return this.keepBoth(conflict);
    }
  }
  
  private lastWriteWins(conflict: Conflict): ConflictResolution {
    // Compare timestamps and choose the later one
    const localTime = new Date(conflict.localVersion.updatedAt);
    const remoteTime = new Date(conflict.remoteVersion.timestamp);
    
    const winner = remoteTime > localTime ? conflict.remoteVersion : conflict.localVersion;
    
    return {
      strategy: ConflictResolutionStrategy.LAST_WRITE_WINS,
      resolvedVersion: winner,
      requiresUserAction: false
    };
  }
  
  private async promptUserChoice(conflict: Conflict): Promise<ConflictResolution> {
    return new Promise((resolve) => {
      // Show conflict resolution UI
      showConflictModal({
        localVersion: conflict.localVersion,
        remoteVersion: conflict.remoteVersion,
        onResolve: (chosenVersion, customResolution) => {
          resolve({
            strategy: ConflictResolutionStrategy.USER_CHOICE,
            resolvedVersion: customResolution || chosenVersion,
            requiresUserAction: false
          });
        }
      });
    });
  }
  
  private mergeContent(conflict: Conflict): ConflictResolution {
    const merged = {
      ...conflict.remoteVersion,
      title: conflict.localVersion.title, // Keep local title
      body: this.mergeTextContent(
        conflict.localVersion.body,
        conflict.remoteVersion.body
      ),
      tags: this.mergeTags(
        conflict.localVersion.tags,
        conflict.remoteVersion.tags
      )
    };
    
    return {
      strategy: ConflictResolutionStrategy.MERGE_CONTENT,
      resolvedVersion: merged,
      requiresUserAction: false
    };
  }
  
  private mergeTextContent(localBody: string, remoteBody: string): string {
    if (localBody === remoteBody) return remoteBody;
    
    // Simple merge strategy - append local changes
    return `${remoteBody}\n\n--- Local Changes ---\n${localBody}`;
  }
  
  private mergeTags(localTags: string[], remoteTags: string[]): string[] {
    // Merge tags by combining unique values
    const combined = [...new Set([...localTags, ...remoteTags])];
    return combined.sort();
  }
}
```

### 3. Background Sync Service
```typescript
class BackgroundSyncService {
  private syncManager: SyncManager;
  private networkMonitor: NetworkMonitor;
  private syncInterval: number;
  private isRunning: boolean = false;
  private timeoutId?: NodeJS.Timeout;
  
  constructor(syncManager: SyncManager, networkMonitor: NetworkMonitor) {
    this.syncManager = syncManager;
    this.networkMonitor = networkMonitor;
    this.syncInterval = this.calculateSyncInterval();
  }
  
  start(): void {
    if (this.isRunning) return;
    
    this.isRunning = true;
    this.scheduleNextSync();
    
    // Listen for network changes
    this.networkMonitor.onNetworkChange((isOnline) => {
      if (isOnline && this.hasPendingChanges()) {
        this.performImmediateSync();
      }
    });
    
    // Listen for app state changes
    AppState.addEventListener('change', (nextAppState) => {
      if (nextAppState === 'active') {
        this.performImmediateSync();
      }
    });
  }
  
  stop(): void {
    this.isRunning = false;
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
  }
  
  private scheduleNextSync(): void {
    if (!this.isRunning) return;
    
    this.timeoutId = setTimeout(async () => {
      try {
        if (await this.shouldPerformSync()) {
          await this.syncManager.performIncrementalSync();
        }
      } catch (error) {
        console.error('Background sync failed:', error);
      }
      
      this.scheduleNextSync();
    }, this.syncInterval);
  }
  
  private async shouldPerformSync(): Promise<boolean> {
    // Check network connectivity
    if (!await this.networkMonitor.isOnline()) {
      return false;
    }
    
    // Check if sync is already in progress
    if (this.syncManager.getSyncStatus().isSyncing) {
      return false;
    }
    
    // Check if there are pending changes or if enough time has passed
    const hasPendingChanges = await this.hasPendingChanges();
    const timeSinceLastSync = await this.getTimeSinceLastSync();
    const shouldSyncByTime = timeSinceLastSync > this.syncInterval;
    
    return hasPendingChanges || shouldSyncByTime;
  }
  
  private calculateSyncInterval(): number {
    const networkType = this.networkMonitor.getNetworkType();
    
    switch (networkType) {
      case 'wifi':
        return 2 * 60 * 1000; // 2 minutes on WiFi
      case 'cellular':
        return 10 * 60 * 1000; // 10 minutes on cellular
      default:
        return 5 * 60 * 1000; // 5 minutes default
    }
  }
}
```

## Network Failure Handling

### 1. Retry Strategy
```typescript
interface RetryConfig {
  maxRetries: number;
  baseDelay: number;
  maxDelay: number;
  backoffMultiplier: number;
}

class RetryManager {
  private config: RetryConfig = {
    maxRetries: 3,
    baseDelay: 1000, // 1 second
    maxDelay: 30000, // 30 seconds
    backoffMultiplier: 2
  };
  
  async executeWithRetry<T>(
    operation: () => Promise<T>,
    context: string
  ): Promise<T> {
    let lastError: Error;
    
    for (let attempt = 0; attempt <= this.config.maxRetries; attempt++) {
      try {
        return await operation();
      } catch (error) {
        lastError = error;
        
        if (attempt === this.config.maxRetries) {
          break; // Max retries reached
        }
        
        if (!this.isRetryableError(error)) {
          throw error; // Don't retry non-retryable errors
        }
        
        const delay = this.calculateDelay(attempt);
        console.log(`${context} failed (attempt ${attempt + 1}), retrying in ${delay}ms`);
        
        await this.delay(delay);
      }
    }
    
    throw new Error(`${context} failed after ${this.config.maxRetries} retries: ${lastError.message}`);
  }
  
  private isRetryableError(error: any): boolean {
    // Network errors are retryable
    if (error.code === 'NETWORK_ERROR') return true;
    
    // Server errors (5xx) are retryable
    if (error.response?.status >= 500) return true;
    
    // Rate limiting is retryable
    if (error.response?.status === 429) return true;
    
    // Client errors (4xx) are generally not retryable
    if (error.response?.status >= 400 && error.response?.status < 500) {
      return false;
    }
    
    return true;
  }
  
  private calculateDelay(attempt: number): number {
    const delay = this.config.baseDelay * Math.pow(this.config.backoffMultiplier, attempt);
    return Math.min(delay, this.config.maxDelay);
  }
  
  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
```

### 2. Network Monitoring
```typescript
class NetworkMonitor {
  private listeners: ((isOnline: boolean) => void)[] = [];
  private currentStatus: boolean = true;
  
  constructor() {
    this.setupNetworkListener();
  }
  
  private setupNetworkListener(): void {
    NetInfo.addEventListener(state => {
      const isOnline = state.isConnected && state.isInternetReachable;
      
      if (isOnline !== this.currentStatus) {
        this.currentStatus = isOnline;
        this.notifyListeners(isOnline);
      }
    });
  }
  
  async isOnline(): Promise<boolean> {
    const state = await NetInfo.fetch();
    return state.isConnected && state.isInternetReachable;
  }
  
  getNetworkType(): 'wifi' | 'cellular' | 'none' {
    // Implementation depends on platform
    return 'wifi'; // Simplified
  }
  
  onNetworkChange(callback: (isOnline: boolean) => void): void {
    this.listeners.push(callback);
  }
  
  private notifyListeners(isOnline: boolean): void {
    this.listeners.forEach(callback => callback(isOnline));
  }
}
```

## Data Consistency Guarantees

### 1. ACID Properties for Local Operations
- **Atomicity**: All local operations are wrapped in database transactions
- **Consistency**: Database constraints ensure data integrity
- **Isolation**: Concurrent operations are properly serialized
- **Durability**: Changes are immediately persisted to disk

### 2. Eventual Consistency for Sync Operations
- Local changes are immediately visible to the user
- Remote changes are eventually synchronized
- Conflicts are detected and resolved automatically or with user input
- System converges to consistent state across all devices

### 3. Data Validation
```typescript
class DataValidator {
  validateEntry(entry: Entry): ValidationResult {
    const errors: string[] = [];
    
    // Required field validation
    if (!entry.date || !this.isValidDate(entry.date)) {
      errors.push('Valid date is required');
    }
    
    if (!entry.title || entry.title.trim().length === 0) {
      errors.push('Title is required');
    }
    
    if (!entry.body || entry.body.trim().length === 0) {
      errors.push('Entry content is required');
    }
    
    // Length validation
    if (entry.title.length > 200) {
      errors.push('Title too long (max 200 characters)');
    }
    
    if (entry.body.length > 50000) {
      errors.push('Entry content too long (max 50,000 characters)');
    }
    
    // Tag validation
    if (entry.tags && entry.tags.length > 20) {
      errors.push('Too many tags (max 20)');
    }
    
    return {
      isValid: errors.length === 0,
      errors: errors
    };
  }
  
  private isValidDate(dateString: string): boolean {
    const date = new Date(dateString);
    return date instanceof Date && !isNaN(date.getTime());
  }
}
```

## Cache Invalidation Strategy

### 1. Time-Based Invalidation
```typescript
interface CacheEntry<T> {
  data: T;
  timestamp: number;
  ttl: number; // Time to live in milliseconds
}

class CacheManager {
  private cache = new Map<string, CacheEntry<any>>();
  private defaultTTL = 5 * 60 * 1000; // 5 minutes
  
  set<T>(key: string, data: T, ttl: number = this.defaultTTL): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttl
    });
  }
  
  get<T>(key: string): T | null {
    const entry = this.cache.get(key);
    
    if (!entry) return null;
    
    // Check if expired
    if (Date.now() - entry.timestamp > entry.ttl) {
      this.cache.delete(key);
      return null;
    }
    
    return entry.data;
  }
  
  invalidate(key: string): void {
    this.cache.delete(key);
  }
  
  invalidatePattern(pattern: RegExp): void {
    for (const key of this.cache.keys()) {
      if (pattern.test(key)) {
        this.cache.delete(key);
      }
    }
  }
  
  clear(): void {
    this.cache.clear();
  }
}
```

### 2. Event-Based Invalidation
```typescript
class EventBasedCacheInvalidation {
  private cacheManager: CacheManager;
  
  constructor(cacheManager: CacheManager) {
    this.cacheManager = cacheManager;
    this.setupEventListeners();
  }
  
  private setupEventListeners(): void {
    // Invalidate entry cache when entry is updated
    EventBus.on('entry:updated', (entry: Entry) => {
      this.cacheManager.invalidate(`entry:${entry.date}`);
      this.cacheManager.invalidatePattern(/^search:/);
    });
    
    // Invalidate search cache when entries change
    EventBus.on('entry:created', () => {
      this.cacheManager.invalidatePattern(/^search:/);
    });
    
    // Clear all cache on logout
    EventBus.on('auth:logout', () => {
      this.cacheManager.clear();
    });
  }
}
```

This comprehensive synchronization strategy ensures robust data consistency, efficient conflict resolution, and reliable offline functionality for the mobile diary application.
