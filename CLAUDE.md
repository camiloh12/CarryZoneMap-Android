# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CarryZoneMap is a modern Android app for mapping carry zones, built with Clean Architecture + MVVM. The app was refactored from a basic MVP to production-ready architecture in October 2025. Key architectural decisions and completed work are documented in `REFACTORING_PLAN.md` and `REFACTORING_SUMMARY.md`.

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

The codebase follows **Clean Architecture with MVVM**, strictly separating concerns across three layers:

```
Presentation (ui/) → Domain (domain/) → Data (data/)
     ↓                    ↓                  ↓
  Compose UI      Business Logic      Room Database
  ViewModel         Models            Repository Impl
  StateFlow        Repository IF       DAOs, Entities
```

**Key Principle**: Dependencies only flow inward. Domain layer has ZERO Android dependencies.

### Layer Responsibilities

**Domain Layer** (`domain/`):
- **Pure Kotlin** business logic - no Android framework imports
- `model/`: Core entities (Pin, Location, PinStatus, PinMetadata)
- `repository/`: Repository interfaces (contracts)
- `mapper/`: Domain ↔ MapLibre Feature conversions

**Data Layer** (`data/`):
- Implements domain repository interfaces
- `local/`: Room database (entities, DAOs, database class)
- `repository/`: Repository implementations
- `mapper/`: Entity ↔ Domain conversions
- Future: `remote/` will be added for cloud sync (Firestore/Supabase)

**Presentation Layer** (`ui/`):
- Jetpack Compose UI + ViewModels
- `MapViewModel`: Owns `MapUiState` (StateFlow), coordinates repository calls
- `MapScreen`: Pure UI rendering, collects StateFlow, no business logic
- ViewModels injected via Hilt's `@HiltViewModel`

**Dependency Injection** (`di/`):
- Hilt modules provide dependencies
- `DatabaseModule`: Room database and DAOs
- `RepositoryModule`: Binds repository interfaces to implementations
- `LocationModule`: Location services (FusedLocationProviderClient)

**Legacy** (`map/`):
- Old map rendering code being phased out
- `FeatureLayerManager` still used for MapLibre layer rendering
- Other classes (`MapFacade`, `FeatureDataStore`, etc.) can be deprecated once refactor completes

### Data Flow Pattern

**Adding a Pin (typical flow):**
1. User long-presses → `MapScreen` calls `viewModel.addPin(lng, lat)`
2. `MapViewModel` creates domain `Pin` model
3. `MapViewModel` calls `pinRepository.addPin(pin)` (suspend function)
4. `PinRepositoryImpl` maps Pin → PinEntity via `EntityMapper`
5. Repository calls `pinDao.insertPin(entity)` (Room)
6. Room emits new data via `Flow<List<PinEntity>>`
7. Repository maps entities → domain models via `EntityMapper`
8. `MapViewModel` collects Flow, updates `MapUiState`
9. `MapScreen` collects StateFlow, recomposes with new pins
10. `MapScreen` maps domain Pins → MapLibre Features via `PinMapper`
11. `FeatureLayerManager` updates map layer

**Key Insight**: Data flows in a circle - UI → ViewModel → Repository → Database → Flow → Repository → ViewModel → StateFlow → UI

### State Management

- **Single source of truth**: `MapUiState` in `MapViewModel`
- **Reactive updates**: Room Flow → Repository → ViewModel StateFlow → Compose
- **Immutability**: All state updates via `.copy()`, never mutate
- **Error handling**: Errors stored in `MapUiState.error`, displayed via Snackbar

### Mappers

Three mapper objects handle conversions between layers:

1. **PinMapper** (domain/mapper): Domain Pin ↔ MapLibre Feature
2. **EntityMapper** (data/mapper): Domain Pin ↔ Room PinEntity
3. These are object singletons with extension functions for clean syntax

When adding new fields to Pin:
1. Add to domain `Pin` model
2. Add to `PinEntity` (database)
3. Update both mappers
4. Create Room migration if schema changes

## Configuration

### Java Version
Project requires **Java 21** (configured in `gradle.properties`):
```properties
org.gradle.java.home=/usr/lib/jvm/java-1.21.0-openjdk-amd64
```
Adjust path if Java 21 is installed elsewhere.

### API Keys
MapTiler API key in `local.properties` (optional - demo tiles work without it):
```properties
MAPTILER_API_KEY=your_key_here
```
Accessed via `BuildConfig.MAPTILER_API_KEY` in MapScreen.

### Room Database
- Database name: `carry_zone_db`
- Version: 1 (initial schema)
- `exportSchema = false` (should be enabled with proper directory in production)
- Migration strategy: Currently `fallbackToDestructiveMigration()` (dev only)

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

## Testing Strategy

### Unit Tests (Phase 3 - not yet implemented)
- **ViewModels**: Test with fake repositories, verify state updates
- **Repositories**: Test with fake DAOs or in-memory Room database
- **Domain models**: Test business logic (e.g., PinStatus.next())
- **Mappers**: Test bidirectional conversions

### Dependencies Available
- `kotlinx-coroutines-test` for `runTest`
- `turbine` for testing Flows
- `androidx.arch.core:core-testing` for LiveData/ViewModel testing
- `room-testing` for in-memory database tests
- `hilt-android-testing` for DI tests

### Test File Locations
- Unit tests: `app/src/test/java/com/carryzonemap/app/`
- Integration tests: `app/src/androidTest/java/com/carryzonemap/app/`

Existing tests: `FeatureDataStoreTest.kt`, `PersistedFeatureTest.kt` (legacy, can be replaced)

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

See `REFACTORING_PLAN.md` for detailed phases. Key priorities:

1. **Phase 3 (Next)**: Write comprehensive tests
2. **Phase 4**: Enhanced features (search, filtering, pin details)
3. **Phase 5**: Cloud sync (add `data/remote/` layer)
4. **Deprecate `map/` package**: Fully migrate to domain models

When adding cloud sync:
- Create `RemoteDataSource` in `data/remote/`
- Update `PinRepositoryImpl` to coordinate local + remote
- Implement offline-first with WorkManager
- Handle conflict resolution

## Files to Reference

- `REFACTORING_PLAN.md`: Phase-by-phase refactoring details
- `REFACTORING_SUMMARY.md`: What changed, why, and benefits
- `README.md`: User-facing documentation and roadmap
- `domain/model/Pin.kt`: Core domain model example
- `ui/viewmodel/MapViewModel.kt`: ViewModel pattern example
- `data/repository/PinRepositoryImpl.kt`: Repository pattern example

## Code Style

- Follow Kotlin conventions (already established in codebase)
- Use KDoc for public APIs (examples in existing code)
- Prefer immutable data classes with `copy()`
- Extension functions for mapper logic
- Sealed classes for complex state (not yet used but recommended for errors)
