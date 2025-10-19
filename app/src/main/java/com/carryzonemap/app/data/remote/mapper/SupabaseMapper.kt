package com.carryzonemap.app.data.remote.mapper

import com.carryzonemap.app.data.remote.dto.SupabasePinDto
import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.model.PinMetadata
import com.carryzonemap.app.domain.model.PinStatus
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Mapper object for converting between Supabase DTOs and domain models.
 *
 * Handles conversion of:
 * - Timestamp formats (ISO 8601 ↔ milliseconds since epoch)
 * - Status codes (Int ↔ PinStatus enum)
 * - Nested structures (DTO ↔ Pin + Location + PinMetadata)
 */
object SupabaseMapper {
    /**
     * Converts a SupabasePinDto to a domain Pin model.
     */
    fun SupabasePinDto.toDomain(): Pin {
        return Pin(
            id = id,
            name = name,
            location = Location.fromLngLat(longitude, latitude),
            status = PinStatus.fromColorCode(status),
            metadata =
                PinMetadata(
                    photoUri = photoUri,
                    notes = notes,
                    votes = votes,
                    createdBy = createdBy,
                    createdAt = parseTimestamp(createdAt),
                    lastModified = parseTimestamp(lastModified),
                ),
        )
    }

    /**
     * Converts a domain Pin to a SupabasePinDto.
     */
    fun Pin.toSupabaseDto(): SupabasePinDto {
        return SupabasePinDto(
            id = id,
            name = name,
            longitude = location.longitude,
            latitude = location.latitude,
            status = status.colorCode,
            photoUri = metadata.photoUri,
            notes = metadata.notes,
            votes = metadata.votes,
            createdBy = metadata.createdBy,
            createdAt = formatTimestamp(metadata.createdAt),
            lastModified = formatTimestamp(metadata.lastModified),
        )
    }

    /**
     * Converts a list of SupabasePinDto to domain Pin models.
     */
    fun List<SupabasePinDto>.toDomainModels(): List<Pin> {
        return map { it.toDomain() }
    }

    /**
     * Converts a list of domain Pins to SupabasePinDto.
     */
    fun List<Pin>.toSupabaseDtos(): List<SupabasePinDto> {
        return map { it.toSupabaseDto() }
    }

    /**
     * Parses an ISO 8601 timestamp string to milliseconds since epoch.
     *
     * Example: "2023-10-15T12:30:45.123456Z" → 1697371845123
     */
    private fun parseTimestamp(isoTimestamp: String): Long {
        return try {
            val instant = Instant.parse(isoTimestamp)
            instant.toEpochMilli()
        } catch (e: Exception) {
            // Log the error and fallback to current time if parsing fails
            timber.log.Timber.w(e, "Failed to parse timestamp: $isoTimestamp")
            System.currentTimeMillis()
        }
    }

    /**
     * Formats milliseconds since epoch to ISO 8601 timestamp string.
     *
     * Example: 1697371845123 → "2023-10-15T12:30:45.123Z"
     */
    private fun formatTimestamp(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        return DateTimeFormatter.ISO_INSTANT.format(instant)
    }
}
