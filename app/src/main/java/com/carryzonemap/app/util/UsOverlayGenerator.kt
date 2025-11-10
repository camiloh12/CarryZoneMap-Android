package com.carryzonemap.app.util

import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

/**
 * Generates a visual overlay to gray out regions outside the US boundaries.
 * Creates a world-covering polygon with cutouts for US state bounding boxes.
 */
object UsOverlayGenerator {

    /**
     * Generates a GeoJSON Feature that represents the world minus US boundaries.
     * This creates a polygon that covers the entire world with holes for each US state.
     *
     * @return A MapLibre Feature that can be added as a fill layer
     */
    fun generateNonUsOverlay(): Feature {
        // Outer ring: covers the entire world
        val worldRing = listOf(
            Point.fromLngLat(-180.0, -90.0),
            Point.fromLngLat(180.0, -90.0),
            Point.fromLngLat(180.0, 90.0),
            Point.fromLngLat(-180.0, 90.0),
            Point.fromLngLat(-180.0, -90.0) // Close the ring
        )

        // Inner rings: holes for each US state bounding box
        val stateHoles = UsBoundaryValidator.US_STATE_BOUNDING_BOXES.map { bbox ->
            listOf(
                Point.fromLngLat(bbox.minLng, bbox.minLat),
                Point.fromLngLat(bbox.maxLng, bbox.minLat),
                Point.fromLngLat(bbox.maxLng, bbox.maxLat),
                Point.fromLngLat(bbox.minLng, bbox.maxLat),
                Point.fromLngLat(bbox.minLng, bbox.minLat) // Close the ring
            )
        }

        // Create polygon with world ring and state holes
        val rings = listOf(worldRing) + stateHoles
        val polygon = Polygon.fromLngLats(rings.map { LineString.fromLngLats(it).coordinates() })

        return Feature.fromGeometry(polygon)
    }

    /**
     * Generates a simplified continental US overlay (faster rendering).
     * Only cuts out a single rectangle around the continental US.
     */
    fun generateSimplifiedNonUsOverlay(): Feature {
        val continentalBox = UsBoundaryValidator.getContinentalUsBoundingBox()

        // Outer ring: covers the entire world
        val worldRing = listOf(
            Point.fromLngLat(-180.0, -90.0),
            Point.fromLngLat(180.0, -90.0),
            Point.fromLngLat(180.0, 90.0),
            Point.fromLngLat(-180.0, 90.0),
            Point.fromLngLat(-180.0, -90.0)
        )

        // Single hole for continental US
        val continentalHole = listOf(
            Point.fromLngLat(continentalBox.minLng, continentalBox.minLat),
            Point.fromLngLat(continentalBox.maxLng, continentalBox.minLat),
            Point.fromLngLat(continentalBox.maxLng, continentalBox.maxLat),
            Point.fromLngLat(continentalBox.minLng, continentalBox.maxLat),
            Point.fromLngLat(continentalBox.minLng, continentalBox.minLat)
        )

        val polygon = Polygon.fromLngLats(
            listOf(
                LineString.fromLngLats(worldRing).coordinates(),
                LineString.fromLngLats(continentalHole).coordinates()
            )
        )

        return Feature.fromGeometry(polygon)
    }
}
