# Concealed Carry Map

A modern Android application for mapping and tracking carry zones with **cloud synchronization**, built with **Kotlin**, **Jetpack Compose**, **MapLibre**, and **Supabase**. This project demonstrates production-ready Android architecture with clean separation of concerns, reactive state management, offline-first data sync, and comprehensive dependency injection.

## 🏗️ Architecture

This app follows **Clean Architecture** principles with **MVVM** pattern:

```
┌──────────────────────────────────────────────────────────────────┐
│                       Presentation Layer                          │
│  (MapScreen, MapViewModel, LoginScreen, AuthViewModel)          │
│  • Jetpack Compose UI                                            │
│  • StateFlow for reactive updates                                │
│  • Hilt ViewModels                                               │
└─────────────────────────────┬────────────────────────────────────┘
                              │
┌─────────────────────────────▼────────────────────────────────────┐
│                        Domain Layer                               │
│  (Pin, User, PinRepository, AuthRepository)                      │
│  • Pure Kotlin business logic                                    │
│  • No Android dependencies                                       │
│  • Framework-agnostic interfaces                                 │
└─────────────────────────────┬────────────────────────────────────┘
                              │
┌─────────────────────────────▼────────────────────────────────────┐
│                         Data Layer                                │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────┐ │
│  │  Local (Room)    │←─┤   SyncManager    │─→│ Remote (Supabase)│
│  │  • PinDao        │  │  • Queue ops     │  │ • Auth          ││
│  │  • SyncQueueDao  │  │  • Upload/Download│  │ • Postgrest     ││
│  │  • Instant reads │  │  • Conflict res. │  │ • Realtime      ││
│  └──────────────────┘  └──────────────────┘  └────────────────┘ │
│                              ↓                                    │
│                      ┌───────────────┐                            │
│                      │ NetworkMonitor │                           │
│                      └───────────────┘                            │
└──────────────────────────────────────────────────────────────────┘

Offline-First: Write to Room (instant UI) → Queue for sync → Upload when online
```

### Key Technologies

- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture + Offline-First Sync
- **DI**: Hilt (Dagger)
- **Database**: Room (local) + Supabase PostgreSQL (remote)
- **Authentication**: Supabase Auth (email/password)
- **Sync**: WorkManager + Custom SyncManager
- **Async**: Kotlin Coroutines + Flow
- **Maps**: MapLibre (no API key required for demo tiles)
- **Location**: Google Play Services Location
- **Networking**: Ktor Client (for Supabase)

### Design Patterns

**MapScreen Architecture** (Refactored Oct 2025):
- **Chain of Responsibility**: Click handling with `FeatureClickHandler`
  - `ExistingPinDetector`: Handles clicks on user pins
  - `OverpassPoiDetector`: Handles Overpass POI layer clicks
  - `MapTilerPoiDetector`: Handles base map POI clicks
- **Single Responsibility**: Focused helper classes for map operations
  - `CameraController`: Camera positioning only
  - `MapLayerManager`: POI layer management only
  - `LocationComponentManager`: Location setup only
- **DRY Principle**: All constants centralized in `MapConstants`
- **Open/Closed**: Extensible click detection without modifying existing code

## ✨ Features

### Current Features

- 📍 **Interactive Map**: Pan, zoom, and explore with MapLibre
- 📌 **Pin Management**:
  - Long-press to open dialog and create pins with chosen status
  - Tap existing pins to edit status or delete
  - Interactive dialog with visual status picker (green/yellow/red)
  - Pins tagged with creator ID for accountability
- 🎨 **Color-Coded Status**:
  - 🟢 Green: Firearms allowed
  - 🟡 Yellow: Status uncertain
  - 🔴 Red: No firearms allowed
- 📍 **Location Services**:
  - Auto-center on user's location
  - Permission handling
  - Re-center FAB button
- 🔐 **User Authentication**:
  - Email/password sign-up and login
  - Secure session management with Supabase
  - Persistent auth across app restarts
- ☁️ **Cloud Synchronization**:
  - **Offline-first**: Works completely offline with local Room database
  - **Auto-sync**: Changes automatically sync to Supabase when online
  - **Conflict resolution**: Last-write-wins strategy with timestamps
  - **Background sync**: WorkManager handles periodic synchronization
  - **Real-time ready**: Infrastructure for live multi-device updates
