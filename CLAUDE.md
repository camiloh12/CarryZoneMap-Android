# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CarryZoneMap is a modern Android app for mapping carry zones with **cloud synchronization**, built with Clean Architecture + MVVM + Offline-First Sync. The app was refactored from a basic MVP to production-ready architecture in October 2025, and Supabase integration was completed for authentication and real-time cloud sync. Key architectural decisions and completed work are documented in `REFACTORING_PLAN.md`, `REFACTORING_SUMMARY.md`, and `SUPABASE_PROGRESS.md`.

## Build & Development Commands

### Building
```bash
# Clean build
./gradlew clean assembleDebug

# Build release (ProGuard rules configured but minify currently disabled)
./gradlew assembleRelease

# Install debug build on connected device/emulator
./gradlew installDebug
```

### Testing
```bash
# Run all unit tests
./gradlew test

# Run single test class
./gradlew test --tests "com.carryzonemap.app.ui.viewmodel.MapViewModelTest"

# Run single test method
./gradlew test --tests "com.carryzonemap.app.ui.viewmodel.MapViewModelTest.adding pin updates state correctly"

# Run instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run all checks (tests + lint)
./gradlew check
```

### Code Quality
```bash
# Future: Detekt and KtLint will be added in Phase 4
# For now, rely on Android Studio's built-in inspections
```

## Architecture

### Clean Architecture Layers

The codebase follows **Clean Architecture with MVVM + Offline-First Sync**, strictly separating concerns across three layers:

```
Presentation (ui/) → Domain (domain/) → Data (data/)
     ↓                    ↓                  ↓
  Compose UI      Business Logic      Hybrid Sync
  ViewModel         Models            ┌──────────────────┐
  StateFlow        Repository IF      │ PinRepositoryImpl│
                                      └────────┬─────────┘
                                               │
                       ┌───────────────────────┼───────────────────────┐
                       ↓                       ↓                       ↓
                 Local (Room)          SyncManager              Remote (Supabase)
                 • PinDao              • Queue ops             • Auth
                 • SyncQueueDao        • Upload/Download       • Postgrest
                 • Instant reads       • Conflict res.         • Realtime
                                       • NetworkMonitor
```

**Key Principles**:
- Dependencies only flow inward. Domain layer has ZERO Android dependencies.
- Offline-first: All writes go to Room immediately, then sync to Supabase when online.
- Queue-based sync: Operations queued and retried automatically on failure.

### Layer Responsibilities

**Domain Layer** (`domain/`):
- **Pure Kotlin** business logic - no Android framework imports
- `model/`: Core entities (Pin, User, Location, PinStatus, PinMetadata, Poi)
  - `Pin`: Now includes `name` field for POI (Point of Interest) association
  - `Poi`: Represents points of interest from OpenStreetMap
- `repository/`: Repository interfaces (PinRepository, AuthRepository)
- `mapper/`: Domain ↔ MapLibre Feature conversions

**Data Layer** (`data/`):
- Implements domain repository interfaces with hybrid sync
- `local/`: Room database (entities, DAOs, database class, migrations)
  - `PinEntity`: Local pin storage
  - `SyncQueueEntity`: Queue for pending operations
- `remote/`: Supabase integration (DTOs, mappers, data sources)
  - `SupabasePinDto`: Data transfer object for Supabase API
  - `SupabaseMapper`: DTO ↔ Domain conversions
  - `SupabasePinDataSource`: Remote CRUD + real-time subscriptions
  - `OverpassDataSource`: Fetches POIs from OpenStreetMap Overpass API with caching
- `sync/`: Offline-first sync infrastructure
  - `SyncManager`: Orchestrates upload/download/conflict resolution
  - `SyncWorker`: Background periodic sync with WorkManager
  - `SyncOperation`: Sealed class for Create/Update/Delete operations
  - `SyncStatus`: Sealed class for sync state tracking
- `network/`: Network monitoring (NetworkMonitor with reactive Flow)
- `repository/`: Repository implementations
  - `PinRepositoryImpl`: Offline-first with SyncManager integration
  - `SupabaseAuthRepository`: Email/password authentication
