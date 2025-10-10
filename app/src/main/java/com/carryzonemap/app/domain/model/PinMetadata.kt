package com.carryzonemap.app.domain.model

/**
 * Additional metadata associated with a location pin.
 *
 * @property photoUri Optional URI to a photo of the location
 * @property notes User notes about the location
 * @property votes Vote count for this pin (for future moderation feature)
 * @property createdBy User ID who created the pin (for future authentication)
 * @property createdAt Timestamp when the pin was created (milliseconds since epoch)
 * @property lastModified Timestamp when the pin was last modified
 */
data class PinMetadata(
    val photoUri: String? = null,
    val notes: String? = null,
    val votes: Int = 0,
    val createdBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
