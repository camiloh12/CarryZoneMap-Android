package com.carryzonemap.app.ui.map

import com.carryzonemap.app.map.FeatureDataStore
import com.carryzonemap.app.map.FeatureLayerManager
import com.carryzonemap.app.ui.viewmodel.MapViewModel
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import timber.log.Timber

/**
 * Handles map click events using Chain of Responsibility pattern.
 * Each detector checks if it can handle the click, and if so, processes it.
 */
class FeatureClickHandler(private val viewModel: MapViewModel) {

    private val detectors: List<FeatureDetector> = listOf(
        ExistingPinDetector(viewModel),
        OverpassPoiDetector(viewModel),
        MapTilerPoiDetector(viewModel)
    )

    /**
     * Handles a map click event by delegating to registered feature detectors.
     * @return true if the click was handled, false otherwise
     */
    fun handleClick(map: MapLibreMap, clickPoint: LatLng): Boolean {
        val screenPoint = map.projection.toScreenLocation(clickPoint)

        for (detector in detectors) {
            if (detector.canHandle(map, screenPoint, clickPoint)) {
                detector.handle(map, screenPoint, clickPoint)
                return true
            }
        }

        // No detector handled the click
        return false
    }
}

/**
 * Interface for detecting and handling different types of map features.
 * Implements the Chain of Responsibility pattern.
 */
interface FeatureDetector {
    /**
     * Checks if this detector can handle the clicked feature.
     */
    fun canHandle(map: MapLibreMap, screenPoint: android.graphics.PointF, clickPoint: LatLng): Boolean

    /**
     * Handles the clicked feature.
     */
    fun handle(map: MapLibreMap, screenPoint: android.graphics.PointF, clickPoint: LatLng)
}

/**
 * Detects clicks on existing user pins.
 */
class ExistingPinDetector(private val viewModel: MapViewModel) : FeatureDetector {
    override fun canHandle(map: MapLibreMap, screenPoint: android.graphics.PointF, clickPoint: LatLng): Boolean {
        val features = map.queryRenderedFeatures(screenPoint, FeatureLayerManager.USER_PINS_LAYER_ID)
        return features.isNotEmpty()
    }

    override fun handle(map: MapLibreMap, screenPoint: android.graphics.PointF, clickPoint: LatLng) {
        val features = map.queryRenderedFeatures(screenPoint, FeatureLayerManager.USER_PINS_LAYER_ID)
        val pinFeature = features.firstOrNull() ?: return

        val pinId = pinFeature.getStringProperty(FeatureDataStore.PROPERTY_FEATURE_ID)
        if (pinId != null) {
            viewModel.showEditPinDialog(pinId)
            Timber.d("User clicked existing pin: $pinId")
        }
    }
}

/**
 * Detects clicks on Overpass POI layer (our custom POI layer).
 */
class OverpassPoiDetector(private val viewModel: MapViewModel) : FeatureDetector {
    override fun canHandle(map: MapLibreMap, screenPoint: android.graphics.PointF, clickPoint: LatLng): Boolean {
        val features = map.queryRenderedFeatures(screenPoint, MapConstants.POI_LAYER_ID)
        return features.isNotEmpty()
    }

    override fun handle(map: MapLibreMap, screenPoint: android.graphics.PointF, clickPoint: LatLng) {
        val features = map.queryRenderedFeatures(screenPoint, MapConstants.POI_LAYER_ID)
        val poiFeature = features.firstOrNull() ?: return

        val poiName = poiFeature.getStringProperty(MapConstants.PROPERTY_NAME)
            ?: MapConstants.UiText.UNKNOWN_POI

        viewModel.showCreatePinDialog(poiName, clickPoint.longitude, clickPoint.latitude)
        Timber.d("User clicked Overpass POI: $poiName")
    }
}

/**
 * Detects clicks on MapTiler base map POIs (labels from the base map style).
 */
class MapTilerPoiDetector(private val viewModel: MapViewModel) : FeatureDetector {
    override fun canHandle(map: MapLibreMap, screenPoint: android.graphics.PointF, clickPoint: LatLng): Boolean {
        val allFeatures = map.queryRenderedFeatures(screenPoint)

        // Look for any feature with a name property
        return allFeatures.any { feature ->
            feature.hasProperty(MapConstants.PROPERTY_NAME) &&
                !feature.getStringProperty(MapConstants.PROPERTY_NAME).isNullOrBlank()
        }
    }

    override fun handle(map: MapLibreMap, screenPoint: android.graphics.PointF, clickPoint: LatLng) {
        val allFeatures = map.queryRenderedFeatures(screenPoint)

        // Log for debugging
        Timber.d("Clicked features count: ${allFeatures.size}")
        allFeatures.take(5).forEach { feature ->
            Timber.d("Feature properties: ${feature.properties()}")
        }

        // Find first feature with a valid name
        val poiFeature = allFeatures.firstOrNull { feature ->
            feature.hasProperty(MapConstants.PROPERTY_NAME) &&
                !feature.getStringProperty(MapConstants.PROPERTY_NAME).isNullOrBlank()
        } ?: return

        // Try multiple name property variants
        val poiName = poiFeature.getStringProperty(MapConstants.PROPERTY_NAME)
            ?: poiFeature.getStringProperty(MapConstants.PROPERTY_NAME_EN)
            ?: poiFeature.getStringProperty(MapConstants.PROPERTY_NAME_EN_UNDERSCORE)

        if (!poiName.isNullOrBlank()) {
            Timber.d("Found MapTiler POI: $poiName")
            viewModel.showCreatePinDialog(poiName, clickPoint.longitude, clickPoint.latitude)
        }
    }
}
