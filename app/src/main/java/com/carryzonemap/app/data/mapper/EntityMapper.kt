package com.carryzonemap.app.data.mapper

import com.carryzonemap.app.data.local.entity.PinEntity
import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.model.PinMetadata
import com.carryzonemap.app.domain.model.PinStatus

/**
 * Mapper for converting between domain models and database entities.
 */
object EntityMapper {
    /**
     * Converts a domain Pin to a database PinEntity.
     */
    fun Pin.toEntity(): PinEntity {
        return PinEntity(
            id = id,
            name = name,
            longitude = location.longitude,
            latitude = location.latitude,
            status = status.colorCode,
            photoUri = metadata.photoUri,
            notes = metadata.notes,
            votes = metadata.votes,
            createdBy = metadata.createdBy,
            createdAt = metadata.createdAt,
            lastModified = metadata.lastModified,
        )
    }

    /**
     * Converts a database PinEntity to a domain Pin.
     */
    fun PinEntity.toDomain(): Pin {
        return Pin(
            id = id,
            name = name,
            location =
                Location(
                    latitude = latitude,
                    longitude = longitude,
                ),
            status = PinStatus.fromColorCode(status),
            metadata =
                PinMetadata(
                    photoUri = photoUri,
                    notes = notes,
                    votes = votes,
                    createdBy = createdBy,
                    createdAt = createdAt,
                    lastModified = lastModified,
                ),
        )
    }

    /**
     * Converts a list of domain Pins to database PinEntities.
     */
    fun List<Pin>.toEntities(): List<PinEntity> = map { it.toEntity() }

    /**
     * Converts a list of database PinEntities to domain Pins.
     */
    fun List<PinEntity>.toDomainModels(): List<Pin> = map { it.toDomain() }
}
