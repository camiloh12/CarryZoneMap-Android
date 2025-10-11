package com.carryzonemap.app.map

import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.match
import org.maplibre.android.style.expressions.Expression.stop
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection

class FeatureLayerManager {
    companion object {
        // IDs for map elements
        const val USER_PINS_SOURCE_ID = "user-pins-source"
        const val USER_PINS_LAYER_ID = "user-pins-layer"

        // Visual Properties (could be configurable if needed)
        private const val COLOR_GREEN_HEX = "#2E7D32"
        private const val COLOR_YELLOW_HEX = "#FBC02D"
        private const val COLOR_RED_HEX = "#D32F2F"
    }

    fun addSourceAndLayer(
        style: Style,
        initialFeatures: List<Feature>,
    ) {
        val source = GeoJsonSource(USER_PINS_SOURCE_ID, FeatureCollection.fromFeatures(initialFeatures.toTypedArray()))
        style.addSource(source)

        val layer =
            CircleLayer(USER_PINS_LAYER_ID, USER_PINS_SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(8f),
                PropertyFactory.circleColor(
                    match(
                        get(FeatureDataStore.PROPERTY_COLOR_STATE), // Uses constant from FeatureDataStore
                        literal(COLOR_GREEN_HEX), // Default color
                        stop(FeatureDataStore.COLOR_STATE_GREEN, literal(COLOR_GREEN_HEX)),
                        stop(FeatureDataStore.COLOR_STATE_YELLOW, literal(COLOR_YELLOW_HEX)),
                        stop(FeatureDataStore.COLOR_STATE_RED, literal(COLOR_RED_HEX)),
                    ),
                ),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
                PropertyFactory.circleStrokeWidth(2f),
            )
        style.addLayer(layer)
    }

    fun updateDataSource(
        style: Style,
        currentFeatures: List<Feature>,
    ) {
        style.getSourceAs<GeoJsonSource>(USER_PINS_SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeatures(currentFeatures.toTypedArray()))
    }
}
