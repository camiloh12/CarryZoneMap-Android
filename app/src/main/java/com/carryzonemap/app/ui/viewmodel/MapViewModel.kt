package com.carryzonemap.app.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.model.PinStatus
import com.carryzonemap.app.domain.repository.PinRepository
import com.carryzonemap.app.ui.state.MapUiState
import com.carryzonemap.app.ui.state.PinDialogState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel for the map screen.
 * Manages UI state and coordinates between the UI and domain/data layers.
 *
 * @property pinRepository Repository for pin data operations
 * @property fusedLocationClient Client for accessing device location
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val pinRepository: PinRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var locationCallback: LocationCallback? = null

    init {
        observePins()
        checkLocationPermission()
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }

    /**
     * Observes pin changes from the repository and updates UI state.
     */
    private fun observePins() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            pinRepository.getAllPins()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load pins: ${error.message}"
                        )
                    }
                }
                .collect { pins ->
                    _uiState.update {
                        it.copy(
                            pins = pins,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    /**
     * Checks if location permission is granted.
     */
    private fun checkLocationPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        _uiState.update { it.copy(hasLocationPermission = hasPermission) }

        // Start location updates if permission is already granted
        if (hasPermission) {
            startLocationUpdates()
        }
    }

    /**
     * Called when location permission result is received.
     */
    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            startLocationUpdates()
        }
    }

    /**
     * Fetches the user's current location.
     */
    fun fetchCurrentLocation() {
        if (!_uiState.value.hasLocationPermission) {
            return
        }

        viewModelScope.launch {
            try {
                @Suppress("MissingPermission")
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    _uiState.update {
                        it.copy(
                            currentLocation = Location(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to get location: ${e.message}")
                }
            }
        }
    }

    /**
     * Shows the dialog to create a new pin at the specified location.
     */
    fun showCreatePinDialog(longitude: Double, latitude: Double) {
        val location = Location.fromLngLat(longitude, latitude)
        _uiState.update {
            it.copy(pinDialogState = PinDialogState.Creating(location))
        }
    }

    /**
     * Shows the dialog to edit an existing pin.
     */
    fun showEditPinDialog(pinId: String) {
        val pin = _uiState.value.pins.find { it.id == pinId }
        if (pin != null) {
            _uiState.update {
                it.copy(pinDialogState = PinDialogState.Editing(pin))
            }
        }
    }

    /**
     * Updates the selected status in the dialog.
     */
    fun onDialogStatusSelected(status: PinStatus) {
        val currentState = _uiState.value.pinDialogState
        _uiState.update {
            it.copy(
                pinDialogState = when (currentState) {
                    is PinDialogState.Creating -> currentState.copy(selectedStatus = status)
                    is PinDialogState.Editing -> currentState.copy(selectedStatus = status)
                    is PinDialogState.Hidden -> currentState
                }
            )
        }
    }

    /**
     * Confirms the pin creation or edit from the dialog.
     */
    fun confirmPinDialog() {
        val dialogState = _uiState.value.pinDialogState
        viewModelScope.launch {
            try {
                when (dialogState) {
                    is PinDialogState.Creating -> {
                        val pin = Pin.fromLngLat(
                            longitude = dialogState.location.longitude,
                            latitude = dialogState.location.latitude,
                            status = dialogState.selectedStatus
                        )
                        pinRepository.addPin(pin)
                    }
                    is PinDialogState.Editing -> {
                        val updatedPin = dialogState.pin.withStatus(dialogState.selectedStatus)
                        pinRepository.updatePin(updatedPin)
                    }
                    is PinDialogState.Hidden -> {
                        // No-op
                    }
                }
                dismissPinDialog()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to save pin: ${e.message}")
                }
            }
        }
    }

    /**
     * Dismisses the pin dialog.
     */
    fun dismissPinDialog() {
        _uiState.update {
            it.copy(pinDialogState = PinDialogState.Hidden)
        }
    }

    /**
     * Deletes the pin being edited in the dialog.
     */
    fun deletePinFromDialog() {
        val dialogState = _uiState.value.pinDialogState
        if (dialogState is PinDialogState.Editing) {
            viewModelScope.launch {
                try {
                    pinRepository.deletePin(dialogState.pin)
                    dismissPinDialog()
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = "Failed to delete pin: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Returns the current location for camera positioning.
     */
    fun getCurrentLocationForCamera(): Location? {
        return _uiState.value.currentLocation
    }

    /**
     * Starts continuous location updates.
     * This enables real-time tracking of the user's location as they move.
     */
    @Suppress("MissingPermission")
    private fun startLocationUpdates() {
        if (!_uiState.value.hasLocationPermission) {
            return
        }

        // Stop any existing updates first
        stopLocationUpdates()

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // Update every 10 seconds
        ).apply {
            setMinUpdateIntervalMillis(5000L) // But not more frequently than every 5 seconds
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    _uiState.update {
                        it.copy(
                            currentLocation = Location(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        )
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            null // Use main looper
        )
    }

    /**
     * Stops location updates to save battery when the ViewModel is cleared.
     */
    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }
}
