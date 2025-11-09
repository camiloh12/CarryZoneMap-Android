package com.carryzonemap.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Pin entities from Supabase.
 *
 * This class represents the structure of pin data as stored in the Supabase
 * PostgreSQL database. Field names use snake_case to match the database schema.
 *
 * @property id Unique identifier (UUID)
 * @property name Name of the POI (Point of Interest) this pin belongs to
 * @property longitude Longitude coordinate
 * @property latitude Latitude coordinate
 * @property status Status code (0=ALLOWED, 1=UNCERTAIN, 2=NO_GUN)
 * @property photoUri Optional URI to a photo in Supabase Storage
 * @property notes User notes about the location
 * @property votes Vote count for accuracy/moderation
 * @property createdBy User ID who created the pin
 * @property createdAt Timestamp when created (ISO 8601 format)
 * @property lastModified Timestamp when last modified (ISO 8601 format)
 * @property restrictionTag Reason for restriction (enum name, required if status is NO_GUN)
 * @property hasSecurityScreening Whether active security screening is present
 * @property hasPostedSignage Whether posted "no guns" signage is visible
 */
@Serializable
data class SupabasePinDto(
    val id: String,
    val name: String,
    val longitude: Double,
    val latitude: Double,
    val status: Int,
    @SerialName("photo_uri")
    val photoUri: String? = null,
    val notes: String? = null,
    val votes: Int = 0,
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("last_modified")
    val lastModified: String,
    @SerialName("restriction_tag")
    val restrictionTag: String? = null,
    @SerialName("has_security_screening")
    val hasSecurityScreening: Boolean = false,
    @SerialName("has_posted_signage")
    val hasPostedSignage: Boolean = false,
)
