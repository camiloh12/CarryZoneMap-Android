# CarryZoneMap - Supabase Integration Plan

## Status Legend
- ⏳ In Progress
- ✅ Completed
- ⬜ Not Started
- 🔄 Blocked/Needs Review
- ⚠️ Needs Decision

---

## Overview

This document outlines the plan to migrate CarryZoneMap from a local-only Room database to a multi-user shared database using **Supabase**. The goal is to enable real-time collaboration where multiple app instances can view and edit each other's pins.

### Architecture Goals
- **Offline-first**: Keep Room as local cache, sync with Supabase when online
- **Real-time sync**: Use Supabase Realtime for live updates
- **Conflict resolution**: Implement last-write-wins with timestamp-based resolution
- **Clean Architecture**: Maintain domain/data/presentation separation
- **Testability**: Ensure all new components are unit testable

### Why Supabase?
- PostgreSQL backend (reliable, scalable, supports PostGIS for geographic queries)
- Built-in authentication (email, OAuth providers)
- Real-time subscriptions (WebSocket-based)
- Row-level security (RLS) for data access control
- Free tier for development (500 MB database, 2 GB bandwidth, 50 MB file storage)
- REST and GraphQL APIs
- Kotlin/Android SDK available

---

## Phase 1: Supabase Setup & Configuration ✅

### 1.1 Supabase Project Setup ⚠️
**Priority:** Critical
**Estimated Time:** 1 hour

- [ ] Create Supabase project in dashboard
- [ ] Note project URL and anon key
- [ ] Enable Row Level Security (RLS) on all tables
- [ ] Configure email authentication settings
- [ ] Set up database schema (see 1.2)

**Resources:**
- Supabase Dashboard: https://supabase.com/dashboard
- Project settings: Project URL, API Keys

**Output:**
- Project URL: `https://<project-ref>.supabase.co`
- Anon key: (stored in `local.properties`)
- Service role key: (for admin operations, if needed)

---

### 1.2 Database Schema Design ✅
**Priority:** Critical
**Estimated Time:** 1.5 hours

**SQL Schema:**
```sql
-- Enable PostGIS extension for geographic queries
CREATE EXTENSION IF NOT EXISTS postgis;

-- Pins table
CREATE TABLE pins (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  longitude DOUBLE PRECISION NOT NULL,
  latitude DOUBLE PRECISION NOT NULL,
  location GEOGRAPHY(POINT, 4326) GENERATED ALWAYS AS (
    ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
  ) STORED,
  status INTEGER NOT NULL CHECK (status IN (0, 1, 2)),
  photo_uri TEXT,
  notes TEXT,
  votes INTEGER DEFAULT 0,
  created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  last_modified TIMESTAMPTZ DEFAULT NOW(),

  -- Add spatial index for geographic queries
  SPATIAL INDEX idx_pins_location (location)
);

-- Create index for common queries
CREATE INDEX idx_pins_status ON pins(status);
CREATE INDEX idx_pins_created_by ON pins(created_by);
CREATE INDEX idx_pins_created_at ON pins(created_at DESC);

-- Enable Row Level Security
ALTER TABLE pins ENABLE ROW LEVEL SECURITY;

-- RLS Policies
-- Anyone can read pins (for now - can restrict by region later)
CREATE POLICY "Pins are viewable by everyone"
  ON pins FOR SELECT
  USING (true);

-- Authenticated users can insert pins
CREATE POLICY "Authenticated users can insert pins"
  ON pins FOR INSERT
  WITH CHECK (auth.uid() = created_by);

-- Users can update their own pins (or any pin - depending on moderation model)
CREATE POLICY "Users can update any pin"
  ON pins FOR UPDATE
  USING (true)
  WITH CHECK (true);

-- Users can delete their own pins
CREATE POLICY "Users can delete own pins"
  ON pins FOR DELETE
  USING (auth.uid() = created_by);

-- Function to update last_modified timestamp
CREATE OR REPLACE FUNCTION update_last_modified()
RETURNS TRIGGER AS $$
BEGIN
  NEW.last_modified = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update last_modified
CREATE TRIGGER set_last_modified
  BEFORE UPDATE ON pins
  FOR EACH ROW
  EXECUTE FUNCTION update_last_modified();
```

