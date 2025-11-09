package com.carryzonemap.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a location pin stored in the local database.
 *
 * @property id Unique identifier for the pin
 * @property name Name of the POI (Point of Interest) this pin belongs to
 * @property longitude Longitude coordinate
 * @property latitude Latitude coordinate
 * @property status Status code (0=ALLOWED, 1=UNCERTAIN, 2=NO_GUN)
 * @property photoUri Optional URI to a photo
 * @property notes User notes about the location
 * @property votes Vote count for moderation
 * @property createdBy User ID who created the pin
 * @property createdAt Timestamp when created (milliseconds since epoch)
 * @property lastModified Timestamp when last modified
 * @property restrictionTag Reason for restriction (enum name, required if status is NO_GUN)
 * @property hasSecurityScreening Whether active security screening is present
 * @property hasPostedSignage Whether posted "no guns" signage is visible
 */
@Entity(tableName = "pins")
data class PinEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val longitude: Double,
    val latitude: Double,
    val status: Int,
    val photoUri: String? = null,
    val notes: String? = null,
    val votes: Int = 0,
    val createdBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val restrictionTag: String? = null,
    val hasSecurityScreening: Boolean = false,
    val hasPostedSignage: Boolean = false,
)