- `mapper/`: Entity ↔ Domain conversions

**Presentation Layer** (`ui/`):
- Jetpack Compose UI + ViewModels
- `MapViewModel`: Owns `MapUiState` (StateFlow), coordinates repository calls
- `AuthViewModel`: Manages authentication state and operations
- `MapScreen`: Pure UI rendering, collects StateFlow, no business logic
- `auth/LoginScreen`: Email/password login and signup UI
- `components/PinDialog`: Reusable dialog for pin creation/editing
- `state/PinDialogState`: Sealed class managing dialog states (Hidden/Creating/Editing)
- `map/`: Map-specific helper classes (refactored Oct 2025 for SOLID/DRY principles)
  - `MapConstants`: Centralized constants (zoom levels, colors, property names, URLs)
  - `FeatureClickHandler`: Chain of Responsibility pattern for polymorphic click handling
  - `ExistingPinDetector`, `OverpassPoiDetector`, `MapTilerPoiDetector`: Detector implementations
  - `CameraController`: Camera positioning logic (Single Responsibility)
  - `MapLayerManager`: POI layer management (Single Responsibility)
  - `LocationComponentManager`: Location component setup (Single Responsibility)
- ViewModels injected via Hilt's `@HiltViewModel`

**Dependency Injection** (`di/`):
- Hilt modules provide dependencies
- `DatabaseModule`: Room database, DAOs, and migrations
- `RepositoryModule`: Binds repository interfaces to implementations
- `SupabaseModule`: Supabase client and service providers
- `SyncModule`: SyncManager binding
- `LocationModule`: Location services (FusedLocationProviderClient)

**Legacy** (`map/`):
- Old map rendering code being phased out
- `FeatureLayerManager` still used for MapLibre layer rendering (to be migrated to ui/map/)
- `FeatureDataStore` still used for legacy feature persistence
- `PersistedFeature` helper class for FeatureDataStore
- **Removed (Oct 2025)**: MapFacade, MapSetupOrchestrator, MapInteractionHandler (replaced by ui/map/ helpers)
- Can be fully deprecated once migration to domain models is complete

### Data Flow Pattern

**Creating a Pin (POI-based dialog flow):**
1. User clicks on POI label (Overpass or MapTiler) → `MapScreen` queries features at click point
2. `MapScreen` extracts POI name and calls `viewModel.showCreatePinDialog(name, lng, lat)`
3. `MapViewModel` updates `MapUiState` with `PinDialogState.Creating(name, location, selectedStatus)`
4. `PinDialog` renders with POI name header and status picker (green/yellow/red options)
5. User selects status → `viewModel.onDialogStatusSelected(status)`
6. User taps "Create" → `viewModel.confirmPinDialog()`
7. `MapViewModel` creates domain `Pin` model with POI name and selected status
8. `MapViewModel` calls `pinRepository.addPin(pin)` (suspend function)
9. `PinRepositoryImpl` maps Pin → PinEntity via `EntityMapper`
10. Repository calls `pinDao.insertPin(entity)` (Room)
11. Room emits new data via `Flow<List<PinEntity>>`
12. Repository maps entities → domain models via `EntityMapper`
13. `MapViewModel` collects Flow, updates `MapUiState` with new pins
14. `MapScreen` collects StateFlow, recomposes with new pins
15. `MapScreen` maps domain Pins → MapLibre Features via `PinMapper`
16. `FeatureLayerManager` updates map layer

**POI Fetching and Caching:**
1. Camera movement → `MapScreen.fetchPoisForCurrentViewport()` called
2. `MapViewModel.fetchPoisInViewport(bounds)` → `OverpassDataSource.fetchPoisInBounds()`
3. `OverpassDataSource` checks cache (key: rounded viewport bounds)
4. If fresh cache (< 30 min) → return cached POIs immediately
5. If stale/no cache → fetch from Overpass API
6. On API success → update cache, return fresh POIs
7. On API failure/throttle → return cached POIs (even if expired)
8. POIs stored in `MapUiState.pois` → rendered as labels on map
9. Cache cleanup: keeps 20 most recent viewports to prevent memory bloat

