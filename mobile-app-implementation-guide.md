# Mobile Diary App Implementation Guide

## Overview
This document provides comprehensive implementation guidance for developing a mobile application that replicates all functionality from the existing web application and maintains data synchronization with the backend API.

## Current Web Application Analysis

### Core Features Identified
1. **Authentication System**
   - JWT-based login/logout with session management
   - Email/password authentication
   - Secure token storage and validation
   - Session persistence across browser sessions

2. **Home Page (Diary Viewing)**
   - Daily diary entry display with markdown rendering
   - Date-based navigation (previous/next buttons)
   - Empty state handling for days without entries
   - Layout toggle (full-width vs narrow for images)
   - Pull-to-refresh functionality

3. **Edit Page (Diary Entry Management)**
   - Rich text editor with markdown support
   - Title, body, and tags input fields
   - Date picker for entry date
   - Asset insertion with click-to-insert functionality
   - Auto-save capabilities
   - Form validation and error handling

4. **Search Page**
   - Full-text search across title and body content
   - Tag-based filtering with multi-tag support
   - Advanced search with date range filtering
   - Search result highlighting
   - Empty search state handling

5. **Asset Management System**
   - Image and video upload support
   - Asset display within diary entries
   - Asset integration with markdown syntax
   - Asset caching and optimization
   - Supported formats: .jpg, .jpeg, .png, .gif, .bmp, .webp, .mp4, .mov, .avi, .wmv, .flv, .mkv

6. **Responsive Design**
   - Mobile-optimized CSS with breakpoints
   - Touch-friendly interface elements
   - Adaptive layouts for different screen sizes
   - Accessibility features and ARIA compliance

### UI Components & Templates
- **Header Template**: Navigation bar with brand, menu, search, and logout
- **Footer Template**: Simple copyright footer
- **Home Template**: Main diary display with date navigation and layout toggle
- **Edit Template**: Comprehensive editing interface with asset management
- **Search Template**: Search interface with results display
- **Login Template**: Authentication form with redirect handling

### JavaScript Functionality
- **Layout Toggle System**: Full-width (100%) vs narrow (30%) image display
- **Asset Management**: Dynamic image insertion into markdown editor
- **Responsive Behavior**: Mobile-optimized interactions and gestures
- **Session Management**: localStorage for user preferences
- **Error Handling**: Graceful degradation and error recovery

## Backend API Specification

### Authentication Endpoints
```
POST /v1/authorize
- Request: { email: string, password: string }
- Response: { token: string }
- Security: No auth required for this endpoint
```

### User Management
```
GET /v1/user
- Response: { id: uuid, email: string, startDate: datetime }
- Security: Bearer token required
```

### Diary Entry Management
```
GET /v1/items
- Query params: date (optional), search (optional), tags (optional)
- Response: { items: ItemsResponse[], totalCount: number }
- Security: Bearer token required

PUT /v1/items
- Request: { date: date, title: string, body: string, tags: string[] }
- Response: { date, title, body, tags, previousDate?, nextDate? }
- Security: Bearer token required
```

### Asset Management
```
GET /v1/assets?path=string
- Response: Binary file data
- Security: Bearer token required

POST /v1/assets
- Request: multipart/form-data with 'asset' field
- Response: filename string
- Security: Bearer token required
- File size limit: 10MB
```

### Synchronization API
```
GET /v1/sync/changes
- Query params: since (optional), limit (optional, default 100, max 1000)
- Response: { changes: SyncChangeResponse[], hasMore: boolean, nextId?: number }
- Security: Bearer token required
```

### Data Models
```typescript
interface User {
  id: string;
  email: string;
  startDate: string;
}

interface ItemsRequest {
  date: string;
  title: string;
  body: string;
  tags: string[];
}

interface ItemsResponse extends ItemsRequest {
  previousDate?: string;
  nextDate?: string;
}

interface SyncChangeResponse {
  id: number;
  userId: string;
  date: string;
  operationType: 'created' | 'updated' | 'deleted';
  timestamp: string;
  itemSnapshot?: ItemsResponse;
  metadata: string[];
}
```

