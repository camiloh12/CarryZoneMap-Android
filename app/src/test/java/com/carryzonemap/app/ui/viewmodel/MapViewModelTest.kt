package com.carryzonemap.app.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.model.PinStatus
import com.carryzonemap.app.domain.repository.PinRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var mockLocationClient: FusedLocationProviderClient
    private lateinit var context: Context
    private lateinit var viewModel: MapViewModel

    private val testPin = Pin(
        id = "test-123",
        location = Location(latitude = 40.7128, longitude = -74.0060),
        status = PinStatus.ALLOWED
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRepository()
        mockLocationClient = mock(FusedLocationProviderClient::class.java)
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        viewModel = MapViewModel(fakeRepository, mockLocationClient, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.pins.size)
        assertNull(state.currentLocation)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `observes pins from repository on initialization`() = runTest {
        fakeRepository.emitPins(listOf(testPin))

        viewModel = MapViewModel(fakeRepository, mockLocationClient, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.pins.size)
        assertEquals(testPin.id, state.pins[0].id)
    }

    @Test
    fun `addPin adds pin to repository`() = runTest {
        viewModel = MapViewModel(fakeRepository, mockLocationClient, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addPin(-74.0060, 40.7128)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.pins.size)
        val addedPin = fakeRepository.pins.values.first()
        assertEquals(40.7128, addedPin.location.latitude, 0.0001)
        assertEquals(-74.0060, addedPin.location.longitude, 0.0001)
    }

    @Test
    fun `cyclePinStatus updates pin status`() = runTest {
        fakeRepository.emitPins(listOf(testPin))

        viewModel = MapViewModel(fakeRepository, mockLocationClient, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.cyclePinStatus(testPin.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedPin = fakeRepository.pins[testPin.id]
        assertEquals(PinStatus.UNCERTAIN, updatedPin?.status)
    }

    @Test
    fun `cyclePinStatus sets error when pin not found`() = runTest {
        viewModel = MapViewModel(fakeRepository, mockLocationClient, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.cyclePinStatus("nonexistent")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error!!.contains("Pin not found"))
    }

    @Test
    fun `deletePin removes pin from repository`() = runTest {
        fakeRepository.emitPins(listOf(testPin))

        viewModel = MapViewModel(fakeRepository, mockLocationClient, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deletePin(testPin)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, fakeRepository.pins.size)
    }

    @Test
    fun `clearError clears error state`() = runTest {
        viewModel = MapViewModel(fakeRepository, mockLocationClient, context)
        testDispatcher.scheduler.advanceUntilIdle()

        // Trigger an error
        viewModel.cyclePinStatus("nonexistent")
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        // Clear the error
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `getCurrentLocationForCamera returns current location`() = runTest {
        val mockLocation = mock(android.location.Location::class.java)
        `when`(mockLocation.latitude).thenReturn(40.7128)
        `when`(mockLocation.longitude).thenReturn(-74.0060)
        `when`(mockLocationClient.lastLocation).thenReturn(Tasks.forResult(mockLocation))

        viewModel = MapViewModel(fakeRepository, mockLocationClient, context)
        testDispatcher.scheduler.advanceUntilIdle()

        // Grant location permission for this test
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
    fun `onLocationPermissionResult updates permission state`() = runTest {
        viewModel = MapViewModel(fakeRepository, mockLocationClient, context)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasLocationPermission)

        viewModel.onLocationPermissionResult(true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasLocationPermission)
    }

    @Test
    fun `repository error is handled and shown in UI state`() = runTest {
        fakeRepository.shouldThrowError = true

        viewModel = MapViewModel(fakeRepository, mockLocationClient, context)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error!!.contains("Failed to load pins"))
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
}