**Editing a Pin (dialog-based flow):**
1. User taps pin → `MapScreen` calls `viewModel.showEditPinDialog(pinId)`
2. `MapViewModel` updates `MapUiState` with `PinDialogState.Editing(pin, currentStatus)`
3. `PinDialog` renders with current status selected and "Delete" button
4. User changes status → `viewModel.onDialogStatusSelected(newStatus)`
5. User taps "Save" → `viewModel.confirmPinDialog()`
6. `MapViewModel` calls `pinRepository.updatePin(updatedPin)` with new status
7. OR user taps "Delete" → `viewModel.deletePinFromDialog()` → `pinRepository.deletePin(pin)`
8. Database updates → Flow emission → UI recomposition (same as steps 10-15 above)

**Key Insights**:
- **POI-based creation**: Pins can only be created by clicking on POI labels (prevents arbitrary pin placement)
- **Dialog-based flow**: Provides explicit user control over pin states
- **POI caching**: Handles Overpass API throttling gracefully, POI labels persist for 30 minutes
- Data flows: POI Click → ViewModel → Dialog State → User Selection → Repository → Database → Flow → ViewModel → StateFlow → UI

**Offline-First Sync Flow (Hybrid Architecture):**
1. User creates/updates/deletes pin → `ViewModel` calls `PinRepository` operation
2. `PinRepositoryImpl` **immediately** writes to Room (instant UI update, works offline)
3. `PinRepositoryImpl` queues operation → `SyncManager.queuePinForUpload/Update/Deletion(pin)`
4. `SyncManager` inserts `SyncQueueEntity` into `sync_queue` table
5. Room Flow emits → UI updates instantly (user sees change immediately)
6. `NetworkMonitor` detects online status → triggers sync
7. `SyncManager.syncWithRemote()` called (by network callback or `SyncWorker`)
8. **Upload phase**: `SyncManager` reads pending operations from `sync_queue`
9. For each operation: calls `RemotePinDataSource` (Supabase) to upload
10. On success: removes operation from `sync_queue`
11. On failure: increments retry count (max 3 retries), logs error
12. **Download phase**: Fetches all pins from Supabase
13. **Conflict resolution**: Last-write-wins using `lastModified` timestamps
14. Newer remote pins → update Room → Flow emits → UI updates
15. Background sync continues via `SyncWorker` (periodic WorkManager task)

**Key Sync Principles**:
- **Instant responsiveness**: Local writes complete in milliseconds, UI never blocks
- **Works offline**: All CRUD operations function without network
- **Automatic retry**: Failed operations retry up to 3 times with exponential backoff
- **Conflict resolution**: Last-write-wins (newest `lastModified` timestamp wins)
- **Queue-based**: Operations survive app restarts (persisted in Room)
- **Network-aware**: Only syncs when online, conserves battery/data

### State Management

- **Single source of truth**: `MapUiState` in `MapViewModel`
- **Reactive updates**: Room Flow → Repository → ViewModel StateFlow → Compose
- **Immutability**: All state updates via `.copy()`, never mutate
- **Error handling**: Errors stored in `MapUiState.error`, displayed via Snackbar
- **Dialog state**: `PinDialogState` (sealed class) tracks creation/editing dialog
  - `Hidden`: No dialog shown
  - `Creating(name, location, selectedStatus)`: Creating new pin for a POI
  - `Editing(pin, selectedStatus)`: Editing existing pin
- **POI state**: POIs fetched from Overpass API, cached for 30 minutes, displayed as map labels

### Mappers

Three mapper objects handle conversions between layers:

1. **PinMapper** (domain/mapper): Domain Pin ↔ MapLibre Feature
   - Includes `name` property for POI association
2. **EntityMapper** (data/mapper): Domain Pin ↔ Room PinEntity
   - Maps `name` field bidirectionally
3. **SupabaseMapper** (data/remote/mapper): Domain Pin ↔ SupabasePinDto
   - Maps `name` field for cloud sync

These are object singletons with extension functions for clean syntax.