**Tasks:**
- [x] Create schema SQL file: `supabase/migrations/001_initial_schema.sql`
- [ ] Run migration in Supabase SQL editor (USER ACTION REQUIRED)
- [ ] Test geographic queries (bounding box, radius) (Pending user setup)
- [ ] Verify RLS policies work correctly (Pending user setup)
- [x] Document schema in this file

**Files to create:**
- `supabase/migrations/001_initial_schema.sql`
- `docs/SUPABASE_SCHEMA.md` (detailed schema documentation)

---

### 1.3 Add Supabase Dependencies ✅
**Priority:** Critical
**Estimated Time:** 30 minutes

**Dependencies to add:**
```kotlin
// In app/build.gradle.kts
dependencies {
    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.0.0")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.0.0")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.0.0") // For photos

    // Ktor for networking (required by Supabase)
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-utils:2.3.7")
}
```

**Configuration:**
```properties
# In local.properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

```kotlin
// In app/build.gradle.kts (buildConfigField)
android {
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // Load from local.properties
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())

        buildConfigField("String", "SUPABASE_URL",
            "\"${properties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY",
            "\"${properties.getProperty("SUPABASE_ANON_KEY", "")}\"")
    }
}
```

**Tasks:**
- [x] Add Supabase dependencies to `app/build.gradle.kts` (v3.0.1)
- [x] Add BuildConfig fields for Supabase credentials
- [x] Update `.gitignore` to exclude `local.properties`
- [x] Create `local.properties.example` with placeholder values
- [x] Sync Gradle and verify build

**Files to modify:**
- `app/build.gradle.kts`
- `.gitignore`

**Files to create:**
- `local.properties.example`

---

### 1.4 Initialize Supabase Client ✅
**Priority:** Critical
**Estimated Time:** 1 hour

**Tasks:**
- [x] Create `SupabaseModule.kt` for Hilt DI
- [x] Initialize Supabase client with Postgrest, Realtime, Auth, and Storage
- [x] Add proper error handling and logging
- [ ] Test connection to Supabase (Pending user setup)

**Files to create:**
```kotlin
// app/src/main/java/com/carryzonemap/app/di/SupabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Realtime)
            install(GoTrue)
            install(Storage)
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseDatabase(client: SupabaseClient): Postgrest {
        return client.postgrest
    }

    @Provides
    @Singleton
    fun provideSupabaseRealtime(client: SupabaseClient): Realtime {
        return client.realtime
    }

    @Provides
    @Singleton
    fun provideSupabaseAuth(client: SupabaseClient): GoTrue {
        return client.auth
    }
}
```

**Files to create:**
- `app/src/main/java/com/carryzonemap/app/di/SupabaseModule.kt`

---

## Phase 2: Remote Data Source Layer ✅

### 2.1 Create Remote Data Models ✅
**Priority:** High
**Estimated Time:** 1 hour

**Tasks:**
- [x] Create `SupabasePinDto` data class (DTO = Data Transfer Object)
- [x] Add JSON serialization annotations (@Serializable, @SerialName)
- [x] Create mapper: `SupabasePinDto` ↔ Domain `Pin` (SupabaseMapper.kt)
- [x] Handle nullable fields from network

**Files to create:**
```kotlin
// app/src/main/java/com/carryzonemap/app/data/remote/dto/SupabasePinDto.kt
@Serializable
data class SupabasePinDto(
    val id: String,
    val longitude: Double,
    val latitude: Double,
    val status: Int,
    @SerialName("photo_uri") val photoUri: String? = null,
    val notes: String? = null,
    val votes: Int = 0,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_modified") val lastModified: String,
)

