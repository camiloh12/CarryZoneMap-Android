# CarryZoneMap

A modern Android application for mapping and tracking carry zones, built with **Kotlin**, **Jetpack Compose**, and **MapLibre**. This project demonstrates production-ready Android architecture with clean separation of concerns, reactive state management, and comprehensive dependency injection.

## 🏗️ Architecture

This app follows **Clean Architecture** principles with **MVVM** pattern:

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  (MapScreen, MapViewModel, MapUiState)                  │
│  • Jetpack Compose UI                                   │
│  • StateFlow for reactive updates                       │
│  • Hilt ViewModels                                      │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│                    Domain Layer                          │
│  (Pin, Location, PinStatus, PinRepository)              │
│  • Pure Kotlin business logic                           │
│  • No Android dependencies                              │
│  • Framework-agnostic interfaces                        │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│                     Data Layer                           │
│  (Room Database, PinRepositoryImpl, DAOs)               │
│  • Room for local persistence                           │
│  • Repository pattern                                   │
│  • Reactive Flow streams                                │
└─────────────────────────────────────────────────────────┘
```

### Key Technologies

- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt (Dagger)
- **Database**: Room
- **Async**: Kotlin Coroutines + Flow
- **Maps**: MapLibre (no API key required for demo tiles)
- **Location**: Google Play Services Location

## ✨ Features

### Current Features

- 📍 **Interactive Map**: Pan, zoom, and explore with MapLibre
- 📌 **Pin Management**:
  - Long-press to open dialog and create pins with chosen status
  - Tap existing pins to edit status or delete
  - Interactive dialog with visual status picker (green/yellow/red)
  - Pins persist in local Room database
- 🎨 **Color-Coded Status**:
  - 🟢 Green: Firearms allowed
  - 🟡 Yellow: Status uncertain
  - 🔴 Red: No firearms allowed
- 📍 **Location Services**:
  - Auto-center on user's location
  - Permission handling
  - Re-center FAB button
- 💾 **Offline-First**: All data stored locally with Room
- ⚡ **Reactive UI**: Real-time updates via Kotlin Flow

### Architecture Features

- ✅ Clean Architecture with domain/data/presentation layers
- ✅ MVVM pattern with reactive StateFlow
- ✅ Repository pattern for data abstraction
- ✅ Hilt dependency injection
- ✅ Room database with type-safe DAOs
- ✅ Proper error handling and loading states
- ✅ Comprehensive testing (81 unit tests, 100% pass rate)
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

2. **Configure MapTiler API Key** (optional - demo tiles work without it)

   Create or edit `local.properties`:
   ```properties
   MAPTILER_API_KEY=your_api_key_here
   ```

   Get a free API key at [MapTiler](https://www.maptiler.com/) or use the demo tiles (already configured).

3. **Install Java 21** (if not already installed)
   ```bash
   sudo apt update
   sudo apt install -y openjdk-21-jdk
   ```

4. **Open in Android Studio**
   - Open the project folder
   - Wait for Gradle sync to complete
   - Android Studio will use the Java 21 path from `gradle.properties`

5. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```

   Or click the ▶️ Run button in Android Studio.

### First Run

1. Grant location permission when prompted
2. Map will center on your current location
3. Long-press anywhere to open the pin creation dialog
4. Select a status (Allowed/Uncertain/No Guns) and tap "Create"
5. Tap any existing pin to edit its status or delete it
6. Use the 📍 FAB button to re-center on your location

## 📂 Project Structure

```
app/src/main/java/com/carryzonemap/app/
│
├── domain/                          # Business Logic (Pure Kotlin)
│   ├── model/                       # Domain models
│   │   ├── Pin.kt                   # Core pin entity
│   │   ├── Location.kt              # Lat/long value object
│   │   ├── PinStatus.kt             # Status enum with business rules
│   │   └── PinMetadata.kt           # Extensible metadata
│   ├── mapper/
│   │   └── PinMapper.kt             # Domain ↔ MapLibre conversions
│   └── repository/
│       └── PinRepository.kt         # Repository interface
│
├── data/                            # Data Layer
│   ├── local/
│   │   ├── entity/
│   │   │   └── PinEntity.kt         # Room database table
│   │   ├── dao/
│   │   │   └── PinDao.kt            # Database operations
│   │   └── database/
│   │       └── CarryZoneDatabase.kt # Room DB configuration
│   ├── mapper/
│   │   └── EntityMapper.kt          # Entity ↔ Domain conversions
│   └── repository/
│       └── PinRepositoryImpl.kt     # Repository implementation
│
├── ui/                              # Presentation Layer
│   ├── MapScreen.kt                 # Main Compose UI
│   ├── components/
│   │   └── PinDialog.kt             # Pin creation/editing dialog
│   ├── state/
│   │   ├── MapUiState.kt            # Immutable UI state
│   │   └── PinDialogState.kt        # Dialog state management
│   └── viewmodel/
│       └── MapViewModel.kt          # State management
│
├── di/                              # Dependency Injection
│   ├── DatabaseModule.kt            # Room DB providers
│   ├── RepositoryModule.kt          # Repository bindings
│   └── LocationModule.kt            # Location service providers
│
├── map/                             # Map Rendering (Legacy)
│   └── FeatureLayerManager.kt       # MapLibre layer management
│
├── MainActivity.kt                  # Entry point
└── CarryZoneApplication.kt          # Hilt application class
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

### Phase 5: Cloud Integration

- [ ] **Backend Setup**
  - [ ] Choose backend (Firestore or Supabase)
  - [ ] Set up authentication
  - [ ] Configure database rules

- [ ] **Offline-First Sync**
  - [ ] Remote data source implementation
  - [ ] Conflict resolution strategy
  - [ ] Background sync worker
  - [ ] Network state handling

- [ ] **Real-time Updates**
  - [ ] WebSocket/Firestore listeners
  - [ ] Live pin updates
  - [ ] Push notifications for nearby changes

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

- **[REFACTORING_PLAN.md](./REFACTORING_PLAN.md)**: Detailed refactoring plan with phase breakdown
- **[REFACTORING_SUMMARY.md](./REFACTORING_SUMMARY.md)**: Comprehensive summary of changes and benefits
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

**Built with ❤️ using modern Android architecture**

*Last Updated: 2025-10-10*
