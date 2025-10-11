package com.carryzonemap.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinTest {
    private val testLocation = Location(latitude = 40.7128, longitude = -74.0060)
    private val testPin =
        Pin(
            id = "test-id",
            location = testLocation,
            status = PinStatus.ALLOWED,
            metadata = PinMetadata(createdAt = 1000L, lastModified = 1000L),
        )

    @Test
    fun `creates pin with fromLngLat factory method`() {
        val pin = Pin.fromLngLat(longitude = -74.0060, latitude = 40.7128)

        assertEquals(40.7128, pin.location.latitude, 0.0001)
        assertEquals(-74.0060, pin.location.longitude, 0.0001)
        assertEquals(PinStatus.ALLOWED, pin.status)
    }

    @Test
    fun `fromLngLat creates pin with custom status`() {
        val pin =
            Pin.fromLngLat(
                longitude = -74.0060,
                latitude = 40.7128,
                status = PinStatus.NO_GUN,
            )

        assertEquals(PinStatus.NO_GUN, pin.status)
    }

    @Test
    fun `withNextStatus cycles status and updates lastModified`() {
        val updatedPin = testPin.withNextStatus()

        assertEquals(PinStatus.UNCERTAIN, updatedPin.status)
        assertTrue(updatedPin.metadata.lastModified > testPin.metadata.lastModified)
        assertEquals(testPin.metadata.createdAt, updatedPin.metadata.createdAt)
    }

    @Test
    fun `withNextStatus creates new instance`() {
        val updatedPin = testPin.withNextStatus()

        assertNotEquals(testPin, updatedPin)
        assertEquals(PinStatus.ALLOWED, testPin.status) // Original unchanged
    }

    @Test
    fun `withStatus updates status and lastModified`() {
        val updatedPin = testPin.withStatus(PinStatus.NO_GUN)

        assertEquals(PinStatus.NO_GUN, updatedPin.status)
        assertTrue(updatedPin.metadata.lastModified > testPin.metadata.lastModified)
    }

    @Test
    fun `withMetadata updates metadata and lastModified`() {
        val newMetadata =
            PinMetadata(
                notes = "Test note",
                photoUri = "test://photo",
            )
        val updatedPin = testPin.withMetadata(newMetadata)

        assertEquals("Test note", updatedPin.metadata.notes)
        assertEquals("test://photo", updatedPin.metadata.photoUri)
        assertTrue(updatedPin.metadata.lastModified >= newMetadata.lastModified)
    }

    @Test
    fun `withMetadata preserves other pin properties`() {
        val newMetadata = PinMetadata(notes = "New note")
        val updatedPin = testPin.withMetadata(newMetadata)

        assertEquals(testPin.id, updatedPin.id)
        assertEquals(testPin.location, updatedPin.location)
        assertEquals(testPin.status, updatedPin.status)
    }

    @Test
    fun `pin generates unique ids by default`() {
        val pin1 = Pin.fromLngLat(-74.0, 40.7)
        val pin2 = Pin.fromLngLat(-74.0, 40.7)

        assertNotEquals(pin1.id, pin2.id)
    }

    @Test
    fun `pin has default metadata when not provided`() {
        val pin = Pin(location = testLocation)

        assertEquals(
            PinMetadata(),
            pin.metadata.copy(
                createdAt = PinMetadata().createdAt,
                lastModified = PinMetadata().lastModified,
            ),
        )
    }
}
