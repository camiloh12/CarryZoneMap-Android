# CCW Map - Feature Description & User Interaction Guide

## What is CCW Map?

CCW Map is a mobile Android application for mapping and sharing information about carry zones - locations where users can or cannot carry concealed weapons (CCW). The app provides a collaborative, crowd-sourced platform where users can mark points of interest (POIs) with status indicators to help the community make informed decisions.

## Core Problem Solved

Users need to quickly determine whether a specific location (restaurant, store, government building, etc.) allows concealed carry. Rather than researching each location individually, CCW Map provides:
- Visual map-based interface for quick location lookup
- Community-contributed data for real-world establishments
- Offline access for use without internet connection
- Status indicators (green/yellow/red) for at-a-glance information

## Current Features

### 1. Authentication & User Management
**What it does:**
- Email and password-based user authentication
- Secure session persistence across app restarts
- Email confirmation via deep links (mobile and web fallback)

**User interaction:**
1. Open app → presented with login screen
2. Enter email and password → tap "Sign In"
3. Or tap "Sign Up" → enter credentials → receive confirmation email
4. Click email link → automatically signed in (mobile) or redirected to instructions (desktop)
5. Session persists → no need to login again until explicit logout

**Current limitations:**
- Only email/password auth (no social login)
- No password reset functionality
- No profile management or user settings

### 2. Map Visualization
**What it does:**
- Interactive map powered by MapLibre
- OpenStreetMap base tiles via MapTiler
- User location tracking with permission
- Camera controls (zoom, pan, tilt)
- POI labels from OpenStreetMap data

**User interaction:**
1. After login → main map screen appears
2. Map shows current location (if permission granted)
3. Pinch to zoom, drag to pan
4. POI labels appear on map (restaurants, stores, buildings, etc.)
5. Map loads POIs dynamically as user moves viewport

**Current limitations:**
- Fixed map style (no dark mode or alternative styles)
- No search functionality
- No filtering of POI types
- No custom map layers

### 3. Pin Creation (POI-Based)
**What it does:**
- Users create pins by clicking on POI labels
- Pins represent carry status for that location
- Three status options:
  - **Green**: Carry allowed / friendly
  - **Yellow**: Uncertain / mixed signals
  - **Red**: No carry / prohibited
- Each pin stores POI name, location, status, and metadata

**User interaction:**
1. User sees POI label on map (e.g., "Starbucks")
2. Taps on POI label → dialog appears
3. Dialog shows POI name as header
4. User selects status (green/yellow/red radio buttons)
5. Taps "Create" → pin saved and appears on map
6. Pin persists even when offline

**Design rationale:**
- POI-based creation prevents arbitrary pin placement
- Ensures pins are tied to real, verifiable locations
- POI names provide context (not just coordinates)

**Current limitations:**
- Can only create pins on existing POI labels
- Cannot create custom locations not in OpenStreetMap
- No additional metadata (notes, photos, timestamps visible to user)
- No verification or voting system

### 4. Pin Editing & Deletion
**What it does:**
- Users can update pin status or delete pins they created
- Edit dialog shows current status pre-selected
- Delete button removes pin from map and database

**User interaction:**
1. User taps existing pin on map → edit dialog appears
2. Dialog shows POI name and current status selected
3. User can:
   - Change status → tap "Save"
   - Delete pin → tap "Delete" → confirmation → pin removed
   - Cancel → tap outside dialog or "Cancel"
4. Changes sync across devices when online

**Current limitations:**
- Users can edit ANY pin (no ownership model yet)
- No edit history or audit trail
- No moderation or dispute resolution
- Delete is permanent (no undo)

### 5. Offline-First Operation
**What it does:**
- All features work without internet connection
- Local database stores all pins and user data
- Operations queued and synced when connection restored
- Seamless transition between offline and online

**User interaction:**
1. User creates/edits/deletes pins → instant UI update
2. If offline → operation stored locally, queued for sync
3. When internet restored → background sync runs automatically
4. User sees no difference (works the same online or offline)
5. Other users' changes downloaded when syncing

