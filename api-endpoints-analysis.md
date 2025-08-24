# API Endpoints Analysis

## Overview
Comprehensive analysis of the backend API specification for mobile app integration, including all endpoints, request/response schemas, authentication patterns, and error handling.

## API Base Information
- **API Version**: 0.0.1
- **Title**: Diary - OpenAPI 3.0
- **Base URL**: To be configured per environment
- **Authentication**: JWT Bearer tokens (except /v1/authorize)
- **Content Type**: application/json (except file uploads)

## Authentication Endpoint

### POST /v1/authorize
**Purpose**: Validate user credentials and return JWT token

**Security**: No authentication required (public endpoint)

**Request Schema**:
```json
{
  "email": "john@email.com",
  "password": "12345"
}
```

**Request Details**:
- Content-Type: application/json
- Required fields: email, password
- Email format validation required
- Password minimum requirements (to be defined)

**Response Schemas**:

**Success (200)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Error (401)**:
```json
{
  "error": "Authentication failed",
  "message": "Invalid credentials"
}
```

**Mobile Implementation Considerations**:
- Secure token storage in Keychain/Keystore
- Token expiration handling
- Biometric authentication integration
- Auto-login with stored credentials
- Network timeout handling (30 seconds recommended)

**Error Handling Patterns**:
- Invalid email format → Client-side validation
- Wrong credentials → Display user-friendly error
- Network errors → Retry mechanism with exponential backoff
- Server errors → Fallback to offline mode if possible

## User Management Endpoint

### GET /v1/user
**Purpose**: Retrieve authenticated user profile information

**Security**: Bearer token required

**Request Details**:
- Method: GET
- Headers: Authorization: Bearer {token}
- No request body required