- 💾 **Dual-Database Architecture**:
  - Local Room database for instant access
  - Remote Supabase PostgreSQL for cloud storage
  - Queue-based sync with automatic retry
- ⚡ **Reactive UI**: Real-time updates via Kotlin Flow

### Architecture Features

- ✅ Clean Architecture with domain/data/presentation layers
- ✅ MVVM pattern with reactive StateFlow
- ✅ Repository pattern for data abstraction
- ✅ **Offline-first sync architecture** with queue-based operations
- ✅ **Dual-database system**: Room (local) + Supabase (remote)
- ✅ **Conflict resolution**: Last-write-wins with timestamps
- ✅ **Network monitoring**: Reactive connectivity tracking
- ✅ **SOLID principles**: Single Responsibility, Open/Closed applied throughout
- ✅ **Design patterns**: Chain of Responsibility, Strategy, Factory
- ✅ **DRY principle**: Centralized constants, no code duplication
- ✅ Hilt dependency injection with WorkManager integration
- ✅ Room database with type-safe DAOs and migrations
- ✅ Supabase integration (Auth, Postgrest, Realtime, Storage)
- ✅ Proper error handling and loading states
- ✅ Comprehensive testing (98 unit tests, 100% pass rate)
- ✅ Code quality tools (Detekt + KtLint)

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or newer
- **JDK**: Java 21 (configured via gradle.properties)
- **Android SDK**: API 34
- **Min SDK**: API 26 (Android 8.0)