When adding new fields to Pin:
1. Add to domain `Pin` model (domain/model/Pin.kt)
2. Add to `PinEntity` (data/local/entity/PinEntity.kt)
3. Add to `SupabasePinDto` (data/remote/dto/SupabasePinDto.kt)
4. Update all three mappers (PinMapper, EntityMapper, SupabaseMapper)
5. Create Room migration if schema changes (DatabaseModule.kt)
6. Create Supabase migration SQL (supabase/migrations/)
7. Update all affected tests to include the new field

## Configuration

### Java Version
Project requires **Java 21**. Gradle automatically uses Java from:
1. `JAVA_HOME` environment variable (if set)
2. The Java version that launched Gradle
3. Android Studio/IntelliJ's configured JDK

**Local Development**: Ensure Android Studio is configured to use Java 21 (File → Project Structure → SDK Location → Gradle JDK)

**CI/CD**: GitHub Actions workflow automatically sets up Java 21 (configured in `.github/workflows/ci.yml`)

**Manual Override** (optional): If needed, you can set a specific Java path in `gradle.properties`:
```properties
# Uncomment and adjust path if you need to override Java version
# org.gradle.java.home=/usr/lib/jvm/java-1.21.0-openjdk-amd64
```

**Note**: Hardcoding Java paths in `gradle.properties` is not recommended as it breaks portability across different machines and CI environments.

### API Keys
Configuration in `local.properties`:
```properties
# MapTiler (optional - demo tiles work without it)
MAPTILER_API_KEY=your_key_here

# Supabase (required for authentication and sync)
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your_supabase_anon_key_here
```

**MapTiler**: Accessed via `BuildConfig.MAPTILER_API_KEY` in MapScreen
**Supabase**: Get from Supabase dashboard → Settings → API. See `SUPABASE_PROGRESS.md` for setup instructions.

### Room Database
- Database name: `carry_zone_db`
- **Current version: 4** (added POI name support)
- Migrations:
  - **v1 → v2**: Added `sync_queue` table with index on `pin_id`, added `createdBy` field to pins
  - **v2 → v3**: No-op migration to force schema consistency for incomplete v2 databases
  - **v3 → v4**: Added `name` column to pins table for POI names
  - Migrations defined in `DatabaseModule.kt`
- `exportSchema = false` (should be enabled with proper directory in production)
- Tables:
  - `pins`: Pin data with POI name, metadata, and timestamps
  - `sync_queue`: Pending sync operations (Create/Update/Delete)

## Adding New Features

### Pattern: Adding a New Domain Entity

1. Create domain model in `domain/model/` (pure Kotlin)
2. Create Room entity in `data/local/entity/`
3. Create DAO interface in `data/local/dao/`
4. Add entity to `CarryZoneDatabase` @Database annotation
5. Create mapper in `data/mapper/` (entity ↔ domain)
6. Create repository interface in `domain/repository/`
7. Implement repository in `data/repository/`
8. Bind repository in `RepositoryModule`
9. Inject repository into ViewModel
10. Update ViewModel state and operations
11. Update UI to collect and display

### Pattern: Adding a ViewModel Operation

```kotlin
// In MapViewModel
fun newOperation(param: String) {
    viewModelScope.launch {
        try {
            // Call repository
            val result = repository.doSomething(param)
            // Update state
            _uiState.update { it.copy(someField = result) }
        } catch (e: Exception) {
            // Handle error
            _uiState.update { it.copy(error = "Failed: ${e.message}") }
        }
    }
}
```

Always use `viewModelScope.launch` for async operations and update state immutably.

### Pattern: Dialog-Based User Input

When adding features that require user confirmation or selection:

```kotlin
// 1. Define dialog state (sealed class in ui/state/)
sealed class MyDialogState {
    data object Hidden : MyDialogState()
    data class Shown(val data: String, val selection: Option) : MyDialogState()
}

// 2. Add to MapUiState
data class MapUiState(
    // ... existing fields
    val myDialogState: MyDialogState = MyDialogState.Hidden
)

// 3. Add ViewModel methods
fun showMyDialog(data: String) {
    _uiState.update { it.copy(myDialogState = MyDialogState.Shown(data, defaultOption)) }
}

fun onDialogOptionSelected(option: Option) {
    val current = _uiState.value.myDialogState
    if (current is MyDialogState.Shown) {
        _uiState.update { it.copy(myDialogState = current.copy(selection = option)) }
    }
}

fun confirmMyDialog() {
    val dialogState = _uiState.value.myDialogState
    if (dialogState is MyDialogState.Shown) {
        viewModelScope.launch {
            // Perform action with dialogState.data and dialogState.selection
            dismissMyDialog()
        }
    }
}

fun dismissMyDialog() {
    _uiState.update { it.copy(myDialogState = MyDialogState.Hidden) }
}

// 4. Create dialog composable in ui/components/
@Composable
fun MyDialog(
    dialogState: MyDialogState,
    onOptionSelected: (Option) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) { /* Implementation */ }
```

### Pattern: Polymorphic Click Handling (Chain of Responsibility)

The MapScreen click handling was refactored (Oct 2025) to use the Chain of Responsibility pattern instead of nested if-else statements. This makes the code more maintainable and extensible.

**Example: Adding a new click detector**

```kotlin
// 1. Create a new detector class in ui/map/
class CustomPoiDetector(private val viewModel: MapViewModel) : FeatureDetector {
    override fun canHandle(map: MapLibreMap, screenPoint: PointF, clickPoint: LatLng): Boolean {
        // Check if this detector should handle the click
        val features = map.queryRenderedFeatures(screenPoint, "custom-layer-id")
        return features.isNotEmpty()
    }

    override fun handle(map: MapLibreMap, screenPoint: PointF, clickPoint: LatLng) {
        // Handle the click
        val features = map.queryRenderedFeatures(screenPoint, "custom-layer-id")
        val customFeature = features.firstOrNull() ?: return

        val customData = customFeature.getStringProperty("custom_property")
        viewModel.handleCustomFeature(customData, clickPoint.longitude, clickPoint.latitude)
        Timber.d("User clicked custom POI: $customData")
    }
}

// 2. Register the detector in FeatureClickHandler constructor
class FeatureClickHandler(private val viewModel: MapViewModel) {
    private val detectors: List<FeatureDetector> = listOf(
        ExistingPinDetector(viewModel),
        OverpassPoiDetector(viewModel),
        MapTilerPoiDetector(viewModel),
        CustomPoiDetector(viewModel)  // Add new detector
    )
    // ... rest of the class
}
```

**Benefits**:
- **Open/Closed Principle**: Add new detectors without modifying existing code
- **Testable**: Each detector can be unit tested independently
- **Readable**: Clear separation of concerns, no nested if-else
- **Maintainable**: Easy to add/remove/reorder detectors

### Pattern: Centralized Constants

All magic numbers and strings are centralized in `MapConstants.kt` (Oct 2025 refactor).

**Example: Adding new constants**

```kotlin
// In ui/map/MapConstants.kt
object MapConstants {
    // ... existing constants

    // Add new feature-specific constants
    const val NEW_FEATURE_ZOOM_LEVEL = 15.0
    const val NEW_FEATURE_COLOR = "#FF5722"
    const val NEW_FEATURE_LAYER_ID = "new-feature-layer"

    object NewFeatureText {
        const val TITLE = "New Feature"
        const val DESCRIPTION = "Description of new feature"
    }
}

// Usage in code
val zoomLevel = MapConstants.NEW_FEATURE_ZOOM_LEVEL
val title = MapConstants.NewFeatureText.TITLE
```

**Benefits**:
- **DRY**: Define once, use everywhere
- **Maintainability**: Change values in one place
- **Discoverability**: All constants in one file
- **Type safety**: Compile-time checking

### Pattern: Single Responsibility Helper Classes

MapScreen was refactored to delegate specific responsibilities to focused helper classes (Oct 2025).

**Example: Adding a new map helper**