**Response Schema (200)**:
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "john@email.com",
  "startDate": "2024-01-01T00:00:00Z"
}
```

**Response Fields**:
- `id`: UUID format, unique user identifier
- `email`: User's email address (login identifier)
- `startDate`: ISO 8601 datetime, user account creation date

**Mobile Implementation Considerations**:
- Cache user data locally for offline access
- Sync user profile changes
- Handle profile updates (if endpoint exists)
- Display user info in profile screen

**Error Handling**:
- 401 Unauthorized → Token expired, redirect to login
- 404 Not Found → User account issues
- 500 Server Error → Display error, retry option

## Diary Entry Management Endpoints

### GET /v1/items
**Purpose**: Retrieve diary entries with optional filtering

**Security**: Bearer token required

**Query Parameters**:
- `date` (optional): Filter by specific date (YYYY-MM-DD format)
- `search` (optional): Full-text search across title and body
- `tags` (optional): Comma-separated list of tags to filter by

**Request Examples**:
```
GET /v1/items?date=2024-01-15
GET /v1/items?search=vacation
GET /v1/items?tags=personal,work
GET /v1/items?search=meeting&tags=work&date=2024-01-15
```

**Response Schema (200)**:
```json
{
  "items": [
    {
      "date": "2024-01-15",
      "title": "My diary entry",
      "body": "Today was a great day...",
      "tags": ["personal", "work", "travel"],
      "previousDate": "2024-01-14",
      "nextDate": "2024-01-16"
    }
  ],
  "totalCount": 42
}
```

**Response Field Details**:
- `items`: Array of diary entries matching criteria
- `totalCount`: Total number of matching entries (for pagination)
- `previousDate`/`nextDate`: Navigation dates (null if none exist)

**Mobile Implementation Considerations**:
- Implement pagination for large result sets
- Cache frequently accessed entries
- Offline search capabilities
- Real-time search with debouncing
- Infinite scroll for mobile UI

### PUT /v1/items
**Purpose**: Create or update diary entry (upsert operation)

**Security**: Bearer token required

**Request Schema**:
```json
{
  "date": "2024-01-15",
  "title": "My diary entry",
  "body": "Today was a great day...",
  "tags": ["personal", "work", "travel"]
}
```

**Request Field Requirements**:
- `date`: Required, ISO date format (YYYY-MM-DD)
- `title`: Required, string (max length to be defined)
- `body`: Required, string (supports markdown)
- `tags`: Optional, array of strings

**Response Schema (200)**:
```json
{
  "date": "2024-01-15",
  "title": "My diary entry",
  "body": "Today was a great day...",
  "tags": ["personal", "work", "travel"],
  "previousDate": "2024-01-14",
  "nextDate": "2024-01-16"
}
```

**Mobile Implementation Considerations**:
- Optimistic updates with rollback on failure
- Auto-save functionality with debouncing
- Offline entry creation with sync queue
- Draft management for unsaved changes
- Conflict resolution for concurrent edits

**Error Handling**:
- 400 Bad Request → Validation errors, show field-specific messages
- 401 Unauthorized → Token issues, re-authenticate
- 409 Conflict → Concurrent edit conflict, show resolution UI

## Asset Management Endpoints

### GET /v1/assets
**Purpose**: Retrieve asset file by path

**Security**: Bearer token required

**Query Parameters**:
- `path` (required): Relative path to asset file

**Request Example**:
```
GET /v1/assets?path=images/photos/vacation.jpg
```

**Response**:
- Content-Type: Varies based on file type (image/jpeg, video/mp4, etc.)
- Body: Binary file data
- Headers: Content-Length, Cache-Control, ETag

**Mobile Implementation Considerations**:
- Implement aggressive caching for assets
- Progressive image loading
- Thumbnail generation for quick display
- Background asset preloading
- Memory management for large assets

**Error Handling**:
- 404 Not Found → Asset deleted or moved, show placeholder
- 401 Unauthorized → Re-authenticate and retry
- 500 Server Error → Show cached version if available

### POST /v1/assets
**Purpose**: Upload asset file (image or video)

**Security**: Bearer token required

**Request Format**:
- Content-Type: multipart/form-data
- Form field: `asset` (binary file data)
- File size limit: 10MB
- Supported formats: .jpg, .jpeg, .png, .gif, .bmp, .webp, .mp4, .mov, .avi, .wmv, .flv, .mkv

**Response Schema (200)**:
```
"123e4567-e89b-12d3-a456-426614174000.jpg"
```

**Response**: Plain text filename (UUID-based)

**Mobile Implementation Considerations**:
- Image compression before upload
- Upload progress indicators
- Background upload processing
- Upload retry on network failure
- Batch upload capabilities
- Camera integration for direct capture

**Error Handling**:
- 400 Bad Request → Invalid file type or missing asset field
- 401 Unauthorized → Re-authenticate
- 413 Payload Too Large → File size exceeded, compress or reject
- 500 Internal Server Error → Retry with exponential backoff

## Synchronization Endpoint

### GET /v1/sync/changes
**Purpose**: Retrieve incremental changes for data synchronization

**Security**: Bearer token required

**Query Parameters**:
- `since` (optional): Get changes since this change ID (exclusive)
- `limit` (optional): Maximum changes to return (default: 100, max: 1000)

**Request Examples**:
```
GET /v1/sync/changes
GET /v1/sync/changes?since=123
GET /v1/sync/changes?since=123&limit=50
```

**Response Schema (200)**:
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

**Response Field Details**:
- `changes`: Array of change records
- `hasMore`: Boolean indicating if more changes exist
- `nextId`: ID to use for next sync request (if hasMore is true)

**Change Record Fields**:
- `id`: Unique change identifier (sequential)
- `userId`: User who made the change
- `date`: Date of the affected diary entry
- `operationType`: "created", "updated", or "deleted"
- `timestamp`: When the change occurred (ISO 8601)
- `itemSnapshot`: Current state of item (null for deleted items)
- `metadata`: Additional change context information

**Mobile Implementation Considerations**:
- Implement incremental sync with change tracking
- Store last sync ID locally
- Handle large sync operations with pagination
- Background sync scheduling
- Conflict resolution for concurrent changes
- Network-aware sync (WiFi vs cellular)

## Authentication Security Patterns

### JWT Token Management
**Token Structure**: Standard JWT with header, payload, signature
**Token Lifetime**: To be configured (recommended: 24 hours)
**Refresh Strategy**: Re-authenticate when token expires

**Mobile Security Requirements**:
```javascript
// Secure token storage
const tokenManager = {
  async storeToken(token) {
    await SecureStore.setItemAsync('auth_token', token);
  },
  
  async getToken() {
    return await SecureStore.getItemAsync('auth_token');
  },
  
  async clearToken() {
    await SecureStore.deleteItemAsync('auth_token');
  }
};
```

### Request Authentication Pattern
**Header Format**: `Authorization: Bearer {token}`
**Token Validation**: Server validates signature and expiration
**Error Response**: 401 Unauthorized for invalid/expired tokens

## Error Response Patterns

### Standard Error Format
```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "details": {
    "field": "Specific field error",
    "code": "VALIDATION_ERROR"
  }
}
```

### HTTP Status Code Usage
- **200 OK**: Successful operation
- **400 Bad Request**: Client error (validation, malformed request)
- **401 Unauthorized**: Authentication required or failed
- **404 Not Found**: Resource not found
- **409 Conflict**: Concurrent modification conflict
- **413 Payload Too Large**: File size exceeded
- **500 Internal Server Error**: Server-side error

### Mobile Error Handling Strategy
```javascript
const apiErrorHandler = {
  handle401: () => {
    // Clear stored token and redirect to login
    tokenManager.clearToken();
    navigation.navigate('Login');
  },
  
  handle400: (error) => {
    // Show validation errors to user
    showValidationErrors(error.details);
  },
  
  handle500: () => {
    // Show generic error, enable retry
    showErrorWithRetry('Something went wrong. Please try again.');
  },
  
  handleNetworkError: () => {
    // Enable offline mode if applicable
    enableOfflineMode();
  }
};
```

## API Client Implementation Recommendations

### Request Interceptors
- Add authentication headers automatically
- Handle request timeouts (30 seconds)
- Add request logging for debugging
- Implement request queuing for offline scenarios

### Response Interceptors
- Handle token expiration automatically
- Parse error responses consistently
- Implement retry logic for transient failures
- Cache successful responses where appropriate

### Network Optimization
- Implement request deduplication
- Use HTTP/2 for multiplexing
- Compress request/response bodies
- Implement connection pooling

### Offline Support Strategy
- Queue API calls when offline
- Sync queued calls when online
- Implement conflict resolution
- Provide offline UI feedback
