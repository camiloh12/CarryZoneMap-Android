package com.carryzonemap.app.ui.map

import android.content.Context
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import timber.log.Timber

/**
 * Manages the MapLibre location component (blue dot showing user's position).
 * Single Responsibility: Location component configuration.
 */
class LocationComponentManager {

    /**
     * Enables the MapLibre location component to display the user's current location as a blue dot.
     * Also enables automatic tracking of the user's location as they move.
     *
     * Note: Requires location permission to be granted before calling this method.
     */
    @Suppress("MissingPermission")
    fun enableLocationComponent(
        context: Context,
        map: MapLibreMap,
        style: Style
    ) {
        val locationComponent = map.locationComponent
        val activationOptions = LocationComponentActivationOptions
            .builder(context, style)
            .useDefaultLocationEngine(true)
            .build()

        locationComponent.activateLocationComponent(activationOptions)
        locationComponent.isLocationComponentEnabled = true
        locationComponent.renderMode = RenderMode.COMPASS

        Timber.d("Location component enabled")
    }
}
