package com.carryzonemap.app.map

import com.carryzonemap.app.ui.map.MapConstants
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

/**
 * Manages the MapLibre layer for user-created pins.
 * Handles adding the source/layer and updating pin features on the map.
 */
class FeatureLayerManager {
    companion object {
        // Visual Properties (could be configurable if needed)
        private const val COLOR_GREEN_HEX = "#2E7D32"
        private const val COLOR_YELLOW_HEX = "#FBC02D"
        private const val COLOR_RED_HEX = "#D32F2F"
    }

    fun addSourceAndLayer(
        style: Style,
        initialFeatures: List<Feature>,
    ) {
        val source = GeoJsonSource(
            MapConstants.USER_PINS_SOURCE_ID,
            FeatureCollection.fromFeatures(initialFeatures.toTypedArray())
        )
        style.addSource(source)

        val layer =
            CircleLayer(MapConstants.USER_PINS_LAYER_ID, MapConstants.USER_PINS_SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(8f),
                PropertyFactory.circleColor(
                    match(
                        get(MapConstants.PROPERTY_COLOR_STATE),
                        // Default color
                        literal(COLOR_GREEN_HEX),
                        stop(MapConstants.COLOR_STATE_GREEN, literal(COLOR_GREEN_HEX)),
                        stop(MapConstants.COLOR_STATE_YELLOW, literal(COLOR_YELLOW_HEX)),
                        stop(MapConstants.COLOR_STATE_RED, literal(COLOR_RED_HEX)),
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
        style.getSourceAs<GeoJsonSource>(MapConstants.USER_PINS_SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeatures(currentFeatures.toTypedArray()))
    }
}
