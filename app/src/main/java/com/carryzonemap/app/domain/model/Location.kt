package com.carryzonemap.app.domain.model

/**
 * Represents a geographic location with latitude and longitude coordinates.
 *
 * @property latitude The latitude coordinate in decimal degrees (-90 to 90)
 * @property longitude The longitude coordinate in decimal degrees (-180 to 180)
 */
data class Location(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }

    companion object {
        /**
         * Creates a Location from longitude and latitude (reversed order, MapLibre convention)
         */
        fun fromLngLat(longitude: Double, latitude: Double): Location {
            return Location(latitude, longitude)
        }
    }
}
