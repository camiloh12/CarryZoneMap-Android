package com.carryzonemap.app.data.mapper

import com.carryzonemap.app.data.local.entity.PinEntity
import com.carryzonemap.app.data.mapper.EntityMapper.toDomain
import com.carryzonemap.app.data.mapper.EntityMapper.toDomainModels
import com.carryzonemap.app.data.mapper.EntityMapper.toEntities
import com.carryzonemap.app.data.mapper.EntityMapper.toEntity
import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.model.PinMetadata
import com.carryzonemap.app.domain.model.PinStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntityMapperTest {
    private val testPin =
        Pin(
            id = "test-123",
            name = "Test POI",
            location = Location(latitude = 40.7128, longitude = -74.0060),
            status = PinStatus.UNCERTAIN,
            metadata =
                PinMetadata(
                    photoUri = "file:///photo.jpg",
                    notes = "Test location",
                    votes = 5,
                    createdBy = "user-456",
                    createdAt = 1000L,
                    lastModified = 2000L,
                ),
        )

    private val testEntity =
        PinEntity(
            id = "test-123",
            name = "Test POI",
            longitude = -74.0060,
            latitude = 40.7128,
            // UNCERTAIN
            status = 1,
            photoUri = "file:///photo.jpg",
            notes = "Test location",
            votes = 5,
            createdBy = "user-456",
            createdAt = 1000L,
            lastModified = 2000L,
        )

    @Test
    fun `converts Pin to PinEntity correctly`() {
        val entity = testPin.toEntity()

        assertEquals(testPin.id, entity.id)
        assertEquals(testPin.location.longitude, entity.longitude, 0.0001)
        assertEquals(testPin.location.latitude, entity.latitude, 0.0001)
        assertEquals(testPin.status.colorCode, entity.status)
        assertEquals(testPin.metadata.photoUri, entity.photoUri)
        assertEquals(testPin.metadata.notes, entity.notes)
        assertEquals(testPin.metadata.votes, entity.votes)
        assertEquals(testPin.metadata.createdBy, entity.createdBy)
        assertEquals(testPin.metadata.createdAt, entity.createdAt)
        assertEquals(testPin.metadata.lastModified, entity.lastModified)
    }

    @Test
    fun `converts PinEntity to Pin correctly`() {
        val pin = testEntity.toDomain()

        assertEquals(testEntity.id, pin.id)
        assertEquals(testEntity.latitude, pin.location.latitude, 0.0001)
        assertEquals(testEntity.longitude, pin.location.longitude, 0.0001)
        assertEquals(PinStatus.fromColorCode(testEntity.status), pin.status)
        assertEquals(testEntity.photoUri, pin.metadata.photoUri)
        assertEquals(testEntity.notes, pin.metadata.notes)
        assertEquals(testEntity.votes, pin.metadata.votes)
        assertEquals(testEntity.createdBy, pin.metadata.createdBy)
        assertEquals(testEntity.createdAt, pin.metadata.createdAt)
        assertEquals(testEntity.lastModified, pin.metadata.lastModified)
    }

    @Test
    fun `roundtrip conversion Pin to Entity to Pin preserves data`() {
        val convertedPin = testPin.toEntity().toDomain()

        assertEquals(testPin.id, convertedPin.id)
        assertEquals(testPin.location, convertedPin.location)
        assertEquals(testPin.status, convertedPin.status)
        assertEquals(testPin.metadata.photoUri, convertedPin.metadata.photoUri)
        assertEquals(testPin.metadata.notes, convertedPin.metadata.notes)
        assertEquals(testPin.metadata.votes, convertedPin.metadata.votes)
    }

    @Test
    fun `roundtrip conversion Entity to Pin to Entity preserves data`() {
        val convertedEntity = testEntity.toDomain().toEntity()

        assertEquals(testEntity.id, convertedEntity.id)
        assertEquals(testEntity.longitude, convertedEntity.longitude, 0.0001)
        assertEquals(testEntity.latitude, convertedEntity.latitude, 0.0001)
        assertEquals(testEntity.status, convertedEntity.status)
        assertEquals(testEntity.photoUri, convertedEntity.photoUri)
        assertEquals(testEntity.notes, convertedEntity.notes)
    }

    @Test
    fun `converts Pin with null metadata fields correctly`() {
        val pinWithNulls =
            Pin(
                id = "null-test",
                name = "Test POI",
                location = Location(0.0, 0.0),
                status = PinStatus.ALLOWED,
                metadata = PinMetadata(),
            )

        val entity = pinWithNulls.toEntity()

        assertNull(entity.photoUri)
        assertNull(entity.notes)
        assertNull(entity.createdBy)
        assertEquals(0, entity.votes)
    }

    @Test
    fun `converts Entity with null fields to Pin correctly`() {
        val entityWithNulls =
            PinEntity(
                id = "null-test",
                name = "Test POI",
                longitude = 0.0,
                latitude = 0.0,
                status = 0,
            )

        val pin = entityWithNulls.toDomain()

        assertNull(pin.metadata.photoUri)
        assertNull(pin.metadata.notes)
        assertNull(pin.metadata.createdBy)
        assertEquals(0, pin.metadata.votes)
    }

    @Test
    fun `converts list of Pins to list of Entities`() {
        val pin1 = testPin
        val pin2 = testPin.copy(id = "test-456")
        val pins = listOf(pin1, pin2)

        val entities = pins.toEntities()

        assertEquals(2, entities.size)
        assertEquals(pin1.id, entities[0].id)
        assertEquals(pin2.id, entities[1].id)
    }

    @Test
    fun `converts list of Entities to list of Pins`() {
        val entity1 = testEntity
        val entity2 = testEntity.copy(id = "test-456")
        val entities = listOf(entity1, entity2)

        val pins = entities.toDomainModels()

        assertEquals(2, pins.size)
        assertEquals(entity1.id, pins[0].id)
        assertEquals(entity2.id, pins[1].id)
    }

    @Test
    fun `converts empty list of Pins to empty list of Entities`() {
        val pins = emptyList<Pin>()
        val entities = pins.toEntities()

        assertEquals(0, entities.size)
    }

    @Test
    fun `converts empty list of Entities to empty list of Pins`() {
        val entities = emptyList<PinEntity>()
        val pins = entities.toDomainModels()

        assertEquals(0, pins.size)
    }

    @Test
    fun `maps all PinStatus values correctly`() {
        val allowedPin = testPin.copy(status = PinStatus.ALLOWED)
        val uncertainPin = testPin.copy(status = PinStatus.UNCERTAIN)
        val noGunPin = testPin.copy(status = PinStatus.NO_GUN)

        assertEquals(0, allowedPin.toEntity().status)
        assertEquals(1, uncertainPin.toEntity().status)
        assertEquals(2, noGunPin.toEntity().status)
    }
}