**Technical details:**
- Queue-based sync with retry logic (max 3 attempts)
- Last-write-wins conflict resolution (newest timestamp wins)
- Background sync via WorkManager (periodic checks)
- Network monitoring triggers immediate sync when online

**Current limitations:**
- No manual sync trigger
- No sync status indicator (user doesn't know if synced)
- Conflict resolution is simple (last-write-wins only)
- No merge strategies for complex conflicts

### 6. POI Data Fetching
**What it does:**
- Fetches points of interest from OpenStreetMap Overpass API
- Displays POI labels on map (restaurants, stores, etc.)
- Caches POI data for 30 minutes to handle API throttling
- Graceful degradation when API unavailable

**User interaction:**
1. User moves map → POIs load for current viewport
2. POI labels appear as text on map
3. Labels persist for 30 minutes (from cache)
4. User can click labels to create pins

**Technical details:**
- Viewport-based caching (rounded bounds as cache key)
- Keeps 20 most recent viewports in memory
- Falls back to stale cache if API fails
- Prevents excessive API calls during map navigation

**Current limitations:**
- No POI type filtering (shows everything)
- Limited to OpenStreetMap data (no custom POIs)
- No POI details or attributes shown
- Cache is memory-only (cleared on app restart)

## User Workflows

### Workflow 1: First-Time User
1. Install app → open → see login screen
2. Tap "Sign Up" → enter email/password → submit
3. Check email → click confirmation link → auto-login
4. Grant location permission → map centers on user location
5. Explore map → see POI labels and existing pins
6. Tap POI → select status → create first pin
7. Pin appears immediately on map

### Workflow 2: Checking a Location
1. Open app (already logged in) → map appears
2. Search for location mentally → navigate map to area
3. Look for existing pins (color-coded status)
4. If no pin exists → tap POI label → create pin
5. Note: Cannot search by address/name (manual navigation only)

### Workflow 3: Updating Information
1. User visits location → confirms carry policy
2. Opens app → navigates to location
3. Taps existing pin → edit dialog
4. Updates status if changed
5. Or deletes if location closed/no longer relevant

### Workflow 4: Offline Usage
1. User in area without signal
2. Opens app → map loads from cache
3. Creates/edits pins → instant local save
4. Later, when signal restored → changes sync automatically
5. User never needs to think about sync

## Data Model

### Pin Object
Each pin contains:
- **id**: Unique identifier (UUID)
- **name**: POI name from OpenStreetMap (e.g., "Starbucks")
- **latitude**: Geographic latitude
- **longitude**: Geographic longitude
- **status**: PinStatus enum (GREEN, YELLOW, RED)
- **createdBy**: User ID of creator
- **createdAt**: Timestamp of creation
- **lastModified**: Timestamp of last edit

### POI Object
Points of interest fetched from OpenStreetMap:
- **name**: POI name
- **latitude**: Geographic latitude
- **longitude**: Geographic longitude
- **type**: OSM type (amenity, shop, etc.)
- Source: OpenStreetMap Overpass API

## Technical Architecture (High-Level)

### Offline-First Philosophy
- **Instant responsiveness**: All operations complete in milliseconds locally
- **Works everywhere**: Full functionality without internet
- **Automatic sync**: Background sync when online
- **Conflict resolution**: Automatic merge of changes

### Cloud Synchronization
- **Supabase Backend**: PostgreSQL + PostGIS for geographic queries
- **Queue-based sync**: Operations queued and retried on failure
- **Real-time capable**: Infrastructure ready (not yet active)
- **Session management**: Secure authentication with token refresh

### Platform
- **Android only** (native Kotlin)
- **Minimum SDK**: API level not specified in docs
- **Target SDK**: Latest Android
- **Dependencies**: Jetpack Compose, Room, Hilt, MapLibre, Supabase

## What Makes This App Unique

1. **POI-Based Pin Creation**: Unlike arbitrary map markers, pins are tied to real OpenStreetMap locations with names
2. **Offline-First**: Full CRUD operations work without internet (rare for collaborative mapping apps)
3. **Simple Status Model**: Three colors (green/yellow/red) provide quick visual assessment
4. **Community-Driven**: Crowd-sourced data from users on the ground
5. **Privacy-Focused**: No social features or user profiles (yet) - just location data

## Current Gaps & Limitations

### User Experience
- **No search**: Cannot search for locations by name or address
- **No filtering**: Cannot filter pins by status or POI type
- **No details**: Cannot see who created pin, when, or why
- **No verification**: No way to verify accuracy or flag incorrect data
- **No undo**: Accidental deletes are permanent

### Features
- **No notes**: Cannot add text notes or context to pins
- **No photos**: Cannot attach images of signage
- **No voting**: No community verification or reliability scoring
- **No reporting**: No moderation or dispute resolution
- **No sharing**: Cannot share specific pins or locations

### Social & Community
- **No profiles**: Anonymous contributions (only user ID stored)
- **No reputation**: No trust system or user ratings
- **No comments**: No discussion or clarification
- **No notifications**: No alerts for nearby pins or updates

### Technical
- **No analytics**: No usage tracking or insights
- **No error reporting**: Crashes not reported automatically
- **No A/B testing**: No feature experimentation
- **Real-time disabled**: Infrastructure ready but not active

## Future Potential (From Roadmap)

Based on project documentation, planned features include:

### Near-Term
1. **Real-time sync**: Enable live updates when other users create/edit pins
2. **Search functionality**: Search for locations by name or address
3. **Status filtering**: Filter map to show only green/yellow/red pins
4. **Radius queries**: Find all pins within X miles of location

### Medium-Term
1. **Pin details**: Notes field for additional context
2. **Photo attachments**: Upload images of signage (with face/plate blurring via OpenCV)
3. **Voting system**: Upvote/downvote pins for accuracy
4. **User profiles**: View contribution history and reputation
5. **Manual sync**: User-triggered sync with status indicator

### Long-Term
1. **Comments & discussion**: Thread-based conversations on pins
2. **Moderation tools**: Flag incorrect pins, admin review
3. **Social features**: Follow users, share pins, notifications
4. **Advanced search**: Filter by POI type, date added, user ratings
5. **Multi-language**: International support

## Questions for Brainstorming & Improvement

### User Acquisition
- How do we bootstrap initial data in new areas?
- What incentives encourage users to contribute?
- How do we market to CCW community?

### Data Quality
- How do we ensure pin accuracy?
- What prevents malicious/false information?
- How do we handle disputes (e.g., user says green, another says red)?
- Should we implement reputation or verification systems?

### Feature Priorities
- What features would drive user retention?
- Search vs. voting vs. comments - which is most critical?
- How do we balance simplicity with power-user features?

### Legal & Ethical
- What liability exists for incorrect information?
- How do we handle sensitive locations (government, schools)?
- Should we verify user age/eligibility for CCW?
- Do we need disclaimers or terms of service enforcement?

### Monetization
- Should the app remain free?
- Potential revenue models: subscriptions, ads, premium features?
- Would monetization hurt community trust?

### Technical Evolution
- Should we support iOS?
- Web version for desktop users?
- API for third-party integrations?
- Export data for personal backup?

## Use This Document To...

- **Understand user needs**: What problems are we solving?
- **Identify gaps**: What's missing from the current experience?
- **Brainstorm features**: What would make this app indispensable?
- **Prioritize work**: Which improvements have highest impact?
- **Challenge assumptions**: Are we solving the right problems?
- **Design UX flows**: How should new features integrate?
- **Consider edge cases**: What can go wrong? How do we handle it?

## Example Prompts for Other LLMs

"Given the features described in FEATURES.md, suggest 5 high-impact improvements for user retention."

"Based on FEATURES.md, what are the top 3 risks or concerns with this app's approach?"

"Using FEATURES.md, design a voting system that ensures data quality without adding complexity."

"From FEATURES.md, identify opportunities to gamify contributions and drive engagement."

"Considering FEATURES.md, what legal disclaimers or terms of service are essential?"