## Mobile-Specific Implementation Requirements

### Technology Stack Recommendations
1. **Cross-Platform Framework**
   - React Native (recommended for web app similarity)
   - Flutter (alternative with excellent performance)
   - Native iOS/Android (maximum performance, separate codebases)

2. **State Management**
   - React Native: Redux Toolkit + RTK Query
   - Flutter: Provider/Riverpod + Dio for HTTP
   - Native: Platform-specific solutions

3. **Local Storage**
   - SQLite with appropriate ORM (Watermelon DB, Drift, Core Data, Room)
   - Secure storage for tokens (Keychain/Keystore)
   - File system for asset caching

4. **Essential Libraries**
   - HTTP client with interceptors
   - Markdown rendering library
   - Image/video picker and camera access
   - Date picker components
   - Navigation library with deep linking
   - Background task scheduling

### Architecture Patterns
1. **Offline-First Architecture**
   - Local database as single source of truth
   - Background synchronization with conflict resolution
   - Queue-based sync for pending changes
   - Network connectivity monitoring

2. **Component Structure**
   ```
   src/
   ├── components/          # Reusable UI components
   ├── screens/            # Screen components
   ├── services/           # API and business logic
   ├── store/              # State management
   ├── utils/              # Helper functions
   ├── types/              # TypeScript definitions
   └── assets/             # Static assets
   ```

3. **Data Flow**
   - UI Components → Actions → Services → API/Local DB
   - Sync Service runs independently in background
   - Conflict resolution at service layer

### Mobile Adaptations Required
1. **Touch Interface**
   - Swipe gestures for date navigation
   - Pull-to-refresh on home screen
   - Touch-friendly button sizes (44pt minimum)
   - Haptic feedback for interactions

2. **Camera Integration**
   - Direct photo/video capture
   - Gallery/library access
   - Image compression before upload
   - Real-time preview in editor

3. **Performance Optimizations**
   - Image lazy loading and caching
   - Memory management for large assets
   - Battery usage optimization
   - Background sync limitations

4. **Platform Features**
   - Biometric authentication
   - Native sharing functionality
   - Push notifications (future enhancement)
   - Deep linking for date-based URLs

## Implementation Phases

### Phase 1: Analysis & Planning (1-2 weeks)
- Complete feature audit and documentation
- Technology stack selection and setup
- Architecture design and data modeling
- Development environment configuration

### Phase 2: Core Infrastructure (2-3 weeks)
- Project setup and navigation structure
- State management implementation
- API client with authentication
- Local database schema and setup

### Phase 3: Authentication & User Management (1-2 weeks)
- Login/logout screens and flows
- Secure token storage implementation
- Session management and guards
- User profile management

### Phase 4: Diary Entry Management (3-4 weeks)
- Home screen with entry display
- Entry editor with markdown support
- Date navigation system
- CRUD operations with offline support

### Phase 5: Search & Discovery (2-3 weeks)
- Search interface and functionality
- Tag management and filtering
- Advanced search features
- Search result optimization

### Phase 6: Asset Management (2-3 weeks)
- Camera and gallery integration
- Asset upload and display
- Markdown editor integration
- Asset synchronization

### Phase 7: Data Synchronization (2-3 weeks)
- Sync API integration
- Conflict resolution system
- Background sync service
- Offline-first data architecture

### Phase 8: Mobile Features & Polish (2-3 weeks)
- Touch gestures and mobile navigation
- Responsive layouts and accessibility
- Performance optimizations
- Native platform integrations

### Phase 9: Testing & QA (2-3 weeks)
- Unit and integration testing
- UI and user experience testing
- Cross-platform compatibility
- Performance and security testing

### Phase 10: Deployment (1-2 weeks)
- App store preparation
- Beta testing program
- Production deployment
- Monitoring and analytics setup

