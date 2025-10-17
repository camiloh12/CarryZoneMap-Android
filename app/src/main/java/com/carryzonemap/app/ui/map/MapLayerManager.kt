package com.carryzonemap.app.ui.map

import com.carryzonemap.app.domain.model.Poi
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import timber.log.Timber

/**
 * Manages map layers (POI labels, pins, etc.).
 * Single Responsibility: Layer creation and updates.
 */
class MapLayerManager {

    /**
     * Adds the POI layer to the map for displaying business names and amenities.
     */
    fun addPoiLayer(style: Style) {
        // Add POI source
        val poiSource = GeoJsonSource(
            MapConstants.POI_SOURCE_ID,
            FeatureCollection.fromFeatures(emptyList())
        )
        style.addSource(poiSource)
        Timber.d("Added POI source: ${MapConstants.POI_SOURCE_ID}")

        // Add POI label layer
        val poiLayer = SymbolLayer(MapConstants.POI_LAYER_ID, MapConstants.POI_SOURCE_ID)
            .withProperties(
                PropertyFactory.textField("{${MapConstants.PROPERTY_NAME}}"),
                PropertyFactory.textSize(MapConstants.POI_TEXT_SIZE),
                PropertyFactory.textColor(MapConstants.POI_TEXT_COLOR),
                PropertyFactory.textHaloColor(MapConstants.POI_TEXT_HALO_COLOR),
                PropertyFactory.textHaloWidth(MapConstants.POI_TEXT_HALO_WIDTH),
                PropertyFactory.textOffset(MapConstants.POI_TEXT_OFFSET),
                PropertyFactory.textAnchor(MapConstants.POI_TEXT_ANCHOR),
                PropertyFactory.textAllowOverlap(false),
                PropertyFactory.textIgnorePlacement(false)
            )
        style.addLayer(poiLayer)
        Timber.d("Added POI layer: ${MapConstants.POI_LAYER_ID}")
    }

    /**
     * Updates the POI layer with new POI data.
     */
    fun updatePoiLayer(style: Style, pois: List<Poi>) {
        val source = style.getSourceAs<GeoJsonSource>(MapConstants.POI_SOURCE_ID)
        if (source != null) {
            val features = pois.map { poi ->
                Feature.fromGeometry(
                    Point.fromLngLat(poi.longitude, poi.latitude)
                ).apply {
                    addStringProperty(MapConstants.PROPERTY_NAME, poi.name)
                    addStringProperty(MapConstants.PROPERTY_TYPE, poi.type)
                }
            }
            source.setGeoJson(FeatureCollection.fromFeatures(features))
            Timber.d("Updated POI layer with ${pois.size} POIs")
        } else {
            Timber.w("POI source not found, cannot update")
        }
    }
}
