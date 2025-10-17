package com.carryzonemap.app.domain.model

import java.util.UUID

/**
 * Domain model representing a location pin on the map.
 *
 * @property id Unique identifier for the pin
 * @property name Name of the POI (Point of Interest) this pin belongs to
 * @property location Geographic coordinates of the pin
 * @property status Current carry zone status (Allowed, Uncertain, No Gun)
 * @property metadata Additional information about the pin
 */
data class Pin(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val location: Location,
    val status: PinStatus = PinStatus.ALLOWED,
    val metadata: PinMetadata = PinMetadata(),
) {
    /**
     * Creates a new Pin with updated status (cycles through statuses)
     */
    fun withNextStatus(): Pin =
        copy(
            status = status.next(),
            metadata = metadata.copy(lastModified = System.currentTimeMillis()),
        )

    /**
     * Creates a new Pin with updated status
     */
    fun withStatus(newStatus: PinStatus): Pin =
        copy(
            status = newStatus,
            metadata = metadata.copy(lastModified = System.currentTimeMillis()),
        )

    /**
     * Creates a new Pin with updated metadata
     */
    fun withMetadata(newMetadata: PinMetadata): Pin =
        copy(
            metadata = newMetadata.copy(lastModified = System.currentTimeMillis()),
        )

    companion object {
        /**
         * Creates a Pin from longitude and latitude coordinates
         */
        fun fromLngLat(
            name: String,
            longitude: Double,
            latitude: Double,
            status: PinStatus = PinStatus.ALLOWED,
        ): Pin {
            return Pin(
                name = name,
                location = Location.fromLngLat(longitude, latitude),
                status = status,
            )
        }
    }
}