## Security Considerations
1. **Token Management**
   - Secure storage using platform keychain/keystore
   - Automatic token refresh handling
   - Biometric authentication integration
   - Token encryption at rest

2. **Data Protection**
   - Local database encryption
   - Secure API communication (HTTPS)
   - Input validation and sanitization
   - Asset access control

3. **Privacy Compliance**
   - Data retention policies
   - User consent management
   - Privacy policy implementation
   - GDPR/CCPA compliance considerations

## Performance Targets
- App launch time: < 3 seconds
- Screen transition time: < 300ms
- Image loading time: < 2 seconds
- Sync completion time: < 10 seconds for typical datasets
- Memory usage: < 100MB for typical usage
- Battery impact: Minimal background usage

## Success Metrics
- Feature parity: 100% of web app functionality
- User adoption: Seamless migration from web app
- Performance: Meets or exceeds web app performance
- Reliability: 99.9% uptime and data consistency
- User satisfaction: 4.5+ app store rating target

## Technical Implementation Details

### Web App Code Structure Analysis
```
webapp/
├── templates/
│   ├── header.tpl          # Navigation, search, user menu
│   ├── footer.tpl          # Simple footer
│   ├── home.tpl            # Main diary view with date nav
│   ├── edit.tpl            # Entry editor with asset insertion
│   ├── search.tpl          # Search interface and results
│   ├── login.tpl           # Authentication form
│   └── about.tpl           # About page
├── static/
│   ├── css/layout.css      # Responsive design, layout toggle
│   └── js/layout-toggle.js # Image layout switching logic
```

### Key Web App Features to Replicate

#### Layout Toggle System
- **Full Layout**: Images display at 100% width
- **Narrow Layout**: Images display at 30% width (default)
- **Implementation**: CSS classes with JavaScript toggle
- **Mobile Adaptation**: Touch-friendly toggle buttons, gesture support

#### Asset Integration Workflow
1. User uploads image/video via form
2. Server generates UUID filename and stores in user directory
3. JavaScript adds clickable thumbnail to editor
4. Click inserts markdown syntax: `![](filename.ext)`
5. Markdown renderer displays asset in entry

#### Date Navigation Pattern
- Previous/Next buttons with date validation
- Current date display with title
- Empty state handling for missing entries
- URL parameter support: `?date=2024-01-15`

#### Search Implementation
- Real-time search with debouncing
- Multi-parameter support: text + tags + date
- Result highlighting and pagination
- Empty state with helpful actions

### Mobile App Architecture Recommendations

#### State Management Structure
```typescript
interface AppState {
  auth: {
    user: User | null;
    token: string | null;
    isAuthenticated: boolean;
    isLoading: boolean;
  };
  entries: {
    currentEntry: ItemsResponse | null;
    currentDate: string;
    entries: Record<string, ItemsResponse>;
    isLoading: boolean;
    error: string | null;
  };
  search: {
    query: string;
    results: ItemsResponse[];
    filters: SearchFilters;
    isLoading: boolean;
  };
  sync: {
    lastSyncId: number;
    pendingChanges: PendingChange[];
    isSyncing: boolean;
    syncError: string | null;
  };
  assets: {
    uploadQueue: AssetUpload[];
    cache: Record<string, string>; // filename -> local path
  };
}
```

#### Database Schema (SQLite)
```sql
-- Users table
CREATE TABLE users (
  id TEXT PRIMARY KEY,
  email TEXT NOT NULL,
  start_date TEXT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Diary entries table
CREATE TABLE entries (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  date TEXT NOT NULL,
  title TEXT NOT NULL,
  body TEXT NOT NULL,
  tags TEXT, -- JSON array
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
  is_synced BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Sync changes queue
CREATE TABLE sync_queue (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id TEXT NOT NULL,
  entry_date TEXT NOT NULL,
  operation_type TEXT NOT NULL, -- 'create', 'update', 'delete'
  data TEXT, -- JSON payload
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Sync state tracking
CREATE TABLE sync_state (
  user_id TEXT PRIMARY KEY,
  last_sync_id INTEGER DEFAULT 0,
  last_sync_timestamp DATETIME,
  FOREIGN KEY (user_id) REFERENCES users (id)
);
```

