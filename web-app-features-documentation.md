# Web Application Features Documentation

## Overview
This document provides a comprehensive analysis of all features, UI components, and functionality in the existing diary web application to ensure complete feature parity in the mobile app.

## Page-by-Page Feature Analysis

### 1. Header Template (Global Navigation)
**File**: `webapp/templates/header.tpl`

**Features**:
- **Brand Logo**: "Diary" brand link to home page
- **Navigation Menu**: 
  - Home (active state indicator)
  - Edit (with optional date parameter)
  - About
- **Search Functionality**:
  - Search input with placeholder "Search diary entries..."
  - Search button with Bootstrap icon
  - Form submission to `/web/search`
  - Search query persistence in input field
- **User Menu**: Logout link
- **Responsive Design**: Collapsible navbar for mobile devices
- **Accessibility**: ARIA labels, screen reader support

**Technical Details**:
- Bootstrap 5.3.3 framework
- Bootstrap Icons 1.11.3
- jQuery 3.7.1 for interactions
- Custom CSS: `/web/static/css/layout.css`
- JavaScript: `/web/static/js/layout-toggle.js`

### 2. Home Page (Main Diary View)
**File**: `webapp/templates/home.tpl`

**Features**:
- **Date Navigation Component**:
  - Previous date button (left arrow) - only shown if previous date exists
  - Current date display with entry title
  - Next date button (right arrow) - only shown if next date exists
  - Accessible navigation with ARIA labels
  - URL parameter support: `?date=YYYY-MM-DD`

- **Layout Toggle Component**:
  - Full Width Layout button (100% image width)
  - Narrow Layout button (30% image width, default)
  - Active state indicators
  - Keyboard navigation support (Enter/Space keys)
  - ARIA accessibility compliance
  - localStorage persistence of user preference

- **Entry Display**:
  - Markdown-rendered diary entry content
  - Image display with responsive sizing
  - Empty state with "Create Entry" button
  - Loading states and error handling

- **JavaScript Disabled Fallback**:
  - Graceful degradation message
  - Core functionality remains available

**Technical Implementation**:
- Template partials: `date-navigation` and `layout-toggle`
- CSS classes: `layout-narrow`, `layout-full`, `diary-image`
- JavaScript: Layout toggle with smooth transitions
- Responsive breakpoints for mobile optimization

### 3. Edit Page (Diary Entry Editor)
**File**: `webapp/templates/edit.tpl`

**Features**:
- **Entry Form**:
  - Hidden date field (populated from URL parameter)
  - Title input field
  - Body textarea (10 rows, markdown support)
  - Tags input field (comma-separated)
  - Save and Delete buttons

- **Asset Management**:
  - Asset upload form with file picker
  - Dynamic asset thumbnail display
  - Click-to-insert asset functionality
  - Markdown syntax generation: `![](filename.ext)`
  - Supported formats: Images and videos
  - Real-time asset preview

- **JavaScript Functionality**:
  - Dynamic image addition to editor
  - Event delegation for asset clicks
  - Automatic markdown insertion
  - Focus management after insertion

**Technical Details**:
- jQuery-based asset management
- Form submission to `/web/edit` (POST)
- Asset upload to `/web/upload` endpoint
- UUID-based asset naming
- Asset storage in user-specific directories

### 4. Search Page
**File**: `webapp/templates/search.tpl`

**Features**:
- **Search Interface**:
  - Search results header with count
  - Query and tag filter display
  - Advanced search capabilities

- **Search Results Display**:
  - Entry cards with date, title, and content preview
  - Tag badges for each entry
  - Edit button for each result
  - Clickable entry links to view full entry
  - Infinite scroll support

- **Empty States**:
  - No results found message
  - Helpful action buttons (Clear Search, Go Home)
  - First-time user guidance

- **Search Parameters**:
  - Text search across title and body
  - Tag-based filtering
  - Date range filtering
  - Combined search capabilities

### 5. Login Page
**File**: `webapp/templates/login.tpl`

**Features**:
- **Authentication Form**:
  - Username (email) input field
  - Password input field
  - Login button
  - Form validation (required fields)
  - Redirect URL handling

- **Security Features**:
  - CSRF protection
  - Secure form submission
  - Error handling and display
  - Redirect after successful login

