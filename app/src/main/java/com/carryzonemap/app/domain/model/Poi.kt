package com.carryzonemap.app.domain.model

/**
 * Represents a Point of Interest (POI) from OpenStreetMap.
 *
 * POIs include businesses, amenities, and other notable locations.
 */
data class Poi(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    // e.g., "restaurant", "bar", "shop", "cafe"
    val type: String,
    // Additional OSM tags
    val tags: Map<String, String> = emptyMap(),
)
