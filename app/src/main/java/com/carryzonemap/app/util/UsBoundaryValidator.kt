package com.carryzonemap.app.util

/**
 * Validates if geographic coordinates are within the 50 US states and Washington DC.
 * Uses bounding boxes for each state for fast validation.
 */
object UsBoundaryValidator {

    /**
     * Data class representing a geographic bounding box.
     */
    data class BoundingBox(
        val minLat: Double,
        val maxLat: Double,
        val minLng: Double,
        val maxLng: Double
    ) {
        /**
         * Checks if a point is within this bounding box.
         */
        fun contains(latitude: Double, longitude: Double): Boolean {
            return latitude in minLat..maxLat && longitude in minLng..maxLng
        }
    }

    /**
     * Bounding boxes for all 50 US states and Washington DC.
     * Source: US Census Bureau and USGS
     */
    internal val US_STATE_BOUNDING_BOXES = listOf(
        // Continental US States
        BoundingBox(30.2, 35.0, -88.5, -84.9),      // Alabama
        BoundingBox(51.2, 71.5, -179.2, -129.0),    // Alaska
        BoundingBox(31.3, 37.0, -114.8, -109.0),    // Arizona
        BoundingBox(33.0, 36.5, -94.6, -89.6),      // Arkansas
        BoundingBox(32.5, 42.0, -124.5, -114.1),    // California
        BoundingBox(37.0, 41.0, -109.1, -102.0),    // Colorado
        BoundingBox(40.9, 42.1, -73.7, -71.8),      // Connecticut
        BoundingBox(38.4, 39.8, -75.8, -75.0),      // Delaware
        BoundingBox(24.5, 31.0, -87.6, -80.0),      // Florida
        BoundingBox(30.4, 35.0, -85.6, -80.8),      // Georgia
        BoundingBox(18.9, 22.3, -160.3, -154.8),    // Hawaii
        BoundingBox(42.0, 49.0, -117.2, -111.0),    // Idaho
        BoundingBox(37.0, 42.5, -91.5, -87.0),      // Illinois
        BoundingBox(37.8, 41.8, -88.1, -84.8),      // Indiana
        BoundingBox(40.4, 43.5, -96.6, -90.1),      // Iowa
        BoundingBox(37.0, 40.0, -102.1, -94.6),     // Kansas
        BoundingBox(36.5, 39.1, -89.6, -81.9),      // Kentucky
        BoundingBox(28.9, 33.0, -94.0, -88.8),      // Louisiana
        BoundingBox(43.1, 47.5, -71.1, -66.9),      // Maine
        BoundingBox(37.9, 39.7, -79.5, -75.0),      // Maryland
        BoundingBox(41.2, 42.9, -73.5, -69.9),      // Massachusetts
        BoundingBox(41.7, 48.3, -90.4, -82.4),      // Michigan
        BoundingBox(43.5, 49.4, -97.2, -89.5),      // Minnesota
        BoundingBox(30.2, 35.0, -91.7, -88.1),      // Mississippi
        BoundingBox(36.0, 40.6, -95.8, -89.1),      // Missouri
        BoundingBox(44.4, 49.0, -116.1, -104.0),    // Montana
        BoundingBox(40.0, 43.0, -104.1, -95.3),     // Nebraska
        BoundingBox(35.0, 42.0, -120.0, -114.0),    // Nevada
        BoundingBox(42.7, 45.3, -72.6, -70.6),      // New Hampshire
        BoundingBox(38.9, 41.4, -75.6, -73.9),      // New Jersey
        BoundingBox(31.3, 37.0, -109.1, -103.0),    // New Mexico
        BoundingBox(40.5, 45.0, -79.8, -71.8),      // New York
        BoundingBox(33.8, 36.6, -84.3, -75.4),      // North Carolina
        BoundingBox(45.9, 49.0, -104.1, -96.6),     // North Dakota
        BoundingBox(38.4, 42.3, -84.8, -80.5),      // Ohio
        BoundingBox(33.6, 37.0, -103.0, -94.4),     // Oklahoma
        BoundingBox(42.0, 46.3, -124.6, -116.5),    // Oregon
        BoundingBox(39.7, 42.3, -80.5, -74.7),      // Pennsylvania
        BoundingBox(41.1, 42.0, -71.9, -71.1),      // Rhode Island
        BoundingBox(32.0, 35.2, -83.4, -78.5),      // South Carolina
        BoundingBox(42.5, 45.9, -104.1, -96.4),     // South Dakota
        BoundingBox(35.0, 36.7, -90.3, -81.6),      // Tennessee
        BoundingBox(25.8, 36.5, -106.7, -93.5),     // Texas
        BoundingBox(37.0, 42.0, -114.1, -109.0),    // Utah
        BoundingBox(42.7, 45.0, -73.4, -71.5),      // Vermont
        BoundingBox(36.5, 39.5, -83.7, -75.2),      // Virginia
        BoundingBox(45.5, 49.0, -124.8, -116.9),    // Washington
        BoundingBox(37.2, 40.6, -82.6, -77.7),      // West Virginia
        BoundingBox(42.5, 47.1, -92.9, -86.2),      // Wisconsin
        BoundingBox(41.0, 45.0, -111.1, -104.0),    // Wyoming
        // Washington DC
        BoundingBox(38.8, 39.0, -77.1, -76.9)       // District of Columbia
    )

    /**
     * Checks if the given coordinates are within the 50 US states or Washington DC.
     *
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return true if the coordinates are within US boundaries, false otherwise
     */
    fun isWithinUsBoundaries(latitude: Double, longitude: Double): Boolean {
        return US_STATE_BOUNDING_BOXES.any { bbox ->
            bbox.contains(latitude, longitude)
        }
    }

    /**
     * Gets the overall bounding box that encompasses all 50 US states and DC.
     * Useful for initial map bounds.
     */
    fun getOverallUsBoundingBox(): BoundingBox {
        return BoundingBox(
            minLat = 18.9,   // Southern tip of Hawaii
            maxLat = 71.5,   // Northern tip of Alaska
            minLng = -179.2, // Western tip of Alaska (Aleutian Islands)
            maxLng = -66.9   // Eastern tip of Maine
        )
    }

    /**
     * Gets a continental US bounding box (excludes Alaska and Hawaii).
     * Useful for default map view.
     */
    fun getContinentalUsBoundingBox(): BoundingBox {
        return BoundingBox(
            minLat = 24.5,   // Southern tip of Florida
            maxLat = 49.4,   // Northern border (Minnesota/North Dakota)
            minLng = -124.8, // Western coast (Washington)
            maxLng = -66.9   // Eastern coast (Maine)
        )
    }
}