### Setup

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd CarryZoneMap_minimal_maplibre
   ```

2. **Configure API Keys**

   Create or edit `local.properties`:
   ```properties
   # MapTiler (optional - demo tiles work without it)
   MAPTILER_API_KEY=your_maptiler_key_here

   # Supabase (required for cloud sync)
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your_supabase_anon_key_here
   ```

   **MapTiler**: Get a free API key at [MapTiler](https://www.maptiler.com/) or use demo tiles.

   **Supabase Setup** (required for authentication and sync):
   1. Create a free account at [Supabase](https://supabase.com)
   2. Create a new project
   3. Go to **Settings → API** to find your URL and anon key
   4. Go to **SQL Editor** and run the migration from `supabase/migrations/001_initial_schema.sql`
   5. Go to **Authentication → Providers → Email** and disable "Confirm email" for development
   6. See [SUPABASE_PROGRESS.md](./SUPABASE_PROGRESS.md) for detailed setup instructions

3. **Install Java 21** (if not already installed)
   ```bash
   sudo apt update
   sudo apt install -y openjdk-21-jdk
   ```

4. **Open in Android Studio**
   - Open the project folder
   - Wait for Gradle sync to complete
   - Android Studio will use the Java 21 path from `gradle.properties`

5. **Clean, Build and Run**
   ```bash
   ./gradlew clean assembleDebug
   ```

   Or click the ▶️ Run button in Android Studio.

### First Run

1. **Sign up/Login**: Create an account or sign in with email/password
2. **Grant location permission** when prompted
3. **Map centers** on your current location
4. **Create pins**: Long-press anywhere to open the pin creation dialog
5. **Select status**: Choose Allowed/Uncertain/No Guns and tap "Create"
6. **Edit pins**: Tap any existing pin to edit its status or delete it
7. **Re-center**: Use the 📍 FAB button to return to your location
8. **Offline mode**: Works completely offline - changes sync automatically when online

## 📂 Project Structure

```
app/src/main/java/com/carryzonemap/app/
│
├── domain/                          # Business Logic (Pure Kotlin)
│   ├── model/                       # Domain models
│   │   ├── Pin.kt                   # Core pin entity
│   │   ├── User.kt                  # User model
│   │   ├── Location.kt              # Lat/long value object
│   │   ├── PinStatus.kt             # Status enum with business rules
│   │   └── PinMetadata.kt           # Extensible metadata
│   ├── mapper/
│   │   └── PinMapper.kt             # Domain ↔ MapLibre conversions
│   └── repository/
│       ├── PinRepository.kt         # Pin repository interface
│       └── AuthRepository.kt        # Auth repository interface
│
├── data/                            # Data Layer
│   ├── local/
│   │   ├── entity/
│   │   │   ├── PinEntity.kt         # Room database table
│   │   │   └── SyncQueueEntity.kt   # Sync queue table
│   │   ├── dao/
│   │   │   ├── PinDao.kt            # Pin database operations
│   │   │   └── SyncQueueDao.kt      # Sync queue operations
│   │   └── database/
│   │       └── CarryZoneDatabase.kt # Room DB configuration
│   ├── remote/
│   │   ├── dto/
│   │   │   └── SupabasePinDto.kt    # Supabase data transfer object
│   │   ├── mapper/
│   │   │   └── SupabaseMapper.kt    # DTO ↔ Domain conversions
│   │   └── datasource/
│   │       ├── RemotePinDataSource.kt    # Remote data source interface
│   │       └── SupabasePinDataSource.kt  # Supabase implementation
│   ├── network/
│   │   └── NetworkMonitor.kt        # Network connectivity monitoring
│   ├── sync/
│   │   ├── SyncManager.kt           # Sync manager interface
│   │   ├── SyncManagerImpl.kt       # Offline-first sync implementation
│   │   ├── SyncWorker.kt            # Background sync worker
│   │   ├── SyncOperation.kt         # Sync operation types (Create/Update/Delete)
│   │   └── SyncStatus.kt            # Sync status states
│   ├── mapper/
│   │   └── EntityMapper.kt          # Entity ↔ Domain conversions
│   └── repository/
│       ├── PinRepositoryImpl.kt     # Pin repository with sync
│       └── SupabaseAuthRepository.kt # Auth repository implementation
│
├── ui/                              # Presentation Layer
│   ├── MapScreen.kt                 # Main map Compose UI
│   ├── auth/
│   │   ├── LoginScreen.kt           # Login/signup UI
│   │   └── AuthViewModel.kt         # Auth state management
│   ├── components/
│   │   └── PinDialog.kt             # Pin creation/editing dialog
│   ├── map/                         # Map-specific helpers (Oct 2025 refactor)
│   │   ├── MapConstants.kt          # Centralized constants (zoom, colors, etc.)
│   │   ├── FeatureClickHandler.kt   # Chain of Responsibility for clicks
│   │   ├── CameraController.kt      # Camera positioning logic
│   │   ├── MapLayerManager.kt       # POI layer management
│   │   └── LocationComponentManager.kt # Location component setup
│   ├── state/
│   │   ├── MapUiState.kt            # Immutable UI state
│   │   └── PinDialogState.kt        # Dialog state management
│   └── viewmodel/
│       └── MapViewModel.kt          # Map state management
│
├── di/                              # Dependency Injection
│   ├── DatabaseModule.kt            # Room DB providers & migrations
│   ├── RepositoryModule.kt          # Repository & data source bindings
│   ├── LocationModule.kt            # Location service providers
│   ├── SupabaseModule.kt            # Supabase client providers
│   └── SyncModule.kt                # Sync manager bindings
│
├── map/                             # Map Rendering (Legacy)
│   └── FeatureLayerManager.kt       # MapLibre layer management
│
├── MainActivity.kt                  # Entry point with auth flow
└── CarryZoneApplication.kt          # Hilt application with WorkManager
```

## 🧪 Testing

### Run Tests

```bash
# Unit tests
./gradlew test

# Instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest

