package com.carryzonemap.app.domain.mapper

import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.model.PinMetadata
import com.carryzonemap.app.domain.model.PinStatus
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

/**
 * Mapper object for converting between domain models and MapLibre Feature objects.
 */
object PinMapper {

    private const val PROPERTY_FEATURE_ID = "feature_id"
    private const val PROPERTY_COLOR_STATE = "color_state"
    private const val PROPERTY_PHOTO_URI = "photo_uri"
    private const val PROPERTY_NOTES = "notes"
    private const val PROPERTY_CREATED_AT = "created_at"

    /**
     * Converts a domain Pin model to a MapLibre Feature for map rendering.
     */
    fun Pin.toFeature(): Feature {
        val point = Point.fromLngLat(location.longitude, location.latitude)
        val feature = Feature.fromGeometry(point)

        feature.addStringProperty(PROPERTY_FEATURE_ID, id)
        feature.addNumberProperty(PROPERTY_COLOR_STATE, status.colorCode)

        metadata.photoUri?.let {
            feature.addStringProperty(PROPERTY_PHOTO_URI, it)
        }
        metadata.notes?.let {
            feature.addStringProperty(PROPERTY_NOTES, it)
        }
        feature.addNumberProperty(PROPERTY_CREATED_AT, metadata.createdAt)

        return feature
    }

    /**
     * Converts a MapLibre Feature to a domain Pin model.
     * Returns null if the feature cannot be converted (missing required data).
     */
    fun Feature.toPin(): Pin? {
        val point = geometry() as? Point ?: return null
        val id = getStringProperty(PROPERTY_FEATURE_ID) ?: return null
        val colorCode = getNumberProperty(PROPERTY_COLOR_STATE)?.toInt() ?: 0

        val location = Location(
            latitude = point.latitude(),
            longitude = point.longitude()
        )

        val status = PinStatus.fromColorCode(colorCode)

        val metadata = PinMetadata(
            photoUri = getStringProperty(PROPERTY_PHOTO_URI),
            notes = getStringProperty(PROPERTY_NOTES),
            createdAt = getNumberProperty(PROPERTY_CREATED_AT)?.toLong()
                ?: System.currentTimeMillis()
        )

        return Pin(
            id = id,
            location = location,
            status = status,
            metadata = metadata
        )
    }

    /**
     * Converts a list of domain Pins to MapLibre Features.
     */
    fun List<Pin>.toFeatures(): List<Feature> = map { it.toFeature() }

    /**
     * Converts a list of MapLibre Features to domain Pins.
     * Filters out any features that cannot be converted.
     */
    fun List<Feature>.toPins(): List<Pin> = mapNotNull { it.toPin() }
}
