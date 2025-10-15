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
    val type: String, // e.g., "restaurant", "bar", "shop", "cafe"
    val tags: Map<String, String> = emptyMap() // Additional OSM tags
)