# All tests
./gradlew check
```

### Test Coverage

The architecture is designed for testability:

- **ViewModels**: Test with fake repositories
- **Repositories**: Test with in-memory Room database
- **Domain Models**: Pure Kotlin, easy to test
- **Mappers**: Simple unit tests

Example test structure:
```kotlin
// ViewModelTest with fake repository
class MapViewModelTest {
    @Test
    fun `creating pin via dialog updates state correctly`() = runTest {
        val fakeRepo = FakePinRepository()
        val viewModel = MapViewModel(fakeRepo, fakeLocationClient, context)

        viewModel.showCreatePinDialog(-122.0, 37.0)
        viewModel.onDialogStatusSelected(PinStatus.ALLOWED)
        viewModel.confirmPinDialog()

        val state = viewModel.uiState.first()
        assertEquals(1, state.pins.size)
    }
}
```

### Current Test Coverage

✅ **81 unit tests** with **100% pass rate**:
- 27 tests for domain models (Location, Pin, PinStatus)
- 25 tests for mappers (EntityMapper, PinMapper)
- 12 tests for PinRepository
- 10 tests for MapViewModel
- 7 tests for legacy components

All tests use best practices:
- Robolectric for Android framework testing
- Turbine for Flow testing
- Fake repositories for ViewModel testing
- Mockito for mocking dependencies

## 🔍 Code Quality

This project uses **static analysis** and **formatting tools** to maintain code quality:

### Detekt (Static Analysis)

```bash
# Run Detekt analysis
./gradlew detekt
```

**Configuration** (`detekt.yml`):
- Complexity thresholds adjusted for Compose functions
- Android-specific rules enabled
- Magic number exceptions for coordinates and constants
- **Current status**: ✅ 0 violations

### KtLint (Code Formatting)

```bash
# Check code formatting
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat
```

**Configuration**:
- Android conventions enabled
- Compose-specific function naming allowed (capital letter functions)
- Wildcard imports allowed for Compose packages
- **Current status**: ✅ Passing (Compose-specific warnings suppressed)

### Running All Quality Checks

```bash
# Run tests + Detekt + KtLint in one command
./gradlew check
```

This will:
1. Compile the code
2. Run all unit tests
3. Run Detekt static analysis
4. Run KtLint formatting checks
5. Generate reports in `app/build/reports/`

## 🔧 Configuration

### Gradle Properties

The project is configured to use Java 21 via `gradle.properties`:

```properties
org.gradle.java.home=/usr/lib/jvm/java-1.21.0-openjdk-amd64
```

Adjust this path if your Java installation is elsewhere.

### Build Variants

- **Debug**: Development build with logging
- **Release**: Production build (ProGuard rules to be added)

## 🤖 Working with Claude Code

This project was architected and refactored with assistance from [Claude Code](https://claude.com/claude-code). Here's how to continue using Claude for development:

### Setting Up Claude Code

1. **Install Claude Code** (if not already installed)
   ```bash
   # Follow instructions at https://claude.com/claude-code
   ```

2. **Open this project with Claude**
   ```bash
   cd /path/to/CarryZoneMap_minimal_maplibre
   claude-code .
   ```

### Example Tasks for Claude

**Add a new feature:**
```
"Add a search functionality to find pins by location name. Use the existing
architecture pattern - create domain models, repository interface, and update
the ViewModel/UI accordingly."
```

**Refactor existing code:**
```
"Refactor the map/ package to use domain models instead of MapLibre Features.
Follow the same pattern we used for the rest of the app."
```

**Add tests:**
```
"Write comprehensive unit tests for MapViewModel covering all user interactions
(add pin, cycle status, delete pin, location permissions)."
```

**Implement cloud sync:**
```
"Implement Firestore integration for cloud sync. Create a RemoteDataSource,
update PinRepository to handle both local and remote data, and implement
offline-first sync logic."
```

### Claude Best Practices for This Project

1. **Maintain Architecture**: Always follow the domain/data/presentation pattern
2. **Use Existing Patterns**: Look at PinRepository/MapViewModel for reference
3. **Write Tests**: Ask Claude to write tests alongside new features
4. **Update Docs**: Request documentation updates with code changes
5. **Follow Conventions**: Stick to the established naming and package structure

### Useful Claude Commands

- "Explain how the pin status cycling works"
- "Show me how to add a new field to Pin model"
- "Create a migration guide for adding a new Room column"
- "Generate a Firestore integration plan"
- "Write integration tests for the repository"

## 📋 Next Steps & Roadmap

### Phase 3: Testing Infrastructure (Immediate Priority)

- [ ] **Unit Tests**
  - [ ] MapViewModel tests (all operations)
  - [ ] PinRepository tests with fake DAO
  - [ ] Domain model tests (Pin, Location, PinStatus)
  - [ ] Mapper tests (PinMapper, EntityMapper)

- [ ] **Integration Tests**
  - [ ] Room database tests with in-memory DB
  - [ ] Repository integration tests
  - [ ] End-to-end data flow tests

- [ ] **UI Tests**
  - [ ] Compose UI tests for MapScreen
  - [ ] Permission flow tests
  - [ ] Error state tests

### Phase 4: Enhanced Features

- [ ] **Search & Filtering**
  - [ ] Search pins by location/address
  - [ ] Filter by status (show only green/red/yellow)
  - [ ] Radius-based filtering

- [ ] **Pin Details**
  - [ ] Detailed view with notes
  - [ ] Photo attachment (with camera integration)
  - [ ] Face/license plate blurring (OpenCV)
  - [ ] Timestamp and user information

- [ ] **Social Features**
  - [ ] User authentication (Firebase Auth)
  - [ ] Voting system for pin accuracy
  - [ ] Community moderation
  - [ ] Comments and reports

### Phase 5: Cloud Integration ✅ (Mostly Complete)

- [x] **Backend Setup**
  - [x] Chose Supabase as backend
  - [x] Set up email/password authentication
  - [x] Configure database schema with RLS policies

- [x] **Offline-First Sync**
  - [x] Remote data source implementation (SupabasePinDataSource)
  - [x] Conflict resolution strategy (last-write-wins)
  - [x] Background sync worker (SyncWorker with WorkManager)
  - [x] Network state handling (NetworkMonitor)
  - [x] Queue-based offline operations (SyncQueue)

- [ ] **Real-time Updates** (Infrastructure ready, needs activation)
  - [x] Supabase Realtime channel subscription implemented
  - [ ] Enable real-time in SyncManager
  - [ ] Test multi-device live updates
  - [ ] Push notifications for nearby changes (future)

### Phase 6: Polish & Production

- [x] **Code Quality** ✅
  - [x] Add Detekt for static analysis
  - [x] Add KtLint for code formatting
  - [ ] Configure ProGuard for release builds
  - [ ] Set up CI/CD pipeline

- [ ] **Performance**
  - [ ] Optimize database queries
  - [ ] Implement pagination for large datasets
  - [ ] Add map clustering for dense areas
  - [ ] Profile and optimize memory usage

- [ ] **UX Improvements**
  - [ ] Onboarding flow for new users
  - [ ] Settings screen
  - [ ] Theme customization (dark mode)
  - [ ] Accessibility improvements

- [ ] **Analytics & Monitoring**
  - [ ] Firebase Analytics
  - [ ] Crash reporting (Crashlytics)
  - [ ] Performance monitoring
  - [ ] User behavior tracking

### Phase 7: Advanced Features

- [ ] **Map Enhancements**
  - [ ] Multiple map styles
  - [ ] Satellite/terrain views
  - [ ] Custom pin icons
  - [ ] Heat map visualization

- [ ] **Data Export/Import**
  - [ ] Export pins to CSV/JSON
  - [ ] Import from other sources
  - [ ] Backup/restore functionality

- [ ] **Gamification**
  - [ ] User reputation system
  - [ ] Achievements/badges
  - [ ] Leaderboards

## 📚 Documentation

- **[SUPABASE_INTEGRATION_PLAN.md](./SUPABASE_INTEGRATION_PLAN.md)**: Complete plan for Supabase cloud sync (Phases 1-4 complete)
- **[SUPABASE_PROGRESS.md](./SUPABASE_PROGRESS.md)**: Implementation progress and setup guide
- **[REFACTORING_PLAN.md](./REFACTORING_PLAN.md)**: Original refactoring plan with phase breakdown
- **[REFACTORING_SUMMARY.md](./REFACTORING_SUMMARY.md)**: Comprehensive summary of architectural changes
- **[CLAUDE.md](./CLAUDE.md)**: Guidance for Claude Code when working with this codebase
- **Architecture Guide**: See the Architecture section above
- **API Documentation**: KDoc comments throughout the codebase

## 🤝 Contributing

Contributions are welcome! Please follow the existing architecture patterns:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Follow the existing code structure (domain/data/presentation)
4. Write tests for new functionality
5. Ensure the build passes (`./gradlew check`)
6. Commit with clear messages
7. Push to your branch
8. Open a Pull Request

### Code Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable names
- Add KDoc comments for public APIs
- Keep functions small and focused
- Follow SOLID principles

## 📄 License

[Add your license here]

## 🙏 Acknowledgments

- **MapLibre**: Open-source mapping platform
- **Jetpack Compose**: Modern Android UI toolkit
- **Hilt**: Dependency injection framework
- **Room**: Robust database solution
- **Claude Code**: AI-assisted development

## 📞 Contact

[Add your contact information]

---

**Built with ❤️ using modern Android architecture + cloud sync**

*Last Updated: 2025-10-17 - MapScreen refactoring with SOLID/DRY principles*
