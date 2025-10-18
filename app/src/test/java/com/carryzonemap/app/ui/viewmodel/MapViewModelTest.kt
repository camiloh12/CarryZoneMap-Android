package com.carryzonemap.app.ui.viewmodel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.carryzonemap.app.data.remote.datasource.OverpassDataSource
import com.carryzonemap.app.data.sync.SyncManager
import com.carryzonemap.app.data.sync.SyncStatus
import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.model.PinStatus
import com.carryzonemap.app.domain.repository.AuthRepository
import com.carryzonemap.app.domain.repository.PinRepository
import com.carryzonemap.app.ui.state.PinDialogState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MapViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepository: FakeRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeSyncManager: FakeSyncManager
    private lateinit var mockOverpassDataSource: OverpassDataSource
    private lateinit var mockLocationClient: FusedLocationProviderClient
    private lateinit var context: Context
    private lateinit var viewModel: MapViewModel

    private val testPin =
        Pin(
            id = "test-123",
            name = "Test POI",
            location = Location(latitude = 40.7128, longitude = -74.0060),
            status = PinStatus.ALLOWED,
        )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRepository()
        fakeAuthRepository = FakeAuthRepository()
        fakeSyncManager = FakeSyncManager()
        mockOverpassDataSource = mock(OverpassDataSource::class.java)
        mockLocationClient = mock(FusedLocationProviderClient::class.java)
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() =
        runTest {
            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(0, state.pins.size)
            assertNull(state.currentLocation)
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertTrue(state.pinDialogState is PinDialogState.Hidden)
        }

    @Test
    fun `observes pins from repository on initialization`() =
        runTest {
            fakeRepository.emitPins(listOf(testPin))

            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.pins.size)
            assertEquals(testPin.id, state.pins[0].id)
        }

    @Test
    fun `showCreatePinDialog opens dialog with correct location`() =
        runTest {
            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.showCreatePinDialog(name = "Test POI", longitude = -74.0060, latitude = 40.7128)
            testDispatcher.scheduler.advanceUntilIdle()

            val dialogState = viewModel.uiState.value.pinDialogState
            assertTrue(dialogState is PinDialogState.Creating)
            val creatingState = dialogState as PinDialogState.Creating
            assertEquals(40.7128, creatingState.location.latitude, 0.0001)
            assertEquals(-74.0060, creatingState.location.longitude, 0.0001)
            assertEquals(PinStatus.ALLOWED, creatingState.selectedStatus) // Default status
        }

    @Test
    fun `onDialogStatusSelected updates selected status in creating dialog`() =
        runTest {
            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.showCreatePinDialog(name = "Test POI", longitude = -74.0060, latitude = 40.7128)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onDialogStatusSelected(PinStatus.UNCERTAIN)
            testDispatcher.scheduler.advanceUntilIdle()

            val dialogState = viewModel.uiState.value.pinDialogState
            assertTrue(dialogState is PinDialogState.Creating)
            assertEquals(PinStatus.UNCERTAIN, (dialogState as PinDialogState.Creating).selectedStatus)
        }

    @Test
    fun `confirmPinDialog creates pin with selected status`() =
        runTest {
            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.showCreatePinDialog(name = "Test POI", longitude = -74.0060, latitude = 40.7128)
            viewModel.onDialogStatusSelected(PinStatus.NO_GUN)
            viewModel.confirmPinDialog()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, fakeRepository.pins.size)
            val addedPin = fakeRepository.pins.values.first()
            assertEquals(40.7128, addedPin.location.latitude, 0.0001)
            assertEquals(-74.0060, addedPin.location.longitude, 0.0001)
            assertEquals(PinStatus.NO_GUN, addedPin.status)

            // Dialog should be hidden after confirmation
            assertTrue(viewModel.uiState.value.pinDialogState is PinDialogState.Hidden)
        }

    @Test
    fun `showEditPinDialog opens dialog with existing pin data`() =
        runTest {
            fakeRepository.emitPins(listOf(testPin))

            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.showEditPinDialog(testPin.id)
            testDispatcher.scheduler.advanceUntilIdle()

            val dialogState = viewModel.uiState.value.pinDialogState
            assertTrue(dialogState is PinDialogState.Editing)
            val editingState = dialogState as PinDialogState.Editing
            assertEquals(testPin.id, editingState.pin.id)
            assertEquals(testPin.status, editingState.selectedStatus)
        }

    @Test
    fun `confirmPinDialog updates pin with new status`() =
        runTest {
            fakeRepository.emitPins(listOf(testPin))

            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.showEditPinDialog(testPin.id)
            viewModel.onDialogStatusSelected(PinStatus.UNCERTAIN)
            viewModel.confirmPinDialog()
            testDispatcher.scheduler.advanceUntilIdle()

            val updatedPin = fakeRepository.pins[testPin.id]
            assertEquals(PinStatus.UNCERTAIN, updatedPin?.status)
        }

    @Test
    fun `deletePinFromDialog removes pin`() =
        runTest {
            fakeRepository.emitPins(listOf(testPin))

            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.showEditPinDialog(testPin.id)
            viewModel.deletePinFromDialog()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(0, fakeRepository.pins.size)
        }

    @Test
    fun `dismissPinDialog closes dialog`() =
        runTest {
            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.showCreatePinDialog(name = "Test POI", longitude = -74.0060, latitude = 40.7128)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.pinDialogState is PinDialogState.Creating)

            viewModel.dismissPinDialog()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.pinDialogState is PinDialogState.Hidden)
        }

    @Test
    fun `clearError clears error state`() =
        runTest {
            fakeRepository.shouldThrowError = true

            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.error)

            viewModel.clearError()
            testDispatcher.scheduler.advanceUntilIdle()

            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `getCurrentLocationForCamera returns current location`() =
        runTest {
            val mockLocation = mock(android.location.Location::class.java)
            `when`(mockLocation.latitude).thenReturn(40.7128)
            `when`(mockLocation.longitude).thenReturn(-74.0060)
            `when`(mockLocationClient.lastLocation).thenReturn(Tasks.forResult(mockLocation))

            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onLocationPermissionResult(true)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.fetchCurrentLocation()
            testDispatcher.scheduler.advanceUntilIdle()

            val location = viewModel.getCurrentLocationForCamera()
            assertNotNull(location)
            assertEquals(40.7128, location!!.latitude, 0.0001)
            assertEquals(-74.0060, location.longitude, 0.0001)
        }

    @Test
    fun `onLocationPermissionResult updates permission state`() =
        runTest {
            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.hasLocationPermission)

            viewModel.onLocationPermissionResult(true)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.hasLocationPermission)
        }

    @Test
    fun `repository error is handled and shown in UI state`() =
        runTest {
            fakeRepository.shouldThrowError = true

            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.error!!.contains("Failed to load pins"))
        }

    @Test
    fun `signOut calls auth repository signOut`() =
        runTest {
            viewModel =
                MapViewModel(
                    fakeRepository,
                    fakeAuthRepository,
                    fakeSyncManager,
                    mockOverpassDataSource,
                    mockLocationClient,
                    context,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.signOut()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(fakeAuthRepository.signOutCalled)
        }

    /**
     * Fake repository implementation for testing.
     */
    private class FakeRepository : PinRepository {
        val pins = mutableMapOf<String, Pin>()
        private val pinsFlow = MutableStateFlow<List<Pin>>(emptyList())
        var shouldThrowError = false

        fun emitPins(newPins: List<Pin>) {
            newPins.forEach { pins[it.id] = it }
            pinsFlow.update { pins.values.toList() }
        }

        override fun getAllPins(): Flow<List<Pin>> {
            return kotlinx.coroutines.flow.flow {
                if (shouldThrowError) {
                    throw Exception("Repository error")
                }
                pinsFlow.collect { emit(it) }
            }
        }

        override suspend fun getPinById(pinId: String): Pin? = pins[pinId]

        override suspend fun addPin(pin: Pin) {
            pins[pin.id] = pin
            emitPins(pins.values.toList())
        }

        override suspend fun updatePin(pin: Pin) {
            if (pins.containsKey(pin.id)) {
                pins[pin.id] = pin
                emitPins(pins.values.toList())
            }
        }

        override suspend fun deletePin(pin: Pin) {
            pins.remove(pin.id)
            emitPins(pins.values.toList())
        }

        override suspend fun deleteAllPins() {
            pins.clear()
            emitPins(emptyList())
        }

        override suspend fun cyclePinStatus(pinId: String): Boolean {
            val pin = pins[pinId] ?: return false
            val updatedPin = pin.withNextStatus()
            pins[pinId] = updatedPin
            emitPins(pins.values.toList())
            return true
        }

        override suspend fun getPinCount(): Int = pins.size
    }

    /**
     * Fake auth repository for testing.
     */
    private class FakeAuthRepository : AuthRepository {
        var signOutCalled = false
        private var currentUser: String? = "test-user-id"

        override val authState: Flow<com.carryzonemap.app.domain.repository.AuthState> = emptyFlow()

        override val currentUserId: String?
            get() = currentUser

        override suspend fun signUpWithEmail(
            email: String,
            password: String,
        ): Result<com.carryzonemap.app.domain.model.User> {
            return Result.success(com.carryzonemap.app.domain.model.User(id = "test-user-id", email = email))
        }

        override suspend fun signInWithEmail(
            email: String,
            password: String,
        ): Result<com.carryzonemap.app.domain.model.User> {
            return Result.success(com.carryzonemap.app.domain.model.User(id = "test-user-id", email = email))
        }

        override suspend fun signOut(): Result<Unit> {
            signOutCalled = true
            currentUser = null
            return Result.success(Unit)
        }

        override suspend fun getCurrentUser(): com.carryzonemap.app.domain.model.User? {
            return currentUser?.let { com.carryzonemap.app.domain.model.User(id = it, email = "test@example.com") }
        }
    }

    /**
     * Fake sync manager for testing.
     */
    private class FakeSyncManager : SyncManager {
        override val syncStatus: Flow<SyncStatus> = emptyFlow()

        override suspend fun queuePinForUpload(pin: Pin) = Unit

        override suspend fun queuePinForUpdate(pin: Pin) = Unit

        override suspend fun queuePinForDeletion(pinId: String) = Unit

        override suspend fun syncWithRemote(): Result<Unit> = Result.success(Unit)

        override suspend fun getPendingOperationCount(): Int = 0

        override suspend fun clearQueue() = Unit

        override fun startRealtimeSubscription(): Flow<String> = emptyFlow()
    }
}