```kotlin
// 1. Create helper class in ui/map/ with single responsibility
class MapAnimationController(private val map: MapLibreMap) {

    fun animateToLocation(location: Location, duration: Long = 1000) {
        // Single responsibility: handle map animations
        val animation = CameraUpdateFactory.newLatLngZoom(
            LatLng(location.latitude, location.longitude),
            MapConstants.ZOOM_LEVEL_USER_LOCATION
        )
        map.animateCamera(animation, duration.toInt())
        Timber.d("Animating to location: ${location.latitude}, ${location.longitude}")
    }

    fun animateZoom(zoomLevel: Double, duration: Long = 500) {
        val animation = CameraUpdateFactory.zoomTo(zoomLevel)
        map.animateCamera(animation, duration.toInt())
        Timber.d("Animating zoom to: $zoomLevel")
    }
}

// 2. Use in MapScreen
val animationController = remember { MapAnimationController(map) }
animationController.animateToLocation(currentLocation)
```

**Benefits**:
- **Single Responsibility**: Each class does one thing well
- **Testability**: Easy to test in isolation
- **Reusability**: Can be used in other screens/features
- **Maintainability**: Changes are localized

### Pattern: Offline-First Sync Operations

When adding features that need cloud synchronization:

```kotlin
// In PinRepositoryImpl (example pattern)
override suspend fun addPin(pin: Pin) {
    // STEP 1: Write to local database FIRST (instant UI response)
    pinDao.insertPin(pin.toEntity())
    Log.d("PinRepository", "Pin saved locally: ${pin.id}")

    // STEP 2: Queue for remote sync (async, non-blocking)
    syncManager.queuePinForUpload(pin)
    Log.d("PinRepository", "Pin queued for sync: ${pin.id}")

    // Room Flow automatically emits → ViewModel → UI updates instantly
}

// In SyncManagerImpl (handles background sync)
override suspend fun syncWithRemote(): Result<Unit> {
    try {
        // Check network status
        if (!networkMonitor.isOnline.first()) {
            return Result.failure(Exception("No network connection"))
        }

        // UPLOAD: Send pending operations to Supabase
        val pendingOps = syncQueueDao.getAllOperations()
        pendingOps.forEach { queueItem ->
            try {
                when (queueItem.operationType) {
                    OperationType.CREATE -> {
                        val pin = pinDao.getPinById(queueItem.pinId)
                        remotePinDataSource.insertPin(pin.toDomain())
                    }
                    OperationType.UPDATE -> { /* ... */ }
                    OperationType.DELETE -> { /* ... */ }
                }
                // Success: remove from queue
                syncQueueDao.deleteOperation(queueItem.id)
            } catch (e: Exception) {
                // Failure: increment retry count
                if (queueItem.retryCount >= MAX_RETRIES) {
                    Log.e("SyncManager", "Max retries exceeded for ${queueItem.id}")
                } else {
                    syncQueueDao.updateOperation(
                        queueItem.copy(
                            retryCount = queueItem.retryCount + 1,
                            lastError = e.message
                        )
                    )
                }
            }
        }

        // DOWNLOAD: Fetch remote changes
        val remotePins = remotePinDataSource.getAllPins().getOrThrow()

        // CONFLICT RESOLUTION: Last-write-wins
        remotePins.forEach { remotePin ->
            val localPin = pinDao.getPinById(remotePin.id)
            if (localPin == null || remotePin.lastModified > localPin.lastModified) {
                // Remote is newer, update local
                pinDao.updatePin(remotePin.toEntity())
            }
        }

        return Result.success(Unit)
    } catch (e: Exception) {
        Log.e("SyncManager", "Sync failed", e)
        return Result.failure(e)
    }
}
```

**Key Principles for Sync Features**:
1. **Always write local first**: UI must remain responsive, never wait for network
2. **Queue operations**: Use `SyncQueueEntity` to persist pending operations
3. **Idempotent operations**: Remote operations should be safe to retry
4. **Conflict resolution**: Implement last-write-wins or custom logic
5. **Retry logic**: Max 3 retries with exponential backoff
6. **Error handling**: Log errors, update sync status, don't crash
7. **Network awareness**: Check `NetworkMonitor` before attempting sync

## Testing Strategy

