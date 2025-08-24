# Data Models & Relationships Analysis

## Overview
Comprehensive documentation of all data models, entity relationships, and data flow patterns in the diary application for mobile app implementation.

## Core Data Models

### 1. Entity Base Model
**Purpose**: Base model for all entities with common identifier pattern

```typescript
interface Entity {
  id: string; // UUID format: "123e4567-e89b-12d3-a456-426614174000"
}
```

**Usage**: Extended by User and other entities requiring unique identification
**Format**: UUID v4 standard
**Generation**: Server-side UUID generation for consistency

### 2. User Entity
**Purpose**: Represents authenticated user account information

```typescript
interface User extends Entity {
  id: string;           // UUID - unique user identifier
  email: string;        // User's email address (login credential)
  startDate: string;    // ISO 8601 datetime - account creation timestamp
}
```

**Field Details**:
- `id`: Primary key, UUID format, immutable after creation
- `email`: Unique constraint, used for authentication, changeable
- `startDate`: Account creation timestamp, used for data retention policies

**Relationships**:
- One-to-many with diary entries (ItemsResponse)
- One-to-many with assets
- One-to-many with sync changes

**Mobile Storage Considerations**:
- Cache user data locally for offline access
- Encrypt sensitive user information
- Sync user profile changes when online

### 3. Authentication Data Model
**Purpose**: Request payload for user authentication

```typescript
interface AuthData {
  email: string;        // User's email address
  password: string;     // Plain text password (HTTPS required)
}
```

**Security Considerations**:
- Password transmitted over HTTPS only
- No password storage in client applications
- Server-side password hashing (bcrypt recommended)
- Rate limiting on authentication attempts

**Mobile Implementation**:
- Secure form validation
- Biometric authentication integration
- Remember email (not password)
- Auto-complete support

### 4. Diary Entry Request Model
**Purpose**: Payload for creating/updating diary entries

```typescript
interface ItemsRequest {
  date: string;         // ISO date format: "2024-01-15"
  title: string;        // Entry title (required)
  body: string;         // Entry content in markdown format
  tags: string[];       // Array of tag strings
}
```

**Field Specifications**:
- `date`: ISO 8601 date format (YYYY-MM-DD), unique per user
- `title`: Required field, recommended max length 200 characters
- `body`: Markdown-formatted content, supports images and formatting
- `tags`: Optional array, case-insensitive, trimmed strings

**Validation Rules**:
- Date must be valid ISO format
- Title cannot be empty or whitespace only
- Body supports markdown syntax
- Tags are normalized (lowercase, trimmed)
- Duplicate tags are removed

### 5. Diary Entry Response Model
**Purpose**: Complete diary entry data with navigation information

```typescript
interface ItemsResponse extends ItemsRequest {
  date: string;         // Entry date
  title: string;        // Entry title
  body: string;         // Entry content (markdown)
  tags: string[];       // Entry tags
  previousDate?: string; // Previous entry date (nullable)
  nextDate?: string;    // Next entry date (nullable)
}
```

**Additional Fields**:
- `previousDate`: Date of previous entry (null if first entry)
- `nextDate`: Date of next entry (null if latest entry)

**Navigation Logic**:
- Server calculates previous/next dates based on user's entry history
- Enables seamless date-based navigation
- Null values indicate boundary conditions

**Mobile Usage**:
- Enable swipe navigation between entries
- Preload adjacent entries for smooth transitions
- Cache navigation data for offline browsing

### 6. Items List Response Model
**Purpose**: Paginated list of diary entries with metadata

```typescript
interface ItemsListResponse {
  items: ItemsResponse[];  // Array of diary entries
  totalCount: number;      // Total matching entries
}
```

**Usage Scenarios**:
- Search results with filtering
- Entry listing with pagination
- Archive browsing

**Mobile Implementation**:
- Implement infinite scroll with totalCount
- Cache search results for offline access
- Optimize for large datasets

### 7. Sync Change Response Model
**Purpose**: Represents individual change in synchronization system

```typescript
interface SyncChangeResponse {
  id: number;                    // Unique change ID (sequential)
  userId: string;                // User who made the change
  date: string;                  // Date of affected diary entry
  operationType: 'created' | 'updated' | 'deleted'; // Change type
  timestamp: string;             // ISO 8601 datetime of change
  itemSnapshot?: ItemsResponse;  // Current item state (null for deleted)
  metadata: string[];            // Additional change context
}
```

**Field Details**:
- `id`: Sequential integer, used for incremental sync
- `userId`: References User.id, enables multi-user sync
- `date`: Date of the diary entry that changed
- `operationType`: Enum for change type tracking
- `timestamp`: When the change occurred (server time)
- `itemSnapshot`: Full entry data (null for deletions)
- `metadata`: Array of strings for change context (app version, device info)

**Sync Logic**:
- Changes are ordered by ID for consistent sync
- Client tracks last processed change ID
- Incremental sync requests changes since last ID

### 8. Sync Response Model
**Purpose**: Batch of changes for synchronization with pagination

```typescript
interface SyncResponse {
  changes: SyncChangeResponse[]; // Array of change records
  hasMore: boolean;              // More changes available flag
  nextId?: number;               // Next change ID for pagination
}
```

**Pagination Logic**:
- `hasMore`: true if more changes exist beyond current batch
- `nextId`: Use this ID for next sync request if hasMore is true
- Default batch size: 100 changes, maximum: 1000

**Mobile Sync Strategy**:
- Process changes in batches to avoid memory issues
- Continue syncing until hasMore is false
- Handle network interruptions gracefully

## Entity Relationships

### 1. User → Diary Entries (One-to-Many)
```
User (1) ←→ (Many) ItemsResponse
- User.id = ItemsResponse.userId (implicit)
- Each user has multiple diary entries
- Entries are isolated per user
```