#### API Client Implementation Pattern
```typescript
class DiaryAPIClient {
  private baseURL: string;
  private token: string | null = null;

  async authenticate(email: string, password: string): Promise<AuthResponse> {
    const response = await this.post('/v1/authorize', { email, password });
    this.token = response.token;
    await this.secureStorage.setItem('auth_token', this.token);
    return response;
  }

  async getEntries(params: GetEntriesParams): Promise<ItemsListResponse> {
    return this.get('/v1/items', params);
  }

  async saveEntry(entry: ItemsRequest): Promise<ItemsResponse> {
    return this.put('/v1/items', entry);
  }

  async uploadAsset(file: File): Promise<string> {
    const formData = new FormData();
    formData.append('asset', file);
    return this.post('/v1/assets', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  }

  async syncChanges(since?: number): Promise<SyncResponse> {
    return this.get('/v1/sync/changes', { since, limit: 100 });
  }

  private async request(method: string, url: string, data?: any, options?: RequestOptions) {
    const headers = {
      'Content-Type': 'application/json',
      ...(this.token && { 'Authorization': `Bearer ${this.token}` }),
      ...options?.headers
    };

    // Add retry logic, error handling, network detection
    return fetch(`${this.baseURL}${url}`, {
      method,
      headers,
      body: data ? JSON.stringify(data) : undefined,
      ...options
    });
  }
}
```

### Critical Implementation Considerations

#### Offline-First Sync Strategy
1. **Write Operations**: Always write to local DB first
2. **Read Operations**: Always read from local DB
3. **Background Sync**: Periodically sync with server
4. **Conflict Resolution**: Last-write-wins with user override option
5. **Network Handling**: Queue operations when offline

#### Asset Management Strategy
1. **Upload Flow**: Compress → Upload → Store local reference
2. **Display Flow**: Check local cache → Download if needed → Display
3. **Sync Strategy**: Upload assets before entry sync
4. **Cleanup**: Remove unused assets periodically

#### Performance Optimization
1. **Image Handling**: Lazy loading, compression, caching
2. **Memory Management**: Dispose unused resources, limit cache size
3. **Battery Optimization**: Limit background sync frequency
4. **Network Optimization**: Batch requests, compress payloads

#### Security Implementation
1. **Token Storage**: Use platform keychain/keystore
2. **Biometric Auth**: Integrate Touch ID/Face ID/Fingerprint
3. **Data Encryption**: Encrypt local database
4. **Certificate Pinning**: Prevent man-in-the-middle attacks

### Testing Strategy

#### Unit Testing Focus Areas
- API client methods and error handling
- Data synchronization logic
- State management actions and reducers
- Utility functions and data transformations
- Offline/online state transitions

#### Integration Testing Scenarios
- Authentication flow end-to-end
- Entry creation, editing, and deletion
- Asset upload and display workflow
- Search functionality with various filters
- Sync process with conflict resolution

#### UI Testing Priorities
- Navigation between screens
- Form validation and error states
- Touch gestures and interactions
- Responsive layout on different devices
- Accessibility features and screen readers

### Deployment Considerations

#### App Store Requirements
- **iOS**: Xcode project, provisioning profiles, App Store Connect
- **Android**: Android Studio project, signing keys, Google Play Console
- **Metadata**: App descriptions, screenshots, privacy policy
- **Review Process**: Allow 1-2 weeks for app store review

#### CI/CD Pipeline
1. **Code Quality**: Linting, type checking, security scanning
2. **Testing**: Automated unit, integration, and UI tests
3. **Building**: Platform-specific builds with proper signing
4. **Distribution**: Beta testing via TestFlight/Play Console
5. **Monitoring**: Crash reporting, analytics, performance monitoring

This comprehensive guide provides all the technical details needed to successfully implement the mobile diary application with full feature parity to the existing web application.