### Unit Tests (Comprehensive Coverage Achieved)
- **ViewModels**: Test with fake repositories, verify state updates (✅ MapViewModelTest - 14 tests)
- **Repositories**: Test with fake DAOs and SyncManager (✅ PinRepositoryImplTest - 12 tests with Robolectric)
- **Domain models**: Test business logic (✅ LocationTest, PinTest, PinStatusTest - 27 tests total)
- **Mappers**: Test bidirectional conversions (✅ EntityMapperTest, PinMapperTest, SupabaseMapperTest - 38 tests total)
- **Legacy components**: FeatureDataStore, PersistedFeature (✅ 7 tests)
- **Total**: **98 tests with 100% success rate** ✅

### Key Testing Features
- **Robolectric Integration**: PinRepositoryImplTest uses Robolectric to support Timber logging in tests
- **Timber Logging**: Production code uses Timber for logging, properly initialized in tests
- **Fake Implementations**: FakePinDao, FakeSyncManager, FakeAuthRepository for isolated testing
- **Flow Testing**: Turbine library for testing reactive Flow emissions
- **Fast Execution**: All 98 tests complete in under 20 seconds

### Dependencies Available
- `kotlinx-coroutines-test` for `runTest`
- `turbine` for testing Flows
- `androidx.arch.core:core-testing` for LiveData/ViewModel testing
- `room-testing` for in-memory database tests
- `hilt-android-testing` for DI tests
- `robolectric` for Android framework support in unit tests
- `timber` for logging (with proper test initialization)

### Test File Locations
- Unit tests: `app/src/test/java/com/carryzonemap/app/`
- Integration tests: `app/src/androidTest/java/com/carryzonemap/app/`

All tests located in `app/src/test/java/com/carryzonemap/app/` organized by layer (domain, data, ui).
Legacy tests: `FeatureDataStoreTest.kt`, `PersistedFeatureTest.kt` still present in `map/` package.

## Common Gotchas

### MapLibre Feature vs Domain Pin
- MapScreen interacts with **MapLibre Features** for rendering
- ViewModel/Repository work with **domain Pins**
- Use `PinMapper.toFeatures()` to convert List<Pin> → List<Feature>
- Never expose MapLibre types to ViewModel or Repository

### StateFlow Collection in Compose
Always collect StateFlow as State in Compose:
```kotlin
val uiState by viewModel.uiState.collectAsState()
// NOT: viewModel.uiState.collect { ... } - this breaks composition
```

### Room Migrations
Currently using `fallbackToDestructiveMigration()` - this **deletes all data** on schema changes. Before production:
1. Remove `fallbackToDestructiveMigration()`
2. Provide proper `Migration` objects
3. Enable `exportSchema = true` with schema location
4. Test migrations thoroughly

### Hilt Injection
- Application class: `@HiltAndroidApp` (CarryZoneApplication)
- Activities: `@AndroidEntryPoint` (MainActivity)
- ViewModels: `@HiltViewModel` with `@Inject constructor`
- Compose: `hiltViewModel()` to get ViewModel instance

### Coroutine Scopes
- Use `viewModelScope` in ViewModels (auto-cancelled on clear)
- Use `LaunchedEffect` in Composables for side effects
- Never use `GlobalScope` or unstructured concurrency

## Future Work Priorities

See `REFACTORING_PLAN.md` and `SUPABASE_PROGRESS.md` for detailed phases.

### Completed ✅
- **Phase 1**: Clean Architecture refactoring with domain/data/presentation layers
- **Phase 2**: MVVM + StateFlow + Repository pattern
- **Phase 3-4**: Comprehensive test suite (**98 tests, 100% pass rate**) + Logging integration
  - SupabaseMapperTest added (13 tests)
  - Timber logging library integrated across codebase
  - Robolectric support for PinRepositoryImplTest
  - All android.util.Log calls replaced with Timber
- **Phase 5**: Supabase integration with offline-first sync
  - Authentication (email/password with session persistence)
  - Remote data source with Supabase PostgreSQL + PostGIS
  - Offline-first sync with SyncManager
  - Background sync with WorkManager
  - Queue-based operations with retry logic (max 3 retries)
  - Network monitoring and last-write-wins conflict resolution
- **MapScreen Refactoring** (Oct 2025): Applied SOLID and DRY principles
  - Extracted MapConstants for all magic numbers and strings
  - Implemented Chain of Responsibility pattern for click handling
  - Created Single Responsibility helper classes (Camera, Layer, Location)
  - Reduced MapScreen from 400 to 330 lines with better readability
  - Improved testability and extensibility

