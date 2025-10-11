package com.carryzonemap.app.map

import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.geojson.Feature // For type hint of map.queryRenderedFeatures

class MapInteractionHandler(
    private val map: MapLibreMap,
    private val featureDataStore: FeatureDataStore,
) {
    fun setupListeners() {
        setupMapLongClickListener()
        setupMapClickListener()
    }

    private fun setupMapLongClickListener() {
        map.addOnMapLongClickListener { latLng ->
            featureDataStore.addFeature(latLng.longitude, latLng.latitude)
            // The onDataChanged callback in FeatureDataStore will notify for UI update
            true // Consume event
        }
    }

    private fun setupMapClickListener() {
        map.addOnMapClickListener { point ->
            handleFeatureClickInteraction(point)
            true // Consume event
        }
    }

    private fun handleFeatureClickInteraction(point: LatLng) {
        val clickedMapFeature = getClickedMapFeature(point) ?: return
        val featureId = clickedMapFeature.getStringProperty(FeatureDataStore.PROPERTY_FEATURE_ID) ?: return

        featureDataStore.cycleFeatureColorState(featureId)
        // The onDataChanged callback in FeatureDataStore will notify for UI update
    }

    private fun getClickedMapFeature(point: LatLng): Feature? {
        val screenPoint = map.projection.toScreenLocation(point)
        // Use the layer ID from FeatureLayerManager for querying
        val features = map.queryRenderedFeatures(screenPoint, FeatureLayerManager.USER_PINS_LAYER_ID)
        return features.firstOrNull()
    }
}
