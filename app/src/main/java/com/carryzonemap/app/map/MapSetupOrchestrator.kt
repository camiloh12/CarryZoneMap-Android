package com.carryzonemap.app.map

import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

class MapSetupOrchestrator {
    companion object {
        private val DEFAULT_CAMERA_POSITION = LatLng(39.5, -98.35)
        private const val DEFAULT_ZOOM_LEVEL = 3.5
        private const val USER_LOCATION_ZOOM_LEVEL = 13.0 // Zoom closer when we have the user's location
    }

    fun initialize(
        map: MapLibreMap,
        styleUrl: String,
        initialLocation: LatLng?,
        onStyleReady: (Style) -> Unit,
    ) {
        map.setStyle(styleUrl) { style ->
            setInitialCameraPosition(map, initialLocation)
            onStyleReady(style)
        }
    }

    private fun setInitialCameraPosition(
        map: MapLibreMap,
        location: LatLng?,
    ) {
        val position = location ?: DEFAULT_CAMERA_POSITION
        val zoom = if (location != null) USER_LOCATION_ZOOM_LEVEL else DEFAULT_ZOOM_LEVEL
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, zoom))
    }
}
