package com.carryzonemap.app.util

import android.content.Context
import org.maplibre.geojson.Feature
import org.maplibre.geojson.MultiPolygon
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import timber.log.Timber

/**
 * Generates a visual overlay to gray out regions outside the US boundaries.
 * Uses actual US boundary coordinates for accurate, smooth borders.
 */
object UsOverlayGenerator {

    /**
     * Generates an overlay using actual US boundary data from GeoJSON.
     * This creates smooth borders that follow the real US geography.
     *
     * @param context Android context for loading assets
     * @return Feature with world polygon minus US boundary, or fallback simplified version
     */
    fun generateAccurateNonUsOverlay(context: Context): Feature {
        try {
            // Load US boundary from assets
            Timber.d("Loading US boundary GeoJSON from assets...")
            val usBoundaryFeature = GeoJsonLoader.loadFirstFeatureFromAssets(
                context,
                "us_boundary_simplified.geojson"
            )

            val geometry = usBoundaryFeature?.geometry()
            if (geometry !is MultiPolygon) {
                val reason = if (usBoundaryFeature == null) {
                    "Failed to load US boundary"
                } else {
                    "US boundary is not a MultiPolygon"
                }
                Timber.w("$reason, falling back to simplified overlay")
                return generateSimplifiedNonUsOverlay()
            }

            Timber.d("US boundary loaded successfully with ${geometry.coordinates().size} polygons")

            // Create world polygon
            val worldRing = listOf(
                Point.fromLngLat(-180.0, -90.0),
                Point.fromLngLat(180.0, -90.0),
                Point.fromLngLat(180.0, 90.0),
                Point.fromLngLat(-180.0, 90.0),
                Point.fromLngLat(-180.0, -90.0)
            )

            // Extract all US polygon rings as holes (need to reverse winding for holes)
            val usHoles = geometry.coordinates().flatMap { polygon ->
                polygon.map { ring ->
                    ring.reversed() // Reverse winding order for holes
                }
            }

            // Combine world ring with US holes
            val allRings = listOf(worldRing) + usHoles
            val overlayPolygon = Polygon.fromLngLats(allRings)

            Timber.d("Created overlay polygon with ${allRings.size} rings (1 world + ${usHoles.size} US holes)")
            return Feature.fromGeometry(overlayPolygon)

        } catch (e: Exception) {
            Timber.e(e, "Failed to generate accurate overlay, falling back to simplified version")
            return generateSimplifiedNonUsOverlay()
        }
    }

    /**
     * Generates a simple test overlay that covers the entire world (no holes).
     * Use this for testing to verify the overlay rendering works.
     */
    fun generateTestOverlay(): Feature {
        val worldRing = listOf(
            Point.fromLngLat(-180.0, -90.0),
            Point.fromLngLat(180.0, -90.0),
            Point.fromLngLat(180.0, 90.0),
            Point.fromLngLat(-180.0, 90.0),
            Point.fromLngLat(-180.0, -90.0)
        )
        val polygon = Polygon.fromLngLats(listOf(worldRing))
        return Feature.fromGeometry(polygon)
    }

    /**
     * Generates a GeoJSON Feature that represents the world minus US boundaries.
     * This creates a polygon that covers the entire world with holes for each US state.
     *
     * @return A MapLibre Feature that can be added as a fill layer
     */
    fun generateNonUsOverlay(): Feature {
        // Outer ring: covers the entire world (clockwise for exterior ring)
        val worldRing = listOf(
            Point.fromLngLat(-180.0, -90.0),
            Point.fromLngLat(180.0, -90.0),
            Point.fromLngLat(180.0, 90.0),
            Point.fromLngLat(-180.0, 90.0),
            Point.fromLngLat(-180.0, -90.0) // Close the ring
        )

        // Inner rings: holes for each US state bounding box (counter-clockwise for holes)
        val stateHoles = UsBoundaryValidator.US_STATE_BOUNDING_BOXES.map { bbox ->
            listOf(
                Point.fromLngLat(bbox.minLng, bbox.minLat),
                Point.fromLngLat(bbox.minLng, bbox.maxLat),  // Counter-clockwise
                Point.fromLngLat(bbox.maxLng, bbox.maxLat),
                Point.fromLngLat(bbox.maxLng, bbox.minLat),
                Point.fromLngLat(bbox.minLng, bbox.minLat) // Close the ring
            )
        }

        // Create polygon with world ring and state holes
        val rings = listOf(worldRing) + stateHoles
        val polygon = Polygon.fromLngLats(rings)

        return Feature.fromGeometry(polygon)
    }

    /**
     * Generates a simplified continental US overlay (faster rendering).
     * Only cuts out a single rectangle around the continental US.
     */
    fun generateSimplifiedNonUsOverlay(): Feature {
        val continentalBox = UsBoundaryValidator.getContinentalUsBoundingBox()

        // Outer ring: covers the entire world (clockwise)
        val worldRing = listOf(
            Point.fromLngLat(-180.0, -90.0),
            Point.fromLngLat(180.0, -90.0),
            Point.fromLngLat(180.0, 90.0),
            Point.fromLngLat(-180.0, 90.0),
            Point.fromLngLat(-180.0, -90.0)
        )

        // Single hole for continental US (counter-clockwise)
        val continentalHole = listOf(
            Point.fromLngLat(continentalBox.minLng, continentalBox.minLat),
            Point.fromLngLat(continentalBox.minLng, continentalBox.maxLat),
            Point.fromLngLat(continentalBox.maxLng, continentalBox.maxLat),
            Point.fromLngLat(continentalBox.maxLng, continentalBox.minLat),
            Point.fromLngLat(continentalBox.minLng, continentalBox.minLat)
        )

        val polygon = Polygon.fromLngLats(listOf(worldRing, continentalHole))

        return Feature.fromGeometry(polygon)
    }
}