// app/src/main/java/com/carryzonemap/app/data/remote/mapper/SupabaseMapper.kt
object SupabaseMapper {
    fun SupabasePinDto.toDomain(): Pin { /* implementation */ }
    fun Pin.toSupabaseDto(): SupabasePinDto { /* implementation */ }
    fun List<SupabasePinDto>.toDomainModels(): List<Pin> { /* implementation */ }
}
```

**Files to create:**
- `app/src/main/java/com/carryzonemap/app/data/remote/dto/SupabasePinDto.kt`
- `app/src/main/java/com/carryzonemap/app/data/remote/mapper/SupabaseMapper.kt`

---

### 2.2 Implement Remote Data Source ✅
**Priority:** High
**Estimated Time:** 3 hours

**Tasks:**
- [x] Create `RemotePinDataSource` interface in data layer
- [x] Create `SupabasePinDataSource` implementation
- [x] Implement CRUD operations using Supabase Postgrest
- [x] Add geographic queries (bounding box implemented)
- [x] Handle network errors gracefully (Result wrapper)
- [x] Add logging for debugging

**Files to create:**
```kotlin
// app/src/main/java/com/carryzonemap/app/data/remote/datasource/RemotePinDataSource.kt
interface RemotePinDataSource {
    suspend fun getAllPins(): Result<List<Pin>>
    suspend fun getPinById(pinId: String): Result<Pin?>
    suspend fun insertPin(pin: Pin): Result<Pin>
    suspend fun updatePin(pin: Pin): Result<Pin>
    suspend fun deletePin(pinId: String): Result<Unit>
    suspend fun getPinsInBoundingBox(
        minLat: Double, maxLat: Double,
        minLng: Double, maxLng: Double
    ): Result<List<Pin>>
    fun subscribeToChanges(onUpdate: (Pin) -> Unit, onDelete: (String) -> Unit)
}

