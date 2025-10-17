package com.carryzonemap.app.ui.map

import com.carryzonemap.app.domain.model.Location
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import timber.log.Timber

/**
 * Handles camera movements and positioning for the map.
 * Single Responsibility: Camera management only.
 */
class CameraController(private val map: MapLibreMap) {
    /**
     * Moves camera to the default position (center of US).
     */
    fun moveToDefaultPosition() {
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                MapConstants.DEFAULT_CAMERA_POSITION,
                MapConstants.ZOOM_LEVEL_DEFAULT,
            ),
        )
        Timber.d("Camera moved to default position")
    }

    /**
     * Animates camera to user's current location with appropriate zoom.
     */
    fun animateToUserLocation(location: Location) {
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude),
                MapConstants.ZOOM_LEVEL_USER_LOCATION,
            ),
        )
        Timber.d("Camera animated to user location: ${location.latitude}, ${location.longitude}")
    }

    /**
     * Animates camera to a specific LatLng with user location zoom level.
     */
    fun animateToLocation(latLng: LatLng) {
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLng,
                MapConstants.ZOOM_LEVEL_USER_LOCATION,
            ),
        )
        Timber.d("Camera animated to: ${latLng.latitude}, ${latLng.longitude}")
    }
}
