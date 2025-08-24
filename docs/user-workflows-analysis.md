# User Workflows & Navigation Patterns Analysis

## Overview
This document maps out all user workflows and navigation patterns in the existing diary web application to ensure seamless mobile app user experience design.

## Core User Workflows

### 1. Authentication Flow
**Entry Points**: Any protected page access, direct login page visit

**Workflow Steps**:
1. **Initial Access**
   - User attempts to access protected content
   - System checks for valid session/token
   - If unauthorized → redirect to login page with return URL

2. **Login Process**
   - Display login form with email/password fields
   - User enters credentials
   - Client-side validation (required fields)
   - Form submission to `/web/login` (POST)
   - Server validates credentials against database
   - Generate JWT token on success
   - Set secure HTTP-only cookie
   - Redirect to original URL or home page

3. **Session Management**
   - Token stored in secure cookie
   - Automatic session validation on each request
   - Session timeout handling
   - Remember user preference persistence

4. **Logout Process**
   - User clicks logout link
   - Cookie cleared/expired
   - Redirect to login page
   - Optional redirect parameter handling

**Mobile Adaptations Needed**:
- Secure token storage (Keychain/Keystore)
- Biometric authentication integration
- Auto-login with stored credentials
- Session timeout with background app handling

### 2. Daily Diary Entry Creation/Editing Workflow
**Entry Points**: Home page "Create Entry" button, Edit navigation link, Search results edit button

**Workflow Steps**:
1. **Entry Access**
   - User navigates to edit page
   - System determines date (URL parameter or current date)
   - Fetch existing entry data or create empty entry
   - Load associated assets for display

2. **Editor Interface**
   - Display form with title, body, tags fields
   - Pre-populate with existing data if available
   - Show asset thumbnails for insertion
   - Enable markdown editing capabilities

3. **Content Creation/Editing**
   - User types in title field
   - User writes content in body textarea (markdown supported)
   - User adds/modifies tags (comma-separated)
   - User can insert assets via click-to-insert
   - Real-time character counting
   - Auto-save functionality (if implemented)

4. **Asset Integration**
   - User uploads new assets via file picker
   - Assets appear as clickable thumbnails
   - Click thumbnail → insert markdown syntax
   - Assets integrated into entry content

5. **Save Process**
   - User clicks Save button
   - Form validation (required fields)
   - Submit to `/web/edit` (POST)
   - Server processes and stores entry
   - Success → redirect to home page with saved entry
   - Error → display error message, retain form data

6. **Delete Process** (if entry exists)
   - User clicks Delete button
   - Confirmation dialog/process
   - Submit delete request
   - Success → redirect to home page
   - Error → display error message

**Mobile Adaptations Needed**:
- Touch-optimized text editing
- Mobile keyboard optimization
- Auto-save with draft management
- Offline editing capabilities
- Camera integration for asset capture
- Voice-to-text input support

### 3. Date-Based Navigation Patterns
**Entry Points**: Home page, direct URL with date parameter

**Navigation Components**:
1. **Previous/Next Navigation**
   - Previous button (left arrow) - conditional display
   - Current date display with entry title
   - Next button (right arrow) - conditional display
   - Smooth transitions between dates
   - URL updates: `/?date=YYYY-MM-DD`

2. **Date Selection Methods**
   - Direct URL entry with date parameter
   - Navigation button clicks
   - Calendar picker (potential enhancement)
   - "Today" quick access button

3. **Date Validation & Handling**
   - Invalid date format handling
   - Future date limitations
   - Date range boundaries (user start date)
   - Empty entry state management

**Workflow Steps**:
1. **Date Navigation**
   - User clicks previous/next buttons
   - System validates target date
   - Fetch entry data for target date
   - Update URL and display content
   - Update navigation button states

2. **Direct Date Access**
   - User enters URL with date parameter
   - System validates date format
   - Fetch entry data or show empty state
   - Display appropriate navigation options

**Mobile Adaptations Needed**:
- Swipe gestures for date navigation
- Calendar picker integration
- Date range selection
- Quick date shortcuts (today, yesterday, etc.)
- Gesture-based navigation feedback

### 4. Search and Discovery Workflows
**Entry Points**: Header search bar, search page direct access

**Workflow Steps**:
1. **Search Initiation**
   - User enters query in header search bar
   - Form submission to `/web/search` (GET)
   - Query parameter: `?search=query`
   - System processes search request

2. **Search Execution**
   - Full-text search across title and body
   - Tag-based filtering support
   - Date range filtering (if specified)
   - Result ranking and relevance scoring
   - Pagination for large result sets

3. **Results Display**
   - Search results with entry previews
   - Highlighted search terms
   - Entry metadata (date, title, tags)
   - Result count display
   - Edit buttons for each result

4. **Result Interaction**
   - Click entry → navigate to full entry view
   - Click edit → open entry editor
   - Refine search with additional filters
   - Clear search to start over

5. **Advanced Search Features**
   - Tag-based filtering
   - Date range selection
   - Combined search parameters
   - Search history (potential)
   - Saved searches (potential)

**Mobile Adaptations Needed**:
- Search suggestions and autocomplete
- Voice search integration
- Advanced filter UI for mobile
- Search result optimization for small screens
- Infinite scroll for results
- Search history and favorites

### 5. Asset Upload and Insertion Workflow
**Entry Points**: Edit page asset upload section

