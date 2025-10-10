package com.carryzonemap.app.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.carryzonemap.app.BuildConfig
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.geojson.Feature

class MapFacade {

    private lateinit var featureDataStore: FeatureDataStore
    private lateinit var featureLayerManager: FeatureLayerManager
    private lateinit var mapInteractionHandler: MapInteractionHandler
    private val mapSetupOrchestrator = MapSetupOrchestrator()

    private var map: MapLibreMap? = null
    private var currentStyle: Style? = null

    companion object {
        // The base style URL, key will be appended from BuildConfig
        private const val BASE_STYLE_URL = "https://api.maptiler.com/maps/streets-v2/style.json?key="
        private const val USER_LOCATION_ZOOM_LEVEL = 13.0
    }

    fun setupMap(
        context: Context,
        map: MapLibreMap,
        onFeaturesChanged: (List<Feature>) -> Unit,
        initialLocation: LatLng? = null
    ) {
        val styleUrl = "$BASE_STYLE_URL${BuildConfig.MAPTILER_API_KEY}"
        println("Style URL: $styleUrl")

        this.map = map // Store the map instance
        featureLayerManager = FeatureLayerManager()

        featureDataStore = FeatureDataStore(context) { features ->
            currentStyle?.let { style ->
                featureLayerManager.updateDataSource(style, features)
            }
            onFeaturesChanged(features)
        }

        mapSetupOrchestrator.initialize(map, styleUrl, initialLocation) { loadedStyle ->
            this.currentStyle = loadedStyle
            featureLayerManager.addSourceAndLayer(loadedStyle, featureDataStore.getFeaturesSnapshot())
            enableLocationComponent(context, map, loadedStyle)
            mapInteractionHandler = MapInteractionHandler(map, featureDataStore)
            mapInteractionHandler.setupListeners()
        }
    }

    fun recenterToCurrentLocation() {
        map?.locationComponent?.let { component ->
            if (component.isLocationComponentActivated) {
                component.lastKnownLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, USER_LOCATION_ZOOM_LEVEL))
                }
            }
        }
    }

    @Suppress("MissingPermission")
    private fun enableLocationComponent(context: Context, map: MapLibreMap, style: Style) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val locationComponent = map.locationComponent
        val activationOptions = LocationComponentActivationOptions.builder(context, style).useDefaultLocationEngine(true).build()
        locationComponent.activateLocationComponent(activationOptions)
        locationComponent.isLocationComponentEnabled = true
        locationComponent.renderMode = RenderMode.COMPASS
    }

    fun addMarker(longitude: Double, latitude: Double) {
        if (::featureDataStore.isInitialized) {
            featureDataStore.addFeature(longitude, latitude)
        } else {
            println("MapFacade: addMarker called before map is setup.")
        }
    }
}