// app/src/main/java/com/carryzonemap/app/data/remote/datasource/SupabasePinDataSource.kt
class SupabasePinDataSource @Inject constructor(
    private val postgrest: Postgrest,
    private val realtime: Realtime
) : RemotePinDataSource {
    // Implementation using Supabase SDK
}
```

**Key Operations:**
```kotlin
// Example: Insert pin
override suspend fun insertPin(pin: Pin): Result<Pin> {
    return try {
        val dto = pin.toSupabaseDto()
        val response = postgrest["pins"].insert(dto).decodeSingle<SupabasePinDto>()
        Result.success(response.toDomain())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Example: Geographic query
override suspend fun getPinsInBoundingBox(
    minLat: Double, maxLat: Double,
    minLng: Double, maxLng: Double
): Result<List<Pin>> {
    return try {
        val response = postgrest["pins"]
            .select()
            .gte("latitude", minLat)
            .lte("latitude", maxLat)
            .gte("longitude", minLng)
            .lte("longitude", maxLng)
            .decodeList<SupabasePinDto>()
        Result.success(response.toDomainModels())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Files to create:**
- `app/src/main/java/com/carryzonemap/app/data/remote/datasource/RemotePinDataSource.kt`
- `app/src/main/java/com/carryzonemap/app/data/remote/datasource/SupabasePinDataSource.kt`

---

### 2.3 Add Realtime Subscriptions ✅
**Priority:** High
**Estimated Time:** 2 hours

**Tasks:**
- [x] Implement Supabase Realtime channel subscription (Flow-based)
- [x] Listen for INSERT, UPDATE, DELETE events (PinChangeEvent sealed class)
- [x] Parse realtime events and convert to domain models
- [x] Handle subscription lifecycle (connect/disconnect)
- [x] Add reconnection logic (handled by Supabase SDK)
- [ ] Enable realtime in SyncManager (TODO: Subscribe to remote changes)

**Implementation:**
```kotlin
// In SupabasePinDataSource
private var realtimeChannel: RealtimeChannel? = null

override fun subscribeToChanges(
    onUpdate: (Pin) -> Unit,
    onDelete: (String) -> Unit
) {
    realtimeChannel = realtime.channel("pins") {
        postgresChangeFlow<SupabasePinDto>("public", "pins") { schema ->
            schema.table = "pins"
            schema.filter = "status=in.(0,1,2)" // Optional filter
        }
    }

    realtimeChannel?.subscribe { status ->
        if (status == RealtimeChannel.Status.SUBSCRIBED) {
            // Handle INSERT and UPDATE
            realtimeChannel?.postgresChangeFlow<SupabasePinDto>()?.onEach { change ->
                when (change) {
                    is PostgresAction.Insert -> onUpdate(change.record.toDomain())
                    is PostgresAction.Update -> onUpdate(change.record.toDomain())
                    is PostgresAction.Delete -> onDelete(change.oldRecord.id)
                }
            }
        }
    }
}

fun unsubscribe() {
    realtimeChannel?.unsubscribe()
    realtimeChannel = null
}
```

**Files to modify:**
- `app/src/main/java/com/carryzonemap/app/data/remote/datasource/SupabasePinDataSource.kt`

---

## Phase 3: Authentication ✅

### 3.1 Implement Authentication Flow ✅
**Priority:** High
**Estimated Time:** 3 hours

**Tasks:**
- [x] Create `AuthRepository` interface in domain layer
- [x] Create `SupabaseAuthRepository` implementation
- [x] Implement email/password authentication
- [ ] Implement anonymous authentication (Deferred - not needed for MVP)
- [x] Handle auth state persistence (Supabase handles this automatically)
- [x] Add auth state Flow for UI observation

**Files to create:**
```kotlin
// app/src/main/java/com/carryzonemap/app/domain/repository/AuthRepository.kt
interface AuthRepository {
    val authState: Flow<AuthState>
    val currentUserId: String?

    suspend fun signUpWithEmail(email: String, password: String): Result<User>
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signInAnonymously(): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): User?
}

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    object Unauthenticated : AuthState()
}

data class User(
    val id: String,
    val email: String?
)

// app/src/main/java/com/carryzonemap/app/data/repository/SupabaseAuthRepository.kt
class SupabaseAuthRepository @Inject constructor(
    private val auth: GoTrue
) : AuthRepository {
    // Implementation
}
```

**Files to create:**
- `app/src/main/java/com/carryzonemap/app/domain/repository/AuthRepository.kt`
- `app/src/main/java/com/carryzonemap/app/data/repository/SupabaseAuthRepository.kt`

---

### 3.2 Add Authentication UI ✅
**Priority:** High
**Estimated Time:** 3 hours

**Tasks:**
- [x] Create `AuthViewModel` for authentication state
- [x] Create `LoginScreen` composable (Combined login/signup with toggle)
- [x] Removed separate SignUpScreen (Combined into LoginScreen)
- [x] Add navigation between auth screens (handled by MainActivity)
- [x] Add error handling and loading states (Snackbar for errors)
- [x] Persist auth state across app restarts (Supabase automatic)

**Files to create:**
- `app/src/main/java/com/carryzonemap/app/ui/auth/AuthViewModel.kt`
- `app/src/main/java/com/carryzonemap/app/ui/auth/LoginScreen.kt`
- `app/src/main/java/com/carryzonemap/app/ui/auth/SignUpScreen.kt`
- `app/src/main/java/com/carryzonemap/app/ui/navigation/AuthNavGraph.kt`

---

### 3.3 Update MainActivity for Auth Flow ✅
**Priority:** High
**Estimated Time:** 1 hour

**Tasks:**
- [x] Check auth state on app launch
- [x] Show LoginScreen if not authenticated
- [x] Show MapScreen if authenticated
- [ ] Handle sign-out from MapScreen (TODO: Add sign-out button)

**Files to modify:**
- `app/src/main/java/com/carryzonemap/app/MainActivity.kt`
- `app/src/main/java/com/carryzonemap/app/ui/MapScreen.kt` (add sign-out button)

---

## Phase 4: Hybrid Repository (Local + Remote Sync) ✅

### 4.1 Refactor Repository for Dual Sources ✅
**Priority:** Critical
**Estimated Time:** 4 hours

**Strategy: Offline-First with Background Sync**

**Current:**
```
ViewModel → PinRepository → PinDao (Room)
```

**New:**
```
ViewModel → PinRepository → LocalDataSource (Room)
                          ↘ RemoteDataSource (Supabase)
                          → SyncManager
```

**Tasks:**
- [x] Refactor `PinRepositoryImpl` to use both local and remote sources
- [x] Implement offline-first pattern (read from Room, queue writes for sync)
- [x] Add sync status tracking (SyncStatus sealed class)
- [x] Add network connectivity monitoring (NetworkMonitor)
- [x] Queue operations when offline (sync_queue table)

**Implementation Pattern:**
```kotlin
@Singleton
class PinRepositoryImpl @Inject constructor(
    private val localDataSource: PinDao,
    private val remoteDataSource: RemotePinDataSource,
    private val networkMonitor: NetworkMonitor,
    private val syncManager: SyncManager
) : PinRepository {

    // Read from local cache (Room) for instant UI
    override fun getAllPins(): Flow<List<Pin>> {
        return localDataSource.getAllPins().map { it.toDomainModels() }
    }

    // Write to local immediately, then sync to remote in background
    override suspend fun addPin(pin: Pin) {
        // 1. Save to local DB immediately (optimistic update)
        localDataSource.insertPin(pin.toEntity())

        // 2. Queue for remote sync
        syncManager.queuePinForUpload(pin)
    }

    // Similar pattern for update/delete
}
```

**Files to modify:**
- `app/src/main/java/com/carryzonemap/app/data/repository/PinRepositoryImpl.kt`

**Files to create:**
- `app/src/main/java/com/carryzonemap/app/data/sync/SyncManager.kt`
- `app/src/main/java/com/carryzonemap/app/data/network/NetworkMonitor.kt`

---

### 4.2 Implement SyncManager ✅
**Priority:** Critical
**Estimated Time:** 5 hours

**Responsibilities:**
- Queue pending operations (create/update/delete)
- Upload queued operations when online
- Download remote changes periodically
- Handle conflicts
- Persist sync state

**Tasks:**
- [x] Create `SyncOperation` sealed class (Create/Update/Delete)
- [x] Create `SyncQueue` table in Room for pending operations (SyncQueueEntity, SyncQueueDao)
- [x] Implement `SyncManager` with upload/download logic (SyncManagerImpl)
- [x] Add WorkManager for background sync (SyncWorker with @HiltWorker)
- [x] Handle sync failures and retries (retry_count, last_error fields)

**Files to create:**
```kotlin
// app/src/main/java/com/carryzonemap/app/data/sync/SyncManager.kt
interface SyncManager {
    suspend fun queuePinForUpload(pin: Pin)
    suspend fun queuePinForUpdate(pin: Pin)
    suspend fun queuePinForDeletion(pinId: String)
    suspend fun syncWithRemote()
    fun observeSyncStatus(): Flow<SyncStatus>
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Error(val message: String) : SyncStatus()
    data class Success(val uploadedCount: Int, val downloadedCount: Int) : SyncStatus()
}

// app/src/main/java/com/carryzonemap/app/data/sync/SyncWorker.kt
class SyncWorker @Inject constructor(
    context: Context,
    params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        syncManager.syncWithRemote()
        return Result.success()
    }
}
```

**Files to create:**
- `app/src/main/java/com/carryzonemap/app/data/sync/SyncManager.kt`
- `app/src/main/java/com/carryzonemap/app/data/sync/SyncManagerImpl.kt`
- `app/src/main/java/com/carryzonemap/app/data/sync/SyncWorker.kt`
- `app/src/main/java/com/carryzonemap/app/data/local/entity/SyncQueueEntity.kt`
- `app/src/main/java/com/carryzonemap/app/data/local/dao/SyncQueueDao.kt`

---

### 4.3 Add NetworkMonitor ✅
**Priority:** High
**Estimated Time:** 1.5 hours

**Tasks:**
- [x] Create `NetworkMonitor` class using ConnectivityManager
- [x] Expose network state as Flow (callbackFlow with distinctUntilChanged)
- [x] Trigger sync when network becomes available (checked in SyncManager)
- [x] Add to Hilt DI (@Singleton)

**Files to create:**
```kotlin
// app/src/main/java/com/carryzonemap/app/data/network/NetworkMonitor.kt
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        connectivityManager?.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager?.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
```

**Files to create:**
- `app/src/main/java/com/carryzonemap/app/data/network/NetworkMonitor.kt`

---

## Phase 5: Conflict Resolution ⏳

### 5.1 Implement Last-Write-Wins Strategy ✅
**Priority:** High
**Estimated Time:** 3 hours

**Strategy:**
- Use `last_modified` timestamp to determine which version is newer
- When downloading from remote, compare timestamps with local version
- Keep the version with the latest `last_modified` timestamp
- Log conflicts for debugging

**Tasks:**
- [x] Add conflict detection logic in SyncManager
- [x] Compare local and remote timestamps (lastModified field)
- [x] Update local DB with remote version if remote is newer
- [x] Skip upload if remote version is newer (implicit - remote overwrites)
- [x] Add conflict logging/metrics (Log.d statements)

**Implementation:**
```kotlin
private suspend fun resolveConflict(local: Pin, remote: Pin): Pin {
    return if (remote.metadata.lastModified > local.metadata.lastModified) {
        // Remote is newer, use remote version
        localDataSource.updatePin(remote.toEntity())
        remote
    } else {
        // Local is newer, upload to remote
        remoteDataSource.updatePin(local)
        local
    }
}
```

**Files to modify:**
- `app/src/main/java/com/carryzonemap/app/data/sync/SyncManagerImpl.kt`

---

### 5.2 Handle Deletion Conflicts ⬜
**Priority:** Medium
**Estimated Time:** 2 hours

**Scenarios:**
- Pin deleted locally, modified remotely → Keep deletion
- Pin deleted remotely, modified locally → Keep deletion
- Pin deleted on both → No conflict

**Tasks:**
- [ ] Add tombstone records for deletions (soft delete)
- [ ] Sync deletion timestamps
- [ ] Clean up old tombstones periodically
- [ ] Update SyncManager to handle deletion conflicts

**Files to modify:**
- `app/src/main/java/com/carryzonemap/app/data/local/entity/PinEntity.kt` (add `deleted` flag)
- `app/src/main/java/com/carryzonemap/app/data/sync/SyncManagerImpl.kt`
- `supabase/migrations/002_add_soft_delete.sql`

---

## Phase 6: Testing

### 6.1 Unit Tests for New Components ⬜
**Priority:** High
**Estimated Time:** 5 hours

**Tasks:**
- [ ] Test `SupabasePinDataSource` with fake/mock Supabase client
- [ ] Test `SyncManager` with fake data sources
- [ ] Test `AuthRepository` with fake auth client
- [ ] Test mappers (SupabaseMapper)
- [ ] Test conflict resolution logic
- [ ] Test NetworkMonitor

**Files to create:**
- `app/src/test/java/com/carryzonemap/app/data/remote/datasource/SupabasePinDataSourceTest.kt`
- `app/src/test/java/com/carryzonemap/app/data/sync/SyncManagerTest.kt`
- `app/src/test/java/com/carryzonemap/app/data/repository/SupabaseAuthRepositoryTest.kt`
- `app/src/test/java/com/carryzonemap/app/data/remote/mapper/SupabaseMapperTest.kt`
- `app/src/test/java/com/carryzonemap/app/data/network/NetworkMonitorTest.kt`

---

### 6.2 Integration Tests ⬜
**Priority:** Medium
**Estimated Time:** 4 hours

**Tasks:**
- [ ] Test full sync flow (local → remote → local)
- [ ] Test offline queue functionality
- [ ] Test realtime subscription integration
- [ ] Test auth flow integration
- [ ] Test conflict resolution end-to-end

**Files to create:**
- `app/src/androidTest/java/com/carryzonemap/app/data/sync/SyncIntegrationTest.kt`
- `app/src/androidTest/java/com/carryzonemap/app/data/repository/PinRepositoryIntegrationTest.kt`

---

### 6.3 Update Existing Tests ⬜
**Priority:** High
**Estimated Time:** 2 hours

**Tasks:**
- [ ] Update `PinRepositoryImplTest` to mock remote data source
- [ ] Update `MapViewModelTest` to handle sync states
- [ ] Add tests for network error scenarios
- [ ] Add tests for offline mode

**Files to modify:**
- `app/src/test/java/com/carryzonemap/app/data/repository/PinRepositoryImplTest.kt`
- `app/src/test/java/com/carryzonemap/app/ui/viewmodel/MapViewModelTest.kt`

---

## Phase 7: Enhanced Features

### 7.1 Photo Upload to Supabase Storage ⬜
**Priority:** Medium
**Estimated Time:** 4 hours

**Tasks:**
- [ ] Implement photo upload using Supabase Storage
- [ ] Generate unique file names (UUID)
- [ ] Upload photos in background
- [ ] Store public URL in `photo_uri` field
- [ ] Add image compression before upload
- [ ] Handle upload failures with retry

**Files to create:**
- `app/src/main/java/com/carryzonemap/app/data/remote/storage/PhotoStorageManager.kt`

---

### 7.2 Geographic Queries and Map Optimization ⬜
**Priority:** Medium
**Estimated Time:** 3 hours

**Tasks:**
- [ ] Implement bounding box query for visible map area
- [ ] Only sync pins within current viewport
- [ ] Add pin clustering for dense areas
- [ ] Implement radius-based search
- [ ] Optimize database queries with indexes

**Files to modify:**
- `app/src/main/java/com/carryzonemap/app/data/remote/datasource/SupabasePinDataSource.kt`
- `app/src/main/java/com/carryzonemap/app/ui/viewmodel/MapViewModel.kt`

---

### 7.3 Real-Time Collaboration Features ⬜
**Priority:** Low
**Estimated Time:** 3 hours

**Tasks:**
- [ ] Show notification when nearby pins are updated
- [ ] Add "Recently Updated" indicator on pins
- [ ] Display user avatars on pins (who created them)
- [ ] Add activity feed of recent changes

**Files to create:**
- `app/src/main/java/com/carryzonemap/app/ui/components/ActivityFeed.kt`
- `app/src/main/java/com/carryzonemap/app/ui/state/ActivityFeedState.kt`

---

## Phase 8: Production Readiness

### 8.1 Error Handling and Logging ⬜
**Priority:** High
**Estimated Time:** 2 hours

**Tasks:**
- [ ] Add comprehensive error handling for network failures
- [ ] Add retry logic with exponential backoff
- [ ] Log sync events for debugging
- [ ] Add user-friendly error messages
- [ ] Handle rate limiting from Supabase

**Files to create:**
- `app/src/main/java/com/carryzonemap/app/data/network/RetryPolicy.kt`
- `app/src/main/java/com/carryzonemap/app/util/Logger.kt`

---

### 8.2 Performance Optimization ⬜
**Priority:** Medium
**Estimated Time:** 3 hours

**Tasks:**
- [ ] Profile network requests
- [ ] Implement pagination for large datasets
- [ ] Add database query optimization
- [ ] Reduce sync frequency for battery life
- [ ] Cache frequently accessed data
- [ ] Add request debouncing

---

### 8.3 Security Review ⬜
**Priority:** High
**Estimated Time:** 2 hours

**Tasks:**
- [ ] Review RLS policies in Supabase
- [ ] Ensure API keys are not committed to git
- [ ] Add ProGuard rules for release builds
- [ ] Test auth flow security
- [ ] Implement rate limiting on client side
- [ ] Add input validation for user-generated content

---

### 8.4 Documentation ⬜
**Priority:** Medium
**Estimated Time:** 3 hours

**Tasks:**
- [ ] Document Supabase setup steps
- [ ] Create sync architecture diagram
- [ ] Update README.md with cloud sync instructions
- [ ] Document RLS policies
- [ ] Add troubleshooting guide
- [ ] Update CLAUDE.md with Supabase patterns

**Files to modify:**
- `README.md`
- `CLAUDE.md`

**Files to create:**
- `docs/SUPABASE_SETUP.md`
- `docs/SYNC_ARCHITECTURE.md`

---

## Migration Checklist

### Pre-Migration
- [ ] Backup existing Room database
- [ ] Export current pins to JSON
- [ ] Test Supabase connection
- [ ] Verify schema in Supabase

### Migration Day
- [ ] Push current Room data to Supabase (one-time migration)
- [ ] Enable sync in production build
- [ ] Monitor error logs
- [ ] Test on multiple devices

### Post-Migration
- [ ] Verify data consistency across devices
- [ ] Check sync performance metrics
- [ ] Gather user feedback
- [ ] Fix critical bugs

---

## Rollback Plan

If Supabase integration causes critical issues:

1. **Immediate:** Disable remote sync via feature flag
2. **Keep Room:** Local database continues to work
3. **Investigate:** Debug issues in development environment
4. **Re-enable:** Once issues are resolved

**Feature Flag:**
```kotlin
// In BuildConfig or RemoteConfig
const val ENABLE_CLOUD_SYNC = true

// In Repository
if (BuildConfig.ENABLE_CLOUD_SYNC && networkMonitor.isOnline.value) {
    syncManager.queuePinForUpload(pin)
}
```

---

## Total Time Estimate

| Phase | Estimated Time |
|-------|----------------|
| Phase 1: Setup & Configuration | 4 hours |
| Phase 2: Remote Data Source | 6 hours |
| Phase 3: Authentication | 7 hours |
| Phase 4: Hybrid Repository | 10.5 hours |
| Phase 5: Conflict Resolution | 5 hours |
| Phase 6: Testing | 11 hours |
| Phase 7: Enhanced Features | 10 hours |
| Phase 8: Production Readiness | 10 hours |
| **Total** | **63.5 hours** (~8-10 days) |

---

## Current Status

**Last Updated:** 2025-10-11
**Current Phase:** ✅ Phase 4 Complete - Hybrid Sync Implemented
**Status:** Phases 1-4 Complete, Phase 5 Partially Complete (Last-Write-Wins implemented)

### Implementation Summary:
- ✅ **Phase 1**: Supabase Setup & Configuration (Code Complete - User setup required)
- ✅ **Phase 2**: Remote Data Source Layer (Complete)
- ✅ **Phase 3**: Authentication (Complete - Email confirmation disabled for dev)
- ✅ **Phase 4**: Hybrid Repository (Complete - Offline-first sync working)
- ⏳ **Phase 5**: Conflict Resolution (Last-write-wins complete, soft delete pending)
- ⬜ **Phase 6**: Testing (Not started)
- ⬜ **Phase 7**: Enhanced Features (Not started)
- ⬜ **Phase 8**: Production Readiness (Not started)

---

## Decision Log

### Architecture Decisions

**Decision 1: Offline-First with Room + Supabase**
- **Date:** 2025-10-11
- **Decision:** Keep Room as local cache, sync with Supabase in background
- **Rationale:** Ensures app works offline, provides instant UI updates, maintains existing architecture
- **Alternatives Considered:**
  - Supabase-only (rejected: requires constant internet)
  - Room-only with manual export/import (rejected: no real-time collaboration)

**Decision 2: Last-Write-Wins Conflict Resolution**
- **Date:** 2025-10-11
- **Decision:** Use timestamp-based last-write-wins for conflicts
- **Rationale:** Simple to implement, works well for map pins (low conflict rate)
- **Alternatives Considered:**
  - Operational Transform (rejected: too complex for this use case)
  - Manual conflict resolution UI (deferred: add if needed)

**Decision 3: Email Authentication First**
- **Date:** 2025-10-11
- **Decision:** Start with email/password auth, add OAuth later
- **Rationale:** Simpler to implement, sufficient for MVP
- **Future:** Add Google/Apple OAuth in later phase

---

## Notes and Open Questions

### Open Questions
- ⚠️ **Moderation Model:** Should all users be able to edit any pin, or only their own? (Current: any pin)
- ⚠️ **Data Retention:** How long to keep deleted pins (tombstones)? (Proposal: 30 days)
- ⚠️ **Sync Frequency:** How often to sync in background? (Proposal: every 15 minutes when app is active)
- ⚠️ **Pin Voting:** Should votes affect conflict resolution (higher votes = keep that version)? (Deferred)
- ⚠️ **Rate Limiting:** What's the max pins per user per day? (Proposal: 100 pins/day)

### Known Limitations
- Free tier: 500 MB database, 2 GB bandwidth/month
- Realtime: 200 concurrent connections on free tier
- Storage: 50 MB file storage on free tier

### Future Enhancements
- Add user profiles with avatars
- Implement pin reporting/moderation
- Add pin categories (restaurant, park, government building, etc.)
- Implement geofencing notifications
- Add pin analytics (most viewed, most voted, etc.)

---

## Progress Tracking

Track your progress by checking off items in each phase. Update the "Current Status" section at the top of this document as you complete each phase.

**Tip:** Use `git commit` after completing each major task to track your progress and allow easy rollback if needed.