**Workflow Steps**:
1. **Asset Selection**
   - User clicks file upload button
   - File picker opens with type filtering
   - User selects image/video file
   - File validation (type, size limits)

2. **Upload Process**
   - File upload to `/web/upload` endpoint
   - Progress indication during upload
   - Server generates UUID filename
   - File stored in user-specific directory
   - Success response with filename

3. **Asset Display**
   - Thumbnail generated and displayed
   - Asset added to editor interface
   - Clickable thumbnail for insertion
   - Asset metadata display

4. **Asset Insertion**
   - User clicks asset thumbnail
   - Markdown syntax generated: `![](filename.ext)`
   - Syntax inserted at cursor position
   - Focus returned to text editor
   - Asset preview in rendered content

5. **Asset Management**
   - Asset deletion (if supported)
   - Asset replacement
   - Asset metadata editing
   - Asset organization

**Mobile Adaptations Needed**:
- Camera integration for direct capture
- Gallery/photo library access
- Image compression before upload
- Multiple asset selection
- Drag-and-drop asset placement
- Asset preview and editing tools

### 6. Tag Management Workflow
**Entry Points**: Edit page tags field, search page tag filters

**Workflow Steps**:
1. **Tag Input**
   - User types in tags field (comma-separated)
   - Auto-complete suggestions (if available)
   - Tag validation and formatting
   - Duplicate tag prevention

2. **Tag Display**
   - Tags shown as badges in entries
   - Clickable tags for filtering
   - Tag color coding (if implemented)
   - Tag popularity indicators

3. **Tag-Based Search**
   - User clicks tag badge
   - System filters entries by tag
   - Multiple tag selection support
   - Tag combination logic (AND/OR)

4. **Tag Management**
   - Tag creation through entry editing
   - Tag deletion when no longer used
   - Tag renaming/merging (if supported)
   - Tag statistics and analytics

**Mobile Adaptations Needed**:
- Tag input with suggestions
- Tag picker interface
- Popular tags display
- Tag management screen
- Tag-based navigation
- Tag analytics and insights

### 7. Layout Preference Management
**Entry Points**: Home page layout toggle buttons

**Workflow Steps**:
1. **Layout Selection**
   - User clicks Full or Narrow layout button
   - Immediate visual feedback
   - CSS class changes applied
   - Button state updates (active/inactive)

2. **Preference Persistence**
   - Setting saved to localStorage
   - Preference key: 'diary-layout-preference'
   - Cross-session persistence
   - Default to narrow layout

3. **Layout Application**
   - Image width adjustments (30% vs 100%)
   - Smooth CSS transitions (300ms)
   - Responsive behavior maintained
   - Accessibility announcements

**Mobile Adaptations Needed**:
- Touch-friendly toggle controls
- Gesture-based layout switching
- Mobile-optimized layout options
- Preference sync across devices
- Adaptive layout based on screen size

## Navigation Architecture

### Primary Navigation Structure
```
Header Navigation:
├── Brand (Home)
├── Home
├── Edit
├── About
├── Search (Global)
└── User Menu
    └── Logout
```

### Page-Level Navigation
```
Home Page:
├── Date Navigation (Previous/Next)
├── Layout Toggle
├── Create Entry (Empty State)
└── Edit Current Entry

Edit Page:
├── Form Navigation (Save/Delete)
├── Asset Management
└── Return to Home

Search Page:
├── Search Refinement
├── Result Navigation
└── Individual Entry Actions
```

### URL Structure and Routing
- **Home**: `/` or `/?date=YYYY-MM-DD`
- **Edit**: `/web/edit` or `/web/edit?date=YYYY-MM-DD`
- **Search**: `/web/search?search=query&tags=tag1,tag2`
- **Login**: `/web/login?redirect=returnURL`
- **About**: `/web/about`
- **Assets**: `/web/assets/filename.ext`
- **Static**: `/web/static/css|js/filename`

## State Management Patterns

### Client-Side State
- **Layout Preferences**: localStorage persistence
- **Form State**: Temporary form data retention
- **Search State**: Query and filter persistence
- **Navigation State**: Current page and context

### Server-Side State
- **Authentication**: Session cookies and JWT tokens
- **User Data**: Database-backed user profiles
- **Entry Data**: Persistent diary entries
- **Asset Data**: File system and database references

### State Synchronization
- **Form Submission**: Client → Server data flow
- **Page Rendering**: Server → Client data flow
- **Asset Upload**: Bidirectional with progress tracking
- **Search Results**: Server processing with client display

## Error Handling Patterns

### Client-Side Error Handling
- Form validation errors
- Network connectivity issues
- JavaScript execution errors
- Asset upload failures

### Server-Side Error Handling
- Authentication failures
- Database connection issues
- File upload errors
- API endpoint errors

### User Experience Considerations
- Graceful degradation
- Error message clarity
- Recovery action guidance
- Progress indication
- Loading state management

## Mobile-Specific Workflow Adaptations

### Touch Interface Considerations
- Minimum touch target sizes (44pt)
- Gesture-based navigation
- Haptic feedback integration
- Touch-friendly form controls

### Mobile Performance Optimizations
- Lazy loading for content and assets
- Image compression and optimization
- Background sync capabilities
- Battery usage optimization

### Platform Integration Opportunities
- Camera and photo library access
- Native sharing functionality
- Push notifications for reminders
- Biometric authentication
- Voice input capabilities
- Offline synchronization