### 6. About Page
**File**: `webapp/templates/about.tpl`

**Features**:
- Simple informational page
- Consistent header/footer layout
- Basic content structure

## UI Components Analysis

### 1. Layout Toggle System
**Location**: `webapp/static/js/layout-toggle.js`

**Functionality**:
- Two layout modes: Full (100%) and Narrow (30%)
- Session persistence using localStorage
- Keyboard navigation support
- ARIA accessibility compliance
- Smooth CSS transitions (300ms)
- Error handling and recovery
- Browser compatibility (IE11+ with fallbacks)

**Implementation Details**:
```javascript
const LAYOUT_MODES = {
    FULL: 'full',    // 100% image width
    NARROW: 'narrow' // 30% image width
};
const STORAGE_KEY = 'diary-layout-preference';
const TRANSITION_DURATION = 300;
```

### 2. Asset Management System
**Components**:
- File upload with validation
- Dynamic thumbnail generation
- Click-to-insert functionality
- Markdown syntax integration
- Asset caching and optimization

**Supported File Types**:
- Images: .jpg, .jpeg, .png, .gif, .bmp, .webp
- Videos: .mp4, .mov, .avi, .wmv, .flv, .mkv
- File size limit: 100MB (configurable)

### 3. Responsive Design System
**File**: `webapp/static/css/layout.css`

**Breakpoints**:
- Large tablets/small desktops: 992px and down
- Tablets: 768px and down
- Mobile phones: 576px and down
- Extra small devices: 480px and down
- Landscape orientation: max-height 500px

**Mobile Optimizations**:
- Touch-friendly button sizes
- Responsive image sizing
- Adaptive navigation layout
- Keyboard avoidance
- Safe area handling
- High DPI display support

## User Workflows

### 1. Authentication Flow
1. User accesses protected page
2. Redirect to login page with return URL
3. User enters email/password
4. Form validation and submission
5. JWT token generation and storage
6. Redirect to original page or home

### 2. Daily Diary Entry Workflow
1. User navigates to home page
2. System displays current date entry (or empty state)
3. User can navigate to different dates
4. User clicks "Edit" or "Create Entry"
5. Entry editor opens with current data
6. User edits title, body, tags
7. User can insert assets
8. User saves entry
9. Return to home page with updated content

### 3. Asset Upload Workflow
1. User opens entry editor
2. User selects file via upload form
3. File validation (type, size)
4. File upload to server with progress
5. Server generates UUID filename
6. Thumbnail added to editor interface
7. User clicks thumbnail to insert
8. Markdown syntax added to entry body
9. Asset displays in rendered entry

### 4. Search and Discovery Workflow
1. User enters search query in header
2. System searches title and body content
3. Results displayed with highlighting
4. User can filter by tags
5. User can refine search parameters
6. User clicks result to view entry
7. User can edit entry from results

### 5. Layout Preference Management
1. User toggles layout button
2. CSS classes updated immediately
3. Preference saved to localStorage
4. Smooth transition animation
5. Preference persists across sessions

## Technical Architecture

### Template System
- Go template engine with custom functions
- Template inheritance (header/footer)
- Partial templates for reusable components
- Data binding with context variables
- Server-side rendering with client-side enhancement

### State Management
- Server-side session management
- Client-side localStorage for preferences
- Form state preservation
- Error state handling
- Loading state indicators

### API Integration
- RESTful API endpoints
- JWT-based authentication
- File upload handling
- Error response processing
- Network failure recovery

### Performance Features
- CSS/JS minification and caching
- Image optimization and lazy loading
- Responsive image serving
- Browser caching strategies
- Progressive enhancement

## Accessibility Features
- ARIA labels and roles
- Keyboard navigation support
- Screen reader compatibility
- High contrast mode support
- Focus management
- Semantic HTML structure
- Alternative text for images

## Browser Compatibility
- Modern browsers: Full functionality
- IE11: Limited functionality with fallbacks
- No JavaScript: CSS-only fallback layouts
- Progressive enhancement approach
- Graceful degradation strategies

## Mobile Responsiveness
- Mobile-first CSS approach
- Touch-friendly interface elements
- Responsive breakpoints
- Adaptive image sizing
- Mobile-optimized navigation
- Gesture support preparation
- Performance optimization for mobile devices
