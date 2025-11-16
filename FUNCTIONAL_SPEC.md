# CarryZoneMap - Functional Specification

**Version:** 1.0
**Last Updated:** 2025-11-16
**Platform:** Cross-platform (Android/iOS via Flutter)
**Current Implementation:** Android (Kotlin + Jetpack Compose)

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Product Overview](#product-overview)
3. [Core Features](#core-features)
4. [User Flows](#user-flows)
5. [Data Models](#data-models)
6. [Architecture](#architecture)
7. [Authentication System](#authentication-system)
8. [Offline-First Sync Mechanism](#offline-first-sync-mechanism)
9. [UI/UX Specifications](#uiux-specifications)
10. [Third-Party Integrations](#third-party-integrations)
11. [Database Schema](#database-schema)
12. [Configuration & Setup](#configuration--setup)
13. [Testing Strategy](#testing-strategy)
14. [Build & Deployment](#build--deployment)

---

## Executive Summary

**CarryZoneMap** is a mobile application that enables users to collaboratively map and share information about concealed carry weapon (CCW) zones across the United States. The app provides a visual, interactive map where users can create, edit, and view location pins indicating whether firearms are allowed, uncertain, or prohibited at specific establishments.

### Key Capabilities

- **Interactive Mapping**: Pan/zoom map with color-coded location pins
- **User Authentication**: Email/password authentication with session persistence
- **POI Integration**: Points of interest from OpenStreetMap (bars, restaurants, schools, etc.)
- **Offline-First Architecture**: Full functionality without internet connection
- **Cloud Synchronization**: Automatic bidirectional sync with Supabase backend
- **Real-time Collaboration**: Live updates when other users modify pins (infrastructure ready)
- **Geographic Restrictions**: Pins limited to 50 US states + Washington DC

### Technical Highlights

- **Clean Architecture** with strict layer separation (Domain/Data/Presentation)
- **MVVM Pattern** with reactive state management
- **Queue-Based Sync** with automatic retry and conflict resolution
- **Last-Write-Wins** conflict resolution using timestamps
- **Background Sync** via WorkManager (Android) / Background Fetch (iOS planned)
- **Comprehensive Testing** (98 unit tests, 100% pass rate in Android version)

---

## Product Overview

### Problem Statement

Concealed carry permit holders need to know where they can legally carry firearms. Laws vary by state, locality, and property type. Static resources quickly become outdated, and no collaborative platform exists for real-time, crowd-sourced carry zone information.

### Solution

A mobile app with an interactive map where authenticated users can:
1. **View** color-coded pins showing carry status at locations
2. **Create** new pins at points of interest by selecting from map POIs
3. **Edit** existing pins to update status or add enforcement details
4. **Sync** data across devices automatically
5. **Work offline** with automatic background sync when connectivity returns

### Target Users

- Concealed carry permit holders in the United States
- Travelers visiting unfamiliar areas
- Users seeking crowd-sourced, up-to-date carry zone information

### Platform Requirements

- **Android**: Min SDK 26 (Android 8.0), Target SDK 34
- **iOS (Flutter)**: iOS 12.0+ (planned)
- **Internet**: Optional (full offline functionality)
- **Location**: Optional (enhances UX but not required)

---

## Core Features

### 1. Interactive Map Viewing

**Description**: Pan/zoom map with MapLibre showing user's location and carry zone pins.

**Functional Requirements**:
- Display base map tiles from MapTiler (or demo tiles if no API key)
- Show user's current location with blue dot (if permission granted)
- Render color-coded pin markers:
  - **Green (0)**: Firearms allowed
  - **Yellow (1)**: Status uncertain
  - **Red (2)**: No firearms allowed
- Display POI labels from OpenStreetMap (businesses, schools, bars, etc.)
- Support pan, zoom, rotate gestures
- Re-center FAB button to return to user location
- Viewport-based POI loading (fetch POIs when map moves)

**Technical Details**:
- **Map Library**: MapLibre (open-source, no API key required)
- **Tile Source**: MapTiler (optional API key) or demo tiles
- **Camera**: Initial zoom 15.0, user location zoom 16.0
- **POI Refresh**: Fetch on camera idle after 500ms debounce

### 2. Pin Creation via POI Selection

**Description**: Users create pins by tapping POI labels on the map, selecting a status, and adding optional details.

**Functional Requirements**:
- User taps on POI label (e.g., "Starbucks", "City Hall")
- App shows dialog with:
  - POI name as header
  - Status picker (Allowed/Uncertain/No Guns)
  - Restriction tag dropdown (required if status is "No Guns")
  - Security screening checkbox
  - Posted signage checkbox
  - "Create" and "Cancel" buttons
- Validate location is within US boundaries before creation
- Associate pin with authenticated user's ID
- Instant local save + queue for cloud sync

**Validation Rules**:
- Location must be within 50 US states + Washington DC
- If status is "No Guns", restriction tag is required
- POI name must not be empty
- User must be authenticated

**Technical Details**:
- POI names come from Overpass API (OpenStreetMap data)
- US boundary check: `24.396308 <= lat <= 49.384358, -125.0 <= lng <= -66.93457`
- Pin associated with user ID from auth session

### 3. Pin Editing & Deletion

**Description**: Users can edit existing pins to update status or delete them.

**Functional Requirements**:
- User taps existing pin on map
- App shows dialog with:
  - Current status pre-selected
  - Restriction tag (if applicable)
  - Security screening/signage checkboxes
  - "Save", "Delete", and "Cancel" buttons
- Only pin creator can delete a pin
- Any authenticated user can update status (crowd-sourced corrections)
- Changes sync to cloud immediately

**Technical Details**:
- Edit triggers database update with new `last_modified` timestamp
- Delete only allowed if `created_by == current_user_id`
- RLS policy on backend enforces deletion permissions

### 4. User Authentication

**Description**: Email/password authentication with session persistence across app restarts.

**Functional Requirements**:
- **Sign Up**:
  - Email + password input
  - Email format validation
  - Password minimum 6 characters
  - Email confirmation required (sent to inbox)
  - Deep link handling for email confirmation
- **Sign In**:
  - Email + password input
  - Remember session (no re-login on app restart)
  - Error handling for invalid credentials
- **Sign Out**:
  - Clear session and return to login screen
  - Option in map screen menu

**Email Confirmation Flow**:
- User signs up → Supabase sends confirmation email
- **Mobile**: Click link in email → App opens automatically → Auto-login
- **Desktop**: Click link → GitHub Pages fallback → Instructions to open on mobile
- Deep link schemes:
  - Custom: `com.carryzonemap.app://auth/callback`
  - HTTPS: `https://camiloh12.github.io/CarryZoneMap-Android/auth/callback`

**Technical Details**:
- Backend: Supabase Auth
- Session storage: Encrypted secure storage (platform-specific)
- Token refresh: Automatic via SDK
- Password policy: Min 6 chars (configurable in Supabase dashboard)

### 5. Offline-First Synchronization

**Description**: Full app functionality without internet, with automatic bidirectional sync when online.

**Functional Requirements**:
- **Offline Mode**:
  - Create/edit/delete pins without internet
  - All changes saved locally to Room database (Android) / SQLite (Flutter)
  - Operations queued for later upload
  - UI shows instant feedback (no loading spinners)
- **Online Mode**:
  - Automatically detect network connectivity
  - Upload queued operations to Supabase
  - Download remote changes and merge with local data
  - Resolve conflicts using last-write-wins strategy
  - Retry failed operations up to 3 times
- **Background Sync**:
  - Periodic sync every 15 minutes (WorkManager on Android)
  - Sync on app launch
  - Sync on network reconnection
  - Optional: Real-time subscriptions for instant updates

**Conflict Resolution**:
- Compare `last_modified` timestamps
- Newer timestamp wins (local or remote)
- Both sides updated to match winner
- No data loss (last edit always preserved)

**Technical Details**:
- Local DB: Room (Android) / Drift or sqflite (Flutter)
- Remote DB: Supabase PostgreSQL
- Network monitor: Reactive stream of connectivity state
- Sync queue table: Stores pending CREATE/UPDATE/DELETE operations
- Max retries: 3 attempts per operation
- Retry backoff: Exponential (1s, 2s, 4s)

### 6. POI Fetching & Caching

**Description**: Fetch points of interest from OpenStreetMap Overpass API and cache them locally.

**Functional Requirements**:
- Fetch POIs when user moves map to new area
- Cache POIs for 30 minutes to reduce API calls
- Handle Overpass API rate limiting gracefully (return cached data if throttled)
- Display POI labels on map (name, type)
- POI types: Restaurants, bars, schools, government buildings, hospitals, places of worship, stadiums, etc.

**Caching Strategy**:
- **Cache Key**: Rounded viewport bounds (precision: 2 decimal places)
- **Cache Duration**: 30 minutes
- **Cache Size**: 20 most recent viewports
- **Fallback**: Return stale cache if API unavailable

**Technical Details**:
- API: Overpass API (https://overpass-api.de/api/interpreter)
- Query: Overpass QL for amenities, tourism, leisure, building types
- Rate limit: 2 requests/second (enforced by API)
- Cache cleanup: LRU eviction when > 20 entries

### 7. Location Services

**Description**: Access user's current location to center map and enable location-based features.

**Functional Requirements**:
- Request location permission on first launch
- Handle permission denied gracefully (map still works)
- Display user location as blue dot with accuracy circle
- Update location continuously (10-second interval)
- Re-center button to snap map to user location

**Technical Details**:
- **Android**: FusedLocationProviderClient (Google Play Services)
- **iOS (Flutter)**: Geolocator package
- Update interval: 10 seconds
- Min interval: 5 seconds
- Accuracy: High (GPS + network)

### 8. Geographic Restrictions

**Description**: Enforce US-only pin placement to comply with app scope.

**Functional Requirements**:
- Block pin creation outside 50 US states + Washington DC
- Show error message: "Pins can only be placed within the 50 US states and Washington DC"
- Boundary check before showing create dialog
- Defensive check before database save

**Boundary Coordinates**:
- **Latitude**: 24.396308 (southernmost FL Keys) to 49.384358 (northernmost MN)
- **Longitude**: -125.0 (westernmost WA) to -66.93457 (easternmost ME)
- **Exclusions**: Hawaii, Alaska, territories (for simplicity)

**Technical Details**:
- Check in ViewModel before showing dialog
- Double-check in repository before database write
- Logging for attempted violations (security/analytics)

---

## User Flows

### Flow 1: First-Time User Onboarding

1. **App Launch** → User sees login screen
2. **Tap "Sign Up"** → Email/password form appears
3. **Enter credentials** → Validation (email format, password length)
4. **Tap "Sign Up"** → Loading spinner
5. **Success** → Message: "Please check your email to confirm"
6. **Check email** → Click confirmation link
7. **Deep link** → App opens, session auto-imported
8. **Map Screen** → User sees map centered on their location (if permission granted)

### Flow 2: Creating a Pin

1. **User** taps POI label on map (e.g., "Chipotle")
2. **App** queries features at tap point
3. **App** extracts POI name from feature
4. **App** validates location is within US boundaries
5. **App** shows pin creation dialog:
   - Header: "Chipotle" (POI name)
   - Status picker: "Allowed" (default)
   - Restriction tag: Hidden (only shown if status = "No Guns")
   - Checkboxes: Security screening, Posted signage
   - Buttons: "Create", "Cancel"
6. **User** selects "No Guns" → Restriction tag dropdown appears
7. **User** selects "Private Property" from dropdown
8. **User** checks "Posted Signage"
9. **User** taps "Create"
10. **App** creates Pin object:
    - `name = "Chipotle"`
    - `location = {lat, lng}`
    - `status = NO_GUN`
    - `restrictionTag = PRIVATE_PROPERTY`
    - `hasPostedSignage = true`
    - `createdBy = current_user_id`
11. **App** writes to local DB (instant UI update)
12. **App** queues operation for sync
13. **Map** re-renders with new red pin at location
14. **Background** syncs to Supabase when online

### Flow 3: Editing a Pin

1. **User** taps existing pin on map
2. **App** queries features at tap point
3. **App** finds pin by ID
4. **App** shows edit dialog:
   - Current status pre-selected
   - Current restriction tag (if applicable)
   - Current checkbox states
   - Buttons: "Save", "Delete", "Cancel"
5. **User** changes status to "Allowed"
6. **User** taps "Save"
7. **App** updates Pin:
   - `status = ALLOWED`
   - `restrictionTag = null` (cleared)
   - `last_modified = now()`
8. **App** writes to local DB
9. **App** queues update operation
10. **Map** re-renders with green pin
11. **Background** syncs to cloud

### Flow 4: Offline Usage

1. **User** opens app with **no internet**
2. **App** loads pins from local database
3. **User** creates/edits pins normally
4. **App** saves all changes to local DB
5. **App** queues operations in sync queue
6. **User** sees changes instantly on map
7. **Internet** reconnects
8. **Network Monitor** detects connectivity
9. **SyncManager** automatically uploads queued operations
10. **SyncManager** downloads remote changes
11. **App** merges changes (last-write-wins)
12. **Map** updates with latest data

### Flow 5: Multi-Device Sync

1. **User A** creates pin on **Device 1** (Android phone)
2. **Pin** syncs to Supabase cloud
3. **User A** opens app on **Device 2** (iPad)
4. **Device 2** downloads pin from cloud
5. **User B** (different user) edits same pin on **Device 3**
6. **Edit** syncs to cloud with newer `last_modified` timestamp
7. **Device 1** performs background sync
8. **SyncManager** compares timestamps: remote is newer
9. **Device 1** updates local DB with remote version
10. **Map** on Device 1 shows updated pin
11. **(Optional)** Real-time subscription instantly pushes update to all devices without waiting for background sync

---

## Data Models

### 1. Pin (Domain Model)

**Description**: Core entity representing a location pin on the map.

**Fields**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `id` | String (UUID) | Yes | Auto-generated | Unique identifier |
| `name` | String | Yes | - | POI name (e.g., "Starbucks") |
| `location` | Location | Yes | - | Geographic coordinates |
| `status` | PinStatus | Yes | ALLOWED | Carry zone status |
| `restrictionTag` | RestrictionTag? | No | null | Reason for restriction (required if status = NO_GUN) |
| `hasSecurityScreening` | Boolean | Yes | false | Active security screening present |
| `hasPostedSignage` | Boolean | Yes | false | "No guns" signage visible |
| `metadata` | PinMetadata | Yes | Auto-generated | Creation/modification metadata |

**Business Rules**:
- If `status == NO_GUN`, `restrictionTag` must not be null
- `location` must be within US boundaries
- `id` is immutable after creation
- `metadata.lastModified` auto-updates on any change

**Methods**:
```kotlin
// Kotlin example (Flutter equivalent in Dart)
fun withNextStatus(): Pin  // Cycle to next status
fun withStatus(newStatus: PinStatus): Pin  // Set specific status
fun withMetadata(newMetadata: PinMetadata): Pin  // Update metadata
```

### 2. PinStatus (Enum)

**Description**: Represents carry zone status.

**Values**:

| Value | Display Name | Color Code | Description |
|-------|--------------|------------|-------------|
| `ALLOWED` | "Allowed" | 0 (Green) | Firearms allowed |
| `UNCERTAIN` | "Uncertain" | 1 (Yellow) | Status unknown/unverified |
| `NO_GUN` | "No Guns" | 2 (Red) | Firearms prohibited |

**Methods**:
```kotlin
fun next(): PinStatus  // ALLOWED -> UNCERTAIN -> NO_GUN -> ALLOWED
fun fromColorCode(code: Int): PinStatus  // Convert integer to enum
```

### 3. RestrictionTag (Enum)

**Description**: Reason why firearms carry is restricted (applicable only for NO_GUN status).

**Values**:

| Tag | Display Name | Description |
|-----|--------------|-------------|
| `FEDERAL_PROPERTY` | "Federal Government Property" | Federal building, post office, military base, VA facility, courthouse, tribal land |
| `AIRPORT_SECURE` | "Airport Secure Area" | Past TSA security checkpoint |
| `STATE_LOCAL_GOVT` | "State/Local Government Property" | State/local government building, courthouse, polling place |
| `SCHOOL_K12` | "School (K-12)" | Elementary, middle, or high school campus |
| `COLLEGE_UNIVERSITY` | "College/University" | College or university campus |
| `BAR_ALCOHOL` | "Bar/Alcohol Establishment" | Bar, restaurant, or venue with alcohol restrictions |
| `HEALTHCARE` | "Healthcare Facility" | Hospital, medical clinic, childcare facility |
| `PLACE_OF_WORSHIP` | "Place of Worship" | Church, mosque, temple, religious facility |
| `SPORTS_ENTERTAINMENT` | "Sports/Entertainment Venue" | Sports stadium, arena, concert hall, amusement park |
| `PRIVATE_PROPERTY` | "Private Property" | Private business, workplace, or property restricting carry |

**Methods**:
```kotlin
fun fromString(name: String?): RestrictionTag?  // Parse from string
```

### 4. Location (Value Object)

**Description**: Geographic coordinates (immutable value object).

**Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `latitude` | Double | Yes | Latitude (-90 to 90) |
| `longitude` | Double | Yes | Longitude (-180 to 180) |

**Factory Methods**:
```kotlin
fun fromLngLat(lng: Double, lat: Double): Location
fun fromLatLng(lat: Double, lng: Double): Location
```

**Validation**:
- Latitude: -90 to 90
- Longitude: -180 to 180

### 5. PinMetadata (Data Class)

**Description**: Metadata about pin creation and modification.

**Fields**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `createdBy` | String? | No | null | User ID who created pin |
| `createdAt` | Long | Yes | Current timestamp | Creation timestamp (epoch millis) |
| `lastModified` | Long | Yes | Current timestamp | Last modification timestamp |
| `photoUri` | String? | No | null | Photo URL (future feature) |
| `notes` | String? | No | null | User notes (future feature) |
| `votes` | Int | Yes | 0 | Voting count (future feature) |

### 6. User (Domain Model)

**Description**: Authenticated user.

**Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | String (UUID) | Yes | Unique user identifier from auth provider |
| `email` | String? | No | User's email address |

### 7. Poi (Domain Model)

**Description**: Point of Interest from OpenStreetMap.

**Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | String | Yes | OSM node/way ID |
| `name` | String | Yes | POI name (e.g., "Starbucks") |
| `latitude` | Double | Yes | Latitude |
| `longitude` | Double | Yes | Longitude |
| `type` | String | Yes | POI type (e.g., "restaurant", "bar", "school") |
| `tags` | Map<String, String> | No | Additional OSM tags |

### 8. SyncOperation (Sealed Class)

**Description**: Type of sync operation in queue.

**Values**:
- `CREATE`: Insert new pin to remote
- `UPDATE`: Update existing pin on remote
- `DELETE`: Delete pin from remote

**Database Representation**:
- Stored as string: "CREATE", "UPDATE", "DELETE"

### 9. SyncStatus (Sealed Class)

**Description**: Current sync state.

**Values**:

| State | Properties | Description |
|-------|------------|-------------|
| `Idle` | - | No sync in progress |
| `Syncing` | `pendingCount: Int` | Sync in progress |
| `Success` | `uploadCount: Int, downloadCount: Int` | Sync completed |
| `Error` | `message: String, retryable: Boolean` | Sync failed |

### 10. AuthState (Sealed Class)

**Description**: Current authentication state.

**Values**:

| State | Properties | Description |
|-------|------------|-------------|
| `Loading` | - | Checking auth status |
| `Authenticated` | `user: User` | User logged in |
| `Unauthenticated` | - | User logged out |

---

## Architecture

### Clean Architecture Layers

The app follows **Clean Architecture** with strict dependency rules:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  (UI, ViewModels, State)                                    │
│  - Jetpack Compose / Flutter Widgets                        │
│  - StateFlow / Stream for reactive updates                  │
│  - Dependency injection via Hilt / GetIt                    │
└──────────────────────┬──────────────────────────────────────┘
                       │ Depends on
                       ↓
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer                            │
│  (Business Logic, Models, Repository Interfaces)            │
│  - Pure Dart/Kotlin (no framework dependencies)             │
│  - Domain models: Pin, User, Location, etc.                 │
│  - Repository interfaces: PinRepository, AuthRepository     │
│  - Business rules and validation logic                      │
└──────────────────────┬──────────────────────────────────────┘
                       │ Depends on
                       ↓
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                             │
│  (Repository Implementations, Data Sources)                 │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ Local DB   │  │ SyncManager  │  │ Remote (Supabase)│   │
│  │ Room/SQLite│←─│ Queue Ops    │─→│ Auth, Postgrest  │   │
│  │ Instant    │  │ Retry Logic  │  │ Realtime (opt)   │   │
│  │ Reads      │  │ Conflict Res │  │                  │   │
│  └────────────┘  └──────────────┘  └──────────────────┘   │
│                                                             │
│  NetworkMonitor: Reactive connectivity tracking             │
└─────────────────────────────────────────────────────────────┘
```

**Dependency Rule**: Dependencies only flow inward (Presentation → Domain → Data). Domain layer has ZERO framework imports.

### Design Patterns

#### 1. MVVM (Model-View-ViewModel)

**View** (MapScreen):
- Stateless Composable / StatelessWidget
- Collects StateFlow / Stream from ViewModel
- Renders UI based on state
- Delegates events to ViewModel

**ViewModel** (MapViewModel):
- Owns UI state (MapUiState)
- Exposes StateFlow / Stream
- Handles user events (create pin, edit pin, etc.)
- Calls repository methods
- Updates state immutably

**Model** (Domain models):
- Pure data classes (Pin, User, etc.)
- Business logic methods
- No UI dependencies

#### 2. Repository Pattern

**Interface** (PinRepository):
- Defined in domain layer
- Abstract CRUD operations
- Returns domain models

**Implementation** (PinRepositoryImpl):
- Defined in data layer
- Coordinates local and remote data sources
- Handles sync queue operations
- Maps entities ↔ domain models

#### 3. Offline-First Pattern

**Write Path**:
1. ViewModel calls `repository.addPin(pin)`
2. Repository writes to local DB **immediately**
3. Repository queues operation for sync
4. Local DB emits Flow update
5. ViewModel receives update
6. UI re-renders (instant feedback)
7. Background: SyncManager uploads to cloud

**Read Path**:
1. Repository exposes `Flow<List<Pin>>`
2. Flow sources from local DB
3. ViewModel collects Flow
4. UI re-renders on each emission
5. Background: SyncManager downloads remote changes → writes to local DB → Flow emits

#### 4. Chain of Responsibility (Click Handling)

**Problem**: MapScreen needs to handle clicks on different feature types (existing pins, Overpass POIs, MapTiler POIs).

**Solution**: Chain of detectors, each checks if it can handle the click:

```kotlin
// Android example
interface FeatureDetector {
    fun canHandle(map, screenPoint, clickPoint): Boolean
    fun handle(map, screenPoint, clickPoint): Unit
}

class ExistingPinDetector : FeatureDetector { ... }
class OverpassPoiDetector : FeatureDetector { ... }
class MapTilerPoiDetector : FeatureDetector { ... }

class FeatureClickHandler(detectors: List<FeatureDetector>) {
    fun handleClick(map, screenPoint, clickPoint) {
        detectors.firstOrNull { it.canHandle(...) }?.handle(...)
    }
}
```

#### 5. Single Responsibility Principle

**MapScreen** delegates responsibilities to helper classes:

- **CameraController**: Camera positioning only
- **MapLayerManager**: POI layer management only
- **LocationComponentManager**: Location component setup only
- **FeatureClickHandler**: Click handling only
- **FeatureLayerManager**: Pin layer rendering only

### Data Flow Example: Creating a Pin

```
User taps POI
    ↓
MapScreen.onMapClick()
    ↓
FeatureClickHandler.handleClick()
    ↓
OverpassPoiDetector.handle()
    ↓
MapViewModel.showCreatePinDialog(name, lng, lat)
    ↓
MapViewModel updates state:
    uiState.copy(pinDialogState = PinDialogState.Creating(...))
    ↓
PinDialog composable re-renders with data
    ↓
User selects status and taps "Create"
    ↓
MapViewModel.confirmPinDialog()
    ↓
MapViewModel calls:
    pinRepository.addPin(pin)
    ↓
PinRepositoryImpl:
    1. pinDao.insertPin(entity)      // Local DB write
    2. syncManager.queuePinForUpload(pin)  // Queue for cloud
    ↓
Room emits Flow<List<PinEntity>>
    ↓
Repository maps to Flow<List<Pin>>
    ↓
MapViewModel collects Flow, updates state:
    uiState.copy(pins = newPins)
    ↓
MapScreen re-renders with new pin
    ↓
Background: SyncManager uploads to Supabase
```

---

## Authentication System

### Flow: Sign Up

1. **User enters email + password** on LoginScreen
2. **Client validates**:
   - Email format (regex)
   - Password length (min 6 chars)
3. **AuthViewModel calls** `authRepository.signUpWithEmail(email, password)`
4. **SupabaseAuthRepository**:
   - Calls `supabase.auth.signUpWith(Email)`
   - Supabase creates user in `auth.users` table
   - Supabase sends confirmation email
5. **Response handling**:
   - Success with empty user ID → Email confirmation required
   - Success with user ID → Immediate login (if email confirmation disabled)
   - Failure → Error message shown
6. **UI shows**: "Please check your email to confirm your account"

### Flow: Email Confirmation

**Mobile Device**:
1. User clicks link in email
2. Android: Intent filter matches HTTPS deep link
3. MainActivity.handleDeepLink() extracts tokens from URL fragment
4. Calls `auth.importAuthToken(accessToken, refreshToken)`
5. Session imported → AuthState changes to Authenticated
6. MainActivity navigates to MapScreen

**Desktop Browser**:
1. User clicks link in email
2. Browser opens GitHub Pages fallback page
3. Page shows instructions: "Please open this link on your mobile device"
4. User can copy link or scan QR code (future feature)

**Deep Link Schemes**:
- Custom: `com.carryzonemap.app://auth/callback#access_token=...&refresh_token=...`
- HTTPS: `https://camiloh12.github.io/CarryZoneMap-Android/auth/callback#access_token=...&refresh_token=...`

### Flow: Sign In

1. **User enters credentials** on LoginScreen
2. **Client validates** (same as sign up)
3. **AuthViewModel calls** `authRepository.signInWithEmail(email, password)`
4. **SupabaseAuthRepository**:
   - Calls `supabase.auth.signInWith(Email)`
   - Supabase validates credentials
   - Returns session with access token + refresh token
5. **Success**: AuthState changes to Authenticated(user)
6. **MainActivity observes** auth state change
7. **UI navigates** to MapScreen

### Flow: Session Persistence

1. **App launch**: AuthRepository checks for existing session
2. **Supabase SDK** auto-loads session from secure storage
3. **If valid session**: AuthState = Authenticated
4. **If no session**: AuthState = Unauthenticated
5. **Token refresh**: Supabase SDK automatically refreshes expired access tokens using refresh token

### Flow: Sign Out

1. **User taps** "Sign Out" in MapScreen menu
2. **MapViewModel calls** `authRepository.signOut()`
3. **SupabaseAuthRepository**:
   - Calls `supabase.auth.signOut()`
   - Clears session from secure storage
4. **AuthState** changes to Unauthenticated
5. **MainActivity observes** state change
6. **UI navigates** to LoginScreen

### Security

**Password Storage**:
- Never stored locally
- Hashed with bcrypt on Supabase backend
- Only access/refresh tokens stored (encrypted)

**Token Management**:
- Access token: Short-lived (1 hour)
- Refresh token: Long-lived (30 days)
- Auto-refresh handled by SDK
- Tokens stored in platform-specific secure storage:
  - Android: EncryptedSharedPreferences
  - iOS: Keychain

**Row Level Security (RLS)**:
- Enforced at database level
- Users can only delete their own pins (`created_by == auth.uid()`)
- Users can update any pin (crowd-sourced corrections)
- Anyone can read pins (public map data)

---

## Offline-First Sync Mechanism

### Architecture

**Components**:

1. **Local Database** (Room / SQLite):
   - `pins` table: User pins with full schema
   - `sync_queue` table: Pending operations

2. **SyncManager**:
   - Orchestrates upload/download
   - Implements conflict resolution
   - Manages retry logic

3. **NetworkMonitor**:
   - Reactive stream of connectivity state
   - Triggers sync on network reconnection

4. **SyncWorker** (Background Task):
   - Periodic sync every 15 minutes
   - Survives app restarts
   - Only runs when online

5. **Remote Database** (Supabase PostgreSQL):
   - Authoritative source of truth for synced data
   - PostGIS extension for geographic queries

### Sync Operations

#### Create Pin

**Local (Instant)**:
1. Insert into `pins` table
2. Insert into `sync_queue` table:
   - `pin_id = pin.id`
   - `operation_type = "CREATE"`
   - `timestamp = now()`

**Remote (Background)**:
1. SyncManager reads queue
2. For each CREATE operation:
   - Fetch pin from local DB
   - POST to Supabase `/pins` endpoint
   - If success: Delete from queue
   - If failure: Increment retry count, log error

#### Update Pin

**Local (Instant)**:
1. Update `pins` table, set `last_modified = now()`
2. Delete any existing queue operations for this pin
3. Insert UPDATE operation into `sync_queue`

**Remote (Background)**:
1. Fetch pin from local DB
2. PATCH to Supabase `/pins/{id}` endpoint
3. Success: Delete from queue
4. Failure: Retry with backoff

#### Delete Pin

**Local (Instant)**:
1. Delete from `pins` table
2. Delete any existing queue operations for this pin
3. Insert DELETE operation into `sync_queue`

**Remote (Background)**:
1. DELETE to Supabase `/pins/{id}` endpoint
2. Success: Delete from queue
3. Failure: Retry (note: pin already deleted locally)

### Download & Conflict Resolution

**Download Phase** (runs after upload):
1. Fetch all pins from Supabase: `GET /pins`
2. For each remote pin:
   - Check if exists locally
   - **If not exists**: Insert into local DB
   - **If exists**: Compare `last_modified` timestamps
     - Remote newer: Update local with remote data
     - Local newer: Keep local (will upload on next sync)
     - Same: No action

**Last-Write-Wins**:
```kotlin
fun mergeRemotePin(remotePin: Pin): Boolean {
    val localPin = pinDao.getPinById(remotePin.id)

    if (localPin == null) {
        pinDao.insertPin(remotePin)  // New pin, insert
        return true
    }

    if (remotePin.metadata.lastModified > localPin.metadata.lastModified) {
        pinDao.updatePin(remotePin)  // Remote is newer, update
        return true
    }

    // Local is newer, keep local
    return false
}
```

### Retry Logic

**Max Retries**: 3 attempts per operation

**Backoff Strategy**: Exponential
- Retry 1: Immediate
- Retry 2: 2 seconds
- Retry 3: 4 seconds
- After 3 failures: Remove from queue, log error

**Queue Management**:
```sql
-- SyncQueueEntity schema
CREATE TABLE sync_queue (
    id TEXT PRIMARY KEY,
    pin_id TEXT NOT NULL,
    operation_type TEXT NOT NULL,  -- CREATE, UPDATE, DELETE
    timestamp INTEGER NOT NULL,
    retry_count INTEGER DEFAULT 0,
    last_error TEXT
);
```

### Network Monitoring

**Implementation**:
- Android: ConnectivityManager.NetworkCallback
- iOS (Flutter): connectivity_plus package
- Reactive stream: `Flow<Boolean>` / `Stream<bool>`
- Distinct emissions (no duplicate events)

**Triggers**:
- Network reconnection → Immediate sync
- App launch → Check connectivity, sync if online
- Background worker → Periodic sync (15 min)

### Real-Time Subscriptions (Optional)

**Infrastructure Ready, Not Enabled by Default**:

```kotlin
// SyncManager
fun startRealtimeSubscription(): Flow<String> {
    return remoteDataSource.subscribeToChanges()
        .map { event ->
            when (event) {
                is PinChangeEvent.Insert -> handleRealtimeInsert(event.pin)
                is PinChangeEvent.Update -> handleRealtimeUpdate(event.pin)
                is PinChangeEvent.Delete -> handleRealtimeDelete(event.pinId)
            }
        }
}
```

**Benefits**:
- Instant updates across devices (no 15-minute delay)
- Live collaboration (see other users' changes immediately)

**Trade-offs**:
- Increased battery usage (WebSocket connection)
- More complex conflict scenarios
- Requires Supabase Realtime enabled ($10/mo)

---

## UI/UX Specifications

### Screens

#### 1. LoginScreen

**Layout**:
```
┌──────────────────────────────────────┐
│                                      │
│         [App Logo/Title]             │
│                                      │
│  ┌────────────────────────────────┐ │
│  │ Email                          │ │
│  └────────────────────────────────┘ │
│                                      │
│  ┌────────────────────────────────┐ │
│  │ Password                  [👁] │ │
│  └────────────────────────────────┘ │
│                                      │
│  [ Error/Success Message ]           │
│                                      │
│  ┌────────────────────────────────┐ │
│  │       Sign In / Sign Up        │ │
│  └────────────────────────────────┘ │
│                                      │
│       [Toggle: "Need an account?    │
│        Sign Up" / "Have an account? │
│        Sign In"]                     │
│                                      │
└──────────────────────────────────────┘
```

**Components**:
- Email TextField (keyboard type: email)
- Password TextField (obscureText: true, toggle visibility)
- Submit Button (disabled while loading)
- Loading indicator (shows during auth)
- Error Snackbar (red, dismissible)
- Success message (green, auto-dismiss after 5s)
- Toggle link (switches between Sign In / Sign Up modes)

**State**:
- `isLoading: Boolean`
- `error: String?`
- `successMessage: String?`
- `isSignUpMode: Boolean`

#### 2. MapScreen

**Layout**:
```
┌──────────────────────────────────────┐
│  [Menu ☰]            [Sign Out]      │ ← AppBar
├──────────────────────────────────────┤
│                                      │
│                                      │
│           [Interactive Map]          │
│         (MapLibre + Pins + POIs)     │
│                                      │
│                                      │
│                                      │
│                                      │
│                              [📍]    │ ← Re-center FAB
└──────────────────────────────────────┘
    [Snackbar for errors]               ← Bottom overlay
```

**Map Features**:
- Base map tiles (MapTiler or demo)
- User location (blue dot with accuracy circle)
- Pin markers (color-coded by status):
  - Green circle: Allowed
  - Yellow circle: Uncertain
  - Red circle: No Guns
- POI labels (text overlays from Overpass API)

**AppBar**:
- Title: "CCW Map"
- Actions:
  - Sign Out IconButton

**FAB** (Floating Action Button):
- Icon: Location crosshairs
- Position: Bottom-right
- Action: Center map on user location

**Gestures**:
- Pan: Drag to move map
- Zoom: Pinch to zoom in/out
- Rotate: Two-finger rotate
- Tap: Open dialog for creating/editing pin

**State**:
- `pins: List<Pin>`
- `pois: List<Poi>`
- `currentLocation: Location?`
- `isLoading: Boolean`
- `error: String?`
- `hasLocationPermission: Boolean`
- `pinDialogState: PinDialogState`

#### 3. PinDialog (Modal)

**Layout - Creating Mode**:
```
┌──────────────────────────────────────┐
│  Create Pin: [POI Name]          [✕] │
├──────────────────────────────────────┤
│                                      │
│  Status:                             │
│  ( ) Allowed                         │
│  ( ) Uncertain                       │
│  ( ) No Guns                         │
│                                      │
│  [If No Guns selected:]              │
│  Restriction Tag: [Dropdown ▼]      │
│    - Federal Government Property     │
│    - Airport Secure Area             │
│    - ...                             │
│                                      │
│  Enforcement Details:                │
│  [ ] Active security screening       │
│  [ ] Posted "No Guns" signage        │
│                                      │
│  ┌────────────────────────────────┐ │
│  │          Create                │ │
│  └────────────────────────────────┘ │
│  ┌────────────────────────────────┐ │
│  │          Cancel                │ │
│  └────────────────────────────────┘ │
└──────────────────────────────────────┘
```

**Layout - Editing Mode**:
```
┌──────────────────────────────────────┐
│  Edit Pin: [POI Name]            [✕] │
├──────────────────────────────────────┤
│  [Same as Creating Mode]             │
│                                      │
│  ┌────────────────────────────────┐ │
│  │           Save                 │ │
│  └────────────────────────────────┘ │
│  ┌────────────────────────────────┐ │
│  │          Delete                │ │ ← Only in Edit mode
│  └────────────────────────────────┘ │
│  ┌────────────────────────────────┐ │
│  │          Cancel                │ │
│  └────────────────────────────────┘ │
└──────────────────────────────────────┘
```

**Components**:
- Header with POI name
- Radio buttons for status selection
- Dropdown for restriction tag (conditional: only if status = No Guns)
- Checkboxes for enforcement details
- Action buttons (Create/Save, Delete, Cancel)

**Validation**:
- If status = NO_GUN, restriction tag is required (disable Create/Save button if not selected)
- All other fields optional

### Theme & Colors

**Material 3 Design** (Android) / **Cupertino** (iOS):

**Pin Status Colors**:
- Allowed: Green (`#4CAF50`)
- Uncertain: Yellow/Amber (`#FFC107`)
- No Guns: Red (`#F44336`)

**Map Constants**:
- Default zoom: 15.0
- User location zoom: 16.0
- Min zoom: 5.0
- Max zoom: 20.0

**Fonts**:
- Android: Roboto (Material default)
- iOS: SF Pro (Cupertino default)

---

## Third-Party Integrations

### 1. MapLibre (Mapping Library)

**Purpose**: Interactive map rendering

**Platform**:
- Android: `maplibre-android` SDK
- iOS (Flutter): `maplibre_gl` package

**Configuration**:
- Tile source: MapTiler or demo tiles
- API key: Optional (demo tiles work without key)
- Style URL: `https://api.maptiler.com/maps/streets/style.json?key={API_KEY}`

**Features Used**:
- Map rendering
- Camera control (pan, zoom, rotate)
- Location component (blue dot)
- Symbol layers (pin markers, POI labels)
- Querying features at point (tap handling)

**Pin Layer Setup**:
```kotlin
// Add GeoJSON source with pin features
map.addSource(GeoJsonSource("pins-source", featureCollection))

// Add symbol layer for pin markers
map.addLayer(SymbolLayer("pins-layer", "pins-source")
    .withProperties(
        iconImage("pin-{color_code}"),  // pin-0, pin-1, pin-2
        iconSize(1.2),
        iconAllowOverlap(true)
    ))
```

### 2. Supabase (Backend as a Service)

**Purpose**: Authentication, database, real-time sync

**SDK**:
- Android: `io.github.jan-tennert.supabase:supabase-kt` (v3.0.1)
- Flutter: `supabase_flutter` package

**Modules Used**:

#### Supabase Auth
- Email/password authentication
- Session management
- Token refresh
- Email confirmation

**Configuration**:
- Site URL: `https://camiloh12.github.io/CarryZoneMap-Android`
- Redirect URLs:
  - `com.carryzonemap.app://auth/callback`
  - `https://camiloh12.github.io/CarryZoneMap-Android/auth/callback`
- Email template: Supabase default with custom Site URL

#### Supabase Postgrest (Database API)
- RESTful API over PostgreSQL
- Automatic CRUD endpoints
- Row Level Security enforcement
- Filtering, sorting, pagination

**Endpoints**:
- `GET /pins` - Fetch all pins
- `POST /pins` - Create pin
- `PATCH /pins?id=eq.{id}` - Update pin
- `DELETE /pins?id=eq.{id}` - Delete pin

**Filters**:
```
GET /pins?longitude=gte.-123&longitude=lte.-122&latitude=gte.37&latitude=lte.38
```

#### Supabase Realtime (Optional)
- WebSocket-based live updates
- Subscribe to table changes (INSERT, UPDATE, DELETE)
- Broadcast messages

**Subscription**:
```kotlin
supabase.realtime.channel("pins")
    .on(ChannelEvent.POSTGRES_CHANGES) { event ->
        // Handle INSERT/UPDATE/DELETE
    }
    .subscribe()
```

### 3. Overpass API (OpenStreetMap POI Data)

**Purpose**: Fetch points of interest for map labels

**API**: `https://overpass-api.de/api/interpreter`

**Query Language**: Overpass QL

**Example Query**:
```
[out:json][timeout:25];
(
  node["amenity"]({{bbox}});
  node["tourism"]({{bbox}});
  node["leisure"]({{bbox}});
  way["amenity"]({{bbox}});
  way["tourism"]({{bbox}});
);
out center;
```

**Rate Limiting**:
- 2 requests per second
- Consider caching to reduce API calls

**Response Handling**:
- Parse JSON response
- Extract name, type, coordinates
- Cache for 30 minutes
- Fallback to cache if API throttled

### 4. Google Play Services Location (Android)

**Purpose**: Access device location

**SDK**: `com.google.android.gms:play-services-location`

**Flutter Alternative**: `geolocator` package

**Features**:
- FusedLocationProviderClient (high accuracy)
- Location updates (continuous)
- Permission handling

**Configuration**:
- Update interval: 10 seconds
- Min update interval: 5 seconds
- Priority: High accuracy (GPS + network)

---

## Database Schema

### Local Database (Room / SQLite)

#### Table: `pins`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | TEXT | PRIMARY KEY | UUID |
| `name` | TEXT | NOT NULL | POI name |
| `longitude` | REAL | NOT NULL | Longitude |
| `latitude` | REAL | NOT NULL | Latitude |
| `status` | INTEGER | NOT NULL | 0=ALLOWED, 1=UNCERTAIN, 2=NO_GUN |
| `restriction_tag` | TEXT | NULLABLE | Enum name (e.g., "FEDERAL_PROPERTY") |
| `has_security_screening` | INTEGER | NOT NULL DEFAULT 0 | Boolean (0/1) |
| `has_posted_signage` | INTEGER | NOT NULL DEFAULT 0 | Boolean (0/1) |
| `photo_uri` | TEXT | NULLABLE | Photo URL (future) |
| `notes` | TEXT | NULLABLE | User notes (future) |
| `votes` | INTEGER | NOT NULL DEFAULT 0 | Vote count (future) |
| `created_by` | TEXT | NULLABLE | User ID |
| `created_at` | INTEGER | NOT NULL | Epoch milliseconds |
| `last_modified` | INTEGER | NOT NULL | Epoch milliseconds |

**Indexes**:
- `idx_pins_status` on `status`
- `idx_pins_created_at` on `created_at DESC`
- `idx_pins_last_modified` on `last_modified DESC`

#### Table: `sync_queue`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | TEXT | PRIMARY KEY | UUID |
| `pin_id` | TEXT | NOT NULL | Pin ID to sync |
| `operation_type` | TEXT | NOT NULL | CREATE, UPDATE, DELETE |
| `timestamp` | INTEGER | NOT NULL | Queue time (epoch millis) |
| `retry_count` | INTEGER | NOT NULL DEFAULT 0 | Number of attempts |
| `last_error` | TEXT | NULLABLE | Error message from last failure |

**Indexes**:
- `idx_sync_queue_pin_id` on `pin_id`

### Remote Database (Supabase PostgreSQL)

#### Table: `pins`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY DEFAULT gen_random_uuid() | Pin ID |
| `name` | TEXT | NOT NULL | POI name |
| `longitude` | DOUBLE PRECISION | NOT NULL | Longitude |
| `latitude` | DOUBLE PRECISION | NOT NULL | Latitude |
| `location` | GEOGRAPHY(POINT, 4326) | GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)) STORED | PostGIS geography column |
| `status` | INTEGER | NOT NULL CHECK (status IN (0, 1, 2)) | Pin status |
| `restriction_tag` | restriction_tag_type | NULLABLE | Enum type (see below) |
| `has_security_screening` | BOOLEAN | NOT NULL DEFAULT false | Enforcement detail |
| `has_posted_signage` | BOOLEAN | NOT NULL DEFAULT false | Enforcement detail |
| `photo_uri` | TEXT | NULLABLE | Photo URL |
| `notes` | TEXT | NULLABLE | User notes |
| `votes` | INTEGER | DEFAULT 0 | Vote count |
| `created_by` | UUID | REFERENCES auth.users(id) ON DELETE SET NULL | User ID |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() NOT NULL | Creation timestamp |
| `last_modified` | TIMESTAMPTZ | DEFAULT NOW() NOT NULL | Last modification timestamp |

**Indexes**:
- `idx_pins_status` on `status`
- `idx_pins_restriction_tag` on `restriction_tag`
- `idx_pins_created_by` on `created_by`
- `idx_pins_created_at` on `created_at DESC`
- `idx_pins_last_modified` on `last_modified DESC`
- `idx_pins_location` (GIST index) on `location` (for geographic queries)

**Constraints**:
- `check_red_pin_has_tag`: Ensures `status = 2` pins have a `restriction_tag`

#### Enum: `restriction_tag_type`

```sql
CREATE TYPE restriction_tag_type AS ENUM (
    'FEDERAL_PROPERTY',
    'AIRPORT_SECURE',
    'STATE_LOCAL_GOVT',
    'SCHOOL_K12',
    'COLLEGE_UNIVERSITY',
    'BAR_ALCOHOL',
    'HEALTHCARE',
    'PLACE_OF_WORSHIP',
    'SPORTS_ENTERTAINMENT',
    'PRIVATE_PROPERTY'
);
```

#### Trigger: Auto-update `last_modified`

```sql
CREATE OR REPLACE FUNCTION update_last_modified()
RETURNS TRIGGER AS $$
BEGIN
  NEW.last_modified = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql
SET search_path = '';

CREATE TRIGGER set_last_modified
  BEFORE UPDATE ON pins
  FOR EACH ROW
  EXECUTE FUNCTION update_last_modified();
```

#### Row Level Security (RLS) Policies

```sql
-- Enable RLS
ALTER TABLE pins ENABLE ROW LEVEL SECURITY;

-- Policy: Anyone can read pins
CREATE POLICY "Pins are viewable by everyone"
  ON pins FOR SELECT
  USING (true);

-- Policy: Authenticated users can insert pins (must match their user ID)
CREATE POLICY "Authenticated users can insert pins"
  ON pins FOR INSERT
  WITH CHECK (auth.uid() = created_by);

-- Policy: Authenticated users can update any pin (crowd-sourced corrections)
CREATE POLICY "Users can update any pin"
  ON pins FOR UPDATE
  USING (auth.role() = 'authenticated')
  WITH CHECK (auth.role() = 'authenticated');

-- Policy: Users can only delete their own pins
CREATE POLICY "Users can delete own pins"
  ON pins FOR DELETE
  USING (auth.uid() = created_by);
```

---

## Configuration & Setup

### Environment Variables / Build Config

**Android (BuildConfig)**:
```kotlin
buildConfigField("String", "MAPTILER_API_KEY", "\"${localProperties["MAPTILER_API_KEY"] ?: ""}\"")
buildConfigField("String", "SUPABASE_URL", "\"${localProperties["SUPABASE_URL"] ?: ""}\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties["SUPABASE_ANON_KEY"] ?: ""}\"")
```

**Flutter (.env file)**:
```
MAPTILER_API_KEY=your_key_here
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your_anon_key_here
```

**local.properties (Android) / .env (Flutter)**:
```properties
# MapTiler (optional - demo tiles work without it)
MAPTILER_API_KEY=get_from_maptiler.com

# Supabase (required)
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Supabase Setup Steps

1. **Create Supabase Project**:
   - Go to https://supabase.com/dashboard
   - Click "New Project"
   - Name: CarryZoneMap
   - Region: Choose closest to users
   - Database password: Save securely

2. **Run Database Migrations**:
   - Go to SQL Editor in Supabase dashboard
   - Execute `001_initial_schema.sql`
   - Execute `002_add_poi_name_to_pins.sql`
   - Execute `003_add_restriction_tags.sql`

3. **Get API Credentials**:
   - Settings → API
   - Copy Project URL
   - Copy anon public key

4. **Configure Authentication**:
   - Authentication → URL Configuration
   - Site URL: `https://camiloh12.github.io/CarryZoneMap-Android` (or your domain)
   - Redirect URLs:
     - `com.carryzonemap.app://auth/callback`
     - `https://camiloh12.github.io/CarryZoneMap-Android/auth/callback`

5. **Enable PostGIS** (should be done by migration):
   - Database → Extensions
   - Enable `postgis`

6. **Optional: Enable Realtime**:
   - Database → Replication
   - Enable replication for `pins` table
   - Note: Requires paid plan ($10/mo)

### MapTiler Setup (Optional)

1. Go to https://www.maptiler.com/
2. Create free account
3. Create API key
4. Add to `local.properties` / `.env`

**Note**: App works with demo tiles if no key provided.

### GitHub Pages Fallback (Email Confirmation)

**Setup**:
1. Enable GitHub Pages for repository
2. Create `auth/callback.html` with fallback UI:
   ```html
   <html>
     <body>
       <h1>Email Confirmed!</h1>
       <p>Please open this link on your mobile device to complete sign-in.</p>
       <p>If you're on mobile, the app should open automatically.</p>
     </body>
   </html>
   ```

### Platform-Specific Permissions

**Android (AndroidManifest.xml)**:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Deep link intent filters -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="com.carryzonemap.app"
          android:host="auth"
          android:pathPrefix="/callback" />
</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https"
          android:host="camiloh12.github.io"
          android:pathPrefix="/CarryZoneMap-Android/auth/callback" />
</intent-filter>
```

**iOS (Info.plist)**:
```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>This app needs access to your location to show nearby carry zones on the map.</string>

<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>com.carryzonemap.app</string>
        </array>
    </dict>
</array>
```

---

## Testing Strategy

### Unit Tests

**Domain Layer**:
- Test all domain model methods (e.g., `Pin.withNextStatus()`)
- Test value object validation (e.g., `Location` coordinate bounds)
- Test enum methods (e.g., `PinStatus.next()`, `RestrictionTag.fromString()`)

**Data Layer**:
- Test mappers (Entity ↔ Domain, DTO ↔ Domain)
- Test repository with fake DAOs and SyncManager
- Test SyncManager logic (upload, download, conflict resolution)
- Test network monitor (simulate online/offline)

**Presentation Layer**:
- Test ViewModels with fake repositories
- Verify state updates on user actions
- Test validation logic (e.g., US boundary check)
- Test dialog state transitions

**Example Test (MapViewModel)**:
```kotlin
@Test
fun `creating pin updates state correctly`() = runTest {
    val fakeRepo = FakePinRepository()
    val viewModel = MapViewModel(fakeRepo, ...)

    viewModel.showCreatePinDialog("Starbucks", -122.0, 37.0)
    viewModel.onDialogStatusSelected(PinStatus.NO_GUN)
    viewModel.onDialogRestrictionTagSelected(RestrictionTag.PRIVATE_PROPERTY)
    viewModel.confirmPinDialog()

    val state = viewModel.uiState.first()
    assertEquals(1, state.pins.size)
    assertEquals("Starbucks", state.pins[0].name)
    assertEquals(PinStatus.NO_GUN, state.pins[0].status)
}
```

### Integration Tests

- Test Room database migrations
- Test Supabase API calls with test project
- Test sync flow end-to-end (local → remote → local)
- Test authentication flow (sign up, confirm, sign in, sign out)

### UI Tests

**Compose/Flutter**:
- Test LoginScreen (sign up, sign in, validation)
- Test MapScreen (map loads, pins render)
- Test PinDialog (create, edit, validation)

**Example UI Test**:
```kotlin
@Test
fun `clicking poi shows create dialog`() {
    composeTestRule.setContent {
        MapScreen(viewModel = viewModel)
    }

    // Simulate map click on POI
    viewModel.showCreatePinDialog("Test POI", -122.0, 37.0)

    // Verify dialog shown
    composeTestRule.onNodeWithText("Create Pin: Test POI").assertIsDisplayed()
    composeTestRule.onNodeWithText("Allowed").assertIsDisplayed()
}
```

### Test Coverage Goals

- **Domain models**: 100% (pure logic, easy to test)
- **Mappers**: 100% (critical for data consistency)
- **Repositories**: 90%+ (core business logic)
- **ViewModels**: 80%+ (user interactions)
- **UI**: 50%+ (smoke tests for critical flows)

### Android Test Suite (Reference)

The Android implementation has **98 unit tests** with **100% pass rate**:
- 27 tests for domain models
- 38 tests for mappers (Entity, Pin, Supabase)
- 12 tests for repository (offline-first pattern)
- 14 tests for MapViewModel
- 7 tests for legacy components

**Total execution time**: < 3 minutes

---

## Build & Deployment

### Build Types

**Debug**:
- Debuggable
- No code obfuscation
- Logging enabled
- Fast build time

**Release**:
- Code obfuscation (ProGuard / R8)
- Logging disabled (Timber tree removed)
- Optimized APK/IPA size
- Signed with release key

### Android Build Commands

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (for direct distribution)
./gradlew assembleRelease

# Release App Bundle (for Google Play)
./gradlew bundleRelease

# Run tests before build
./gradlew test

# Run all quality checks (tests + lint + detekt + ktlint)
./gradlew check
```

### Flutter Build Commands

```bash
# Debug builds
flutter run  # Hot reload during development

# Release APK (Android)
flutter build apk --release

# Release App Bundle (Android)
flutter build appbundle --release

# Release IPA (iOS)
flutter build ios --release

# Run tests
flutter test
```

### Code Quality Tools

**Android**:
- **Detekt** (static analysis): `./gradlew detekt`
- **KtLint** (code formatting): `./gradlew ktlintCheck`
- **JaCoCo** (code coverage): `./gradlew jacocoTestReport`

**Flutter**:
- **Dart analyzer**: `flutter analyze`
- **Dart formatter**: `dart format .`
- **Coverage**: `flutter test --coverage`

### CI/CD Pipeline (GitHub Actions)

**Triggers**:
- Pull requests to `develop` branch
- Pull requests to `master` branch
- Push to `master` (production deployment)

**Jobs**:
1. **Build**: Compile app (debug + release)
2. **Test**: Run all unit tests
3. **Lint**: Run Detekt/KtLint or Dart analyzer
4. **Deploy** (master only): Upload to Google Play / App Store

**Example Workflow** (.github/workflows/ci.yml):
```yaml
name: CI/CD

on:
  pull_request:
    branches: [develop, master]
  push:
    branches: [master]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - run: ./gradlew build
      - run: ./gradlew test
      - run: ./gradlew detekt
      - run: ./gradlew ktlintCheck

  deploy:
    if: github.ref == 'refs/heads/master'
    needs: build
    runs-on: ubuntu-latest
    steps:
      - run: ./gradlew bundleRelease
      - uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.SERVICE_ACCOUNT_JSON }}
          packageName: com.carryzonemap.app
          releaseFiles: app/build/outputs/bundle/release/app-release.aab
          track: production
```

### Release Process (Git Flow)

1. **Feature Development**:
   ```bash
   git checkout develop
   git checkout -b feature/new-feature
   # Make changes
   git commit -m "feat: Add new feature"
   git push origin feature/new-feature
   # Create PR to develop
   ```

2. **Integration** (develop branch):
   - Merge feature branches via PRs
   - CI runs tests and checks
   - Manual testing on develop

3. **Release Preparation**:
   ```bash
   git checkout develop
   git checkout -b release/v1.0.0
   # Update version numbers
   # Update CHANGELOG.md
   git commit -m "chore: Prepare v1.0.0"
   git push origin release/v1.0.0
   ```

4. **Production Deployment**:
   ```bash
   # Merge release to master
   git checkout master
   git merge release/v1.0.0
   git tag v1.0.0
   git push origin master --tags

   # Merge release back to develop
   git checkout develop
   git merge release/v1.0.0
   git push origin develop

   # Delete release branch
   git branch -d release/v1.0.0
   ```

5. **Automated Deployment**:
   - GitHub Actions detects push to master
   - Runs CI checks
   - Builds release bundle
   - Uploads to Google Play / App Store
   - Creates GitHub release with APK/IPA

### Version Numbering

**Semantic Versioning**: `MAJOR.MINOR.PATCH`

- **MAJOR**: Breaking changes (e.g., 1.0.0 → 2.0.0)
- **MINOR**: New features (e.g., 1.0.0 → 1.1.0)
- **PATCH**: Bug fixes (e.g., 1.0.0 → 1.0.1)

**Android** (build.gradle.kts):
```kotlin
versionCode = 1  // Increment for each release
versionName = "1.0.0"
```

**Flutter** (pubspec.yaml):
```yaml
version: 1.0.0+1  # version+buildNumber
```

---

## Appendix

### API Endpoints Reference

**Supabase Postgrest**:

```
GET    /pins                     # Fetch all pins
GET    /pins?id=eq.{uuid}        # Fetch pin by ID
POST   /pins                     # Create pin
PATCH  /pins?id=eq.{uuid}        # Update pin
DELETE /pins?id=eq.{uuid}        # Delete pin

# Geographic query (bounding box)
GET /pins?longitude=gte.{west}&longitude=lte.{east}&latitude=gte.{south}&latitude=lte.{north}

# Filter by status
GET /pins?status=eq.2  # Only red (NO_GUN) pins

# Order by timestamp
GET /pins?order=last_modified.desc
```

**Supabase Auth**:

```
POST /auth/v1/signup           # Sign up with email
POST /auth/v1/token?grant_type=password  # Sign in
POST /auth/v1/logout           # Sign out
POST /auth/v1/token?grant_type=refresh_token  # Refresh token
```

### Database Migration Files

**001_initial_schema.sql**: Creates `pins` table with PostGIS, RLS policies, indexes

**002_add_poi_name_to_pins.sql**: Adds `name` column for POI names

**003_add_restriction_tags.sql**: Adds `restriction_tag`, `has_security_screening`, `has_posted_signage` columns, creates enum type, adds constraint

### US Boundary Coordinates

**Continental US Bounding Box**:
- **Min Latitude**: 24.396308 (Key West, FL)
- **Max Latitude**: 49.384358 (Northwest Angle, MN)
- **Min Longitude**: -125.0 (Cape Alava, WA)
- **Max Longitude**: -66.93457 (West Quoddy Head, ME)

**Excluded** (for simplicity):
- Alaska
- Hawaii
- US territories (Puerto Rico, Guam, etc.)

### Key Dependencies (Android)

```gradle
// Kotlin
kotlin("android") version "2.0.21"

// Jetpack Compose
androidx.compose:compose-bom:2024.04.00

// Hilt (DI)
com.google.dagger:hilt-android:2.51

// Room (local DB)
androidx.room:room-runtime:2.6.1
androidx.room:room-ktx:2.6.1

// Supabase
io.github.jan-tennert.supabase:postgrest-kt:3.0.1
io.github.jan-tennert.supabase:auth-kt:3.0.1
io.github.jan-tennert.supabase:realtime-kt:3.0.1

// Ktor (networking)
io.ktor:ktor-client-android:3.0.1

// MapLibre
org.maplibre.gl:android-sdk:11.0.0

// Location
com.google.android.gms:play-services-location:21.3.0

// WorkManager
androidx.work:work-runtime-ktx:2.9.0

// Logging
com.jakewharton.timber:timber:5.0.1

// Testing
junit:junit:4.13.2
org.mockito.kotlin:mockito-kotlin:5.4.0
app.cash.turbine:turbine:1.1.0
org.robolectric:robolectric:4.13
```

### Key Dependencies (Flutter - Planned)

```yaml
dependencies:
  flutter:
    sdk: flutter

  # State management
  provider: ^6.1.0

  # Database
  drift: ^2.16.0  # Or sqflite

  # Supabase
  supabase_flutter: ^2.3.0

  # Map
  maplibre_gl: ^0.20.0

  # Location
  geolocator: ^11.0.0

  # Network
  connectivity_plus: ^5.0.0

  # Storage
  shared_preferences: ^2.2.0
  flutter_secure_storage: ^9.0.0

  # Background tasks
  workmanager: ^0.5.0

dev_dependencies:
  flutter_test:
    sdk: flutter
  mockito: ^5.4.0
  integration_test:
    sdk: flutter
```

---

## Summary

This functional specification captures all the requirements, architecture, and implementation details of the CarryZoneMap Android application. It can be used as a comprehensive reference to:

1. **Rebuild the app in Flutter/Dart** for cross-platform Android/iOS deployment
2. **Onboard new developers** to the project
3. **Document product requirements** for stakeholders
4. **Guide testing efforts** with detailed user flows and edge cases
5. **Plan future enhancements** based on the existing foundation

**Key Takeaways**:

- **Clean Architecture** ensures maintainability and testability
- **Offline-first** provides excellent UX regardless of connectivity
- **Queue-based sync** with retry logic guarantees data consistency
- **Last-write-wins** conflict resolution is simple and effective
- **Supabase** provides robust backend with minimal setup
- **MapLibre** offers open-source mapping without API key requirements
- **Comprehensive testing** (98 tests) validates business logic

**Next Steps for Flutter Implementation**:

1. Set up Flutter project structure following Clean Architecture
2. Implement domain models (Pin, User, Location, etc.)
3. Set up Drift/sqflite for local database
4. Integrate Supabase SDK for auth and remote DB
5. Implement repository pattern with offline-first sync
6. Build UI with Flutter widgets (Material/Cupertino)
7. Integrate MapLibre for interactive map
8. Write unit/integration tests
9. Set up CI/CD pipeline
10. Deploy to Google Play and App Store

---

**Document Version**: 1.0
**Created By**: Claude Code (AI-assisted)
**Date**: 2025-11-16
**Contact**: camilo@kyberneticlabs.com