### 2. User → Assets (One-to-Many)
```
User (1) ←→ (Many) Assets
- User.id determines asset storage path
- Assets are user-specific and isolated
- Asset cleanup when user is deleted
```

### 3. User → Sync Changes (One-to-Many)
```
User (1) ←→ (Many) SyncChangeResponse
- SyncChangeResponse.userId = User.id
- Changes track all user modifications
- Used for multi-device synchronization
```

### 4. Diary Entry → Assets (Many-to-Many)
```
ItemsResponse (Many) ←→ (Many) Assets
- Relationship through markdown references
- Assets referenced in entry body: ![](filename.ext)
- Orphaned asset cleanup possible
```

## Data Flow Patterns

### 1. Authentication Flow
```
Client → AuthData → Server
Server → JWT Token → Client
Client stores token securely
Subsequent requests include Bearer token
```

### 2. Entry Creation Flow
```
Client → ItemsRequest → Server
Server validates and stores entry
Server → ItemsResponse (with navigation) → Client
Client updates local cache
```

### 3. Asset Upload Flow
```
Client → Multipart file → Server
Server generates UUID filename
Server stores file in user directory
Server → filename string → Client
Client references filename in entry body
```

### 4. Synchronization Flow
```
Client → GET /sync/changes?since=lastId → Server
Server → SyncResponse (batch of changes) → Client
Client processes changes and updates local data
Repeat until hasMore = false
```

### 5. Search Flow
```
Client → GET /items?search=query&tags=tag1,tag2 → Server
Server processes search and filtering
Server → ItemsListResponse → Client
Client displays results with pagination
```

## Mobile Data Architecture

### 1. Local Database Schema
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
  FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Sync state tracking
CREATE TABLE sync_state (
  user_id TEXT PRIMARY KEY,
  last_sync_id INTEGER DEFAULT 0,
  last_sync_timestamp DATETIME,
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
  FOREIGN KEY (user_id) REFERENCES users (id)
);
```

### 2. Data Transformation Patterns

#### API to Local Database
```typescript
// Transform API response to local storage
const transformApiToLocal = (apiResponse: ItemsResponse): LocalEntry => {
  return {
    id: generateLocalId(apiResponse.date, apiResponse.userId),
    user_id: getCurrentUserId(),
    date: apiResponse.date,
    title: apiResponse.title,
    body: apiResponse.body,
    tags: JSON.stringify(apiResponse.tags),
    previous_date: apiResponse.previousDate,
    next_date: apiResponse.nextDate,
    is_synced: true,
    updated_at: new Date().toISOString()
  };
};
```

#### Local Database to API
```typescript
// Transform local data for API submission
const transformLocalToApi = (localEntry: LocalEntry): ItemsRequest => {
  return {
    date: localEntry.date,
    title: localEntry.title,
    body: localEntry.body,
    tags: JSON.parse(localEntry.tags || '[]')
  };
};
```

### 3. Conflict Resolution Strategy

#### Last-Write-Wins with User Override
```typescript
interface ConflictResolution {
  strategy: 'last-write-wins' | 'user-choice' | 'merge';
  localVersion: ItemsResponse;
  remoteVersion: ItemsResponse;
  resolution?: ItemsResponse;
}

const resolveConflict = (conflict: ConflictResolution): ItemsResponse => {
  switch (conflict.strategy) {
    case 'last-write-wins':
      return conflict.remoteVersion; // Server wins by default
    case 'user-choice':
      return showConflictResolutionUI(conflict);
    case 'merge':
      return mergeEntries(conflict.localVersion, conflict.remoteVersion);
  }
};
```

## Data Validation Patterns

### 1. Client-Side Validation
```typescript
const validateItemsRequest = (data: ItemsRequest): ValidationResult => {
  const errors: string[] = [];
  
  // Date validation
  if (!isValidISODate(data.date)) {
    errors.push('Invalid date format');
  }
  
  // Title validation
  if (!data.title || data.title.trim().length === 0) {
    errors.push('Title is required');
  }
  
  if (data.title.length > 200) {
    errors.push('Title too long (max 200 characters)');
  }
  
  // Body validation
  if (!data.body || data.body.trim().length === 0) {
    errors.push('Entry content is required');
  }
  
  // Tags validation
  if (data.tags) {
    data.tags = data.tags
      .map(tag => tag.trim().toLowerCase())
      .filter(tag => tag.length > 0)
      .filter((tag, index, arr) => arr.indexOf(tag) === index); // Remove duplicates
  }
  
  return {
    isValid: errors.length === 0,
    errors: errors,
    sanitizedData: data
  };
};
```

### 2. Data Sanitization
```typescript
const sanitizeEntry = (entry: ItemsRequest): ItemsRequest => {
  return {
    date: entry.date.trim(),
    title: entry.title.trim().substring(0, 200),
    body: sanitizeMarkdown(entry.body),
    tags: entry.tags
      ?.map(tag => tag.trim().toLowerCase())
      .filter(tag => tag.length > 0 && tag.length <= 50)
      .slice(0, 20) // Limit number of tags
  };
};
```

## Performance Considerations

### 1. Data Caching Strategy
- Cache frequently accessed entries locally
- Implement LRU cache for memory management
- Preload adjacent entries for navigation
- Cache search results with TTL

### 2. Sync Optimization
- Batch sync operations to reduce API calls
- Implement exponential backoff for failed syncs
- Use delta sync for large datasets
- Compress sync payloads when possible

### 3. Memory Management
- Lazy load entry content for large entries
- Implement pagination for entry lists
- Clean up unused cached data
- Monitor memory usage and optimize accordingly