### MVP Complete - Ready for Production! 🎉
The app now has a fully functional offline-first architecture with:
- ✅ Complete authentication system
- ✅ Hybrid local + remote storage
- ✅ Automatic background sync
- ✅ Comprehensive test coverage
- ✅ Production-ready logging

### Next Priorities
1. **Enable Real-time Subscriptions**: Infrastructure ready, needs activation in SyncManager
2. **Test Multi-Device Sync**: Verify sync works across multiple devices
3. **Production Deployment**: Configure release build, ProGuard, signing
4. **Enhanced Features**: Search, filtering by status, radius queries
5. **Pin Details**: Notes, photos, face/license plate blurring (OpenCV)
6. **Social Features**: Voting, comments, moderation
7. **Deprecate `map/` package**: Fully migrate to domain models

### Working with Supabase
The app now uses Supabase for:
- **Authentication**: Email/password via `SupabaseAuthRepository`
- **Database**: PostgreSQL with PostGIS for geographic queries
- **Real-time**: Subscription infrastructure (ready to enable)
- **Sync**: Offline-first queue-based synchronization

When extending Supabase features:
- DTOs go in `data/remote/dto/`
- Mappers go in `data/remote/mapper/`
- Data sources go in `data/remote/datasource/`
- Always maintain offline-first: write to Room, then queue for sync
- See `SUPABASE_INTEGRATION_PLAN.md` for architecture decisions

## Files to Reference

### Architecture & Planning
- `REFACTORING_PLAN.md`: Phase-by-phase refactoring details
- `REFACTORING_SUMMARY.md`: What changed, why, and benefits
- `SUPABASE_INTEGRATION_PLAN.md`: Complete Supabase integration roadmap (Phases 1-5)
- `SUPABASE_PROGRESS.md`: Implementation progress, setup guide, and troubleshooting
- `README.md`: User-facing documentation and roadmap

### Code Examples
- `domain/model/Pin.kt`: Core domain model example
- `domain/model/User.kt`: User model for authentication
- `ui/viewmodel/MapViewModel.kt`: ViewModel pattern with dialog state management
- `ui/auth/AuthViewModel.kt`: Authentication ViewModel example
- `ui/state/PinDialogState.kt`: Sealed class for dialog state (example pattern)
- `ui/components/PinDialog.kt`: Reusable dialog component example
- `ui/map/MapConstants.kt`: **Centralized constants (DRY principle)**
- `ui/map/FeatureClickHandler.kt`: **Chain of Responsibility pattern for click handling**
- `ui/map/CameraController.kt`: Single Responsibility camera controller
- `ui/map/MapLayerManager.kt`: Single Responsibility layer manager
- `ui/map/LocationComponentManager.kt`: Single Responsibility location setup
- `data/repository/PinRepositoryImpl.kt`: **Offline-first repository pattern with sync**
- `data/repository/SupabaseAuthRepository.kt`: Authentication repository example
- `data/sync/SyncManagerImpl.kt`: **Sync orchestration with conflict resolution**
- `data/sync/SyncWorker.kt`: Background sync worker example
- `data/remote/datasource/SupabasePinDataSource.kt`: Remote data source implementation
- `data/network/NetworkMonitor.kt`: Network connectivity monitoring

### Testing
- `app/src/test/`: Comprehensive test suite with 98 tests (100% pass rate)
- `app/src/test/java/com/carryzonemap/app/data/repository/PinRepositoryImplTest.kt`: Repository testing with Robolectric
- `app/src/test/java/com/carryzonemap/app/data/remote/mapper/SupabaseMapperTest.kt`: Supabase DTO mapper testing
- `app/src/test/java/com/carryzonemap/app/ui/viewmodel/MapViewModelTest.kt`: ViewModel testing example

## Code Style

- Follow Kotlin conventions (already established in codebase)
- Use KDoc for public APIs (examples in existing code)
- Prefer immutable data classes with `copy()`
- Extension functions for mapper logic
- Sealed classes for complex state (not yet used but recommended for errors)
