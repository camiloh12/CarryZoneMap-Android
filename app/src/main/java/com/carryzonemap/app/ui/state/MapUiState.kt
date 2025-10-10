package com.carryzonemap.app.ui.state

import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin

/**
 * UI state for the map screen.
 *
 * @property pins List of pins to display on the map
 * @property currentLocation User's current location (null if not available)
 * @property isLoading Whether data is currently loading
 * @property error Error message if something went wrong
 * @property hasLocationPermission Whether location permission is granted
 */
data class MapUiState(
    val pins: List<Pin> = emptyList(),
    val currentLocation: Location? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasLocationPermission: Boolean = false
)
