package com.carryzonemap.app.domain.mapper

import com.carryzonemap.app.domain.mapper.PinMapper.toFeature
import com.carryzonemap.app.domain.mapper.PinMapper.toFeatures
import com.carryzonemap.app.domain.mapper.PinMapper.toPin
import com.carryzonemap.app.domain.mapper.PinMapper.toPins
import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.model.PinMetadata
import com.carryzonemap.app.domain.model.PinStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

class PinMapperTest {
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
                    createdAt = 1000L,
                ),
        )

    @Test
    fun `converts Pin to Feature correctly`() {
        val feature = testPin.toFeature()

        val point = feature.geometry() as Point
        assertEquals(testPin.location.latitude, point.latitude(), 0.0001)
        assertEquals(testPin.location.longitude, point.longitude(), 0.0001)
        assertEquals(testPin.id, feature.getStringProperty("feature_id"))
        assertEquals(testPin.status.colorCode, feature.getNumberProperty("color_state")?.toInt())
        assertEquals(testPin.metadata.photoUri, feature.getStringProperty("photo_uri"))
        assertEquals(testPin.metadata.notes, feature.getStringProperty("notes"))
        assertEquals(testPin.metadata.createdAt, feature.getNumberProperty("created_at")?.toLong())
    }

    @Test
    fun `converts Feature to Pin correctly`() {
        val feature = testPin.toFeature()
        val pin = feature.toPin()

        assertNotNull(pin)
        assertEquals(testPin.id, pin!!.id)
        assertEquals(testPin.location.latitude, pin.location.latitude, 0.0001)
        assertEquals(testPin.location.longitude, pin.location.longitude, 0.0001)
        assertEquals(testPin.status, pin.status)
        assertEquals(testPin.metadata.photoUri, pin.metadata.photoUri)
        assertEquals(testPin.metadata.notes, pin.metadata.notes)
        assertEquals(testPin.metadata.createdAt, pin.metadata.createdAt)
    }

    @Test
    fun `roundtrip conversion Pin to Feature to Pin preserves data`() {
        val convertedPin = testPin.toFeature().toPin()

        assertNotNull(convertedPin)
        assertEquals(testPin.id, convertedPin!!.id)
        assertEquals(testPin.location, convertedPin.location)
        assertEquals(testPin.status, convertedPin.status)
        assertEquals(testPin.metadata.photoUri, convertedPin.metadata.photoUri)
        assertEquals(testPin.metadata.notes, convertedPin.metadata.notes)
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

        val feature = pinWithNulls.toFeature()

        assertEquals("null-test", feature.getStringProperty("feature_id"))
        assertNull(feature.getStringProperty("photo_uri"))
        assertNull(feature.getStringProperty("notes"))
    }

    @Test
    fun `converts Feature with missing optional properties to Pin`() {
        val point = Point.fromLngLat(-74.0060, 40.7128)
        val feature = Feature.fromGeometry(point)
        feature.addStringProperty("feature_id", "minimal-test")
        feature.addStringProperty("name", "Test POI")
        feature.addNumberProperty("color_state", 0)

        val pin = feature.toPin()

        assertNotNull(pin)
        assertEquals("minimal-test", pin!!.id)
        assertEquals("Test POI", pin.name)
        assertNull(pin.metadata.photoUri)
        assertNull(pin.metadata.notes)
    }

    @Test
    fun `returns null when Feature has no geometry`() {
        val feature = Feature.fromGeometry(null as Point?)
        feature.addStringProperty("feature_id", "no-geometry")

        val pin = feature.toPin()

        assertNull(pin)
    }

    @Test
    fun `returns null when Feature has no feature_id`() {
        val point = Point.fromLngLat(-74.0060, 40.7128)
        val feature = Feature.fromGeometry(point)
        feature.addNumberProperty("color_state", 0)

        val pin = feature.toPin()

        assertNull(pin)
    }

    @Test
    fun `uses default ALLOWED status when color_state is missing`() {
        val point = Point.fromLngLat(-74.0060, 40.7128)
        val feature = Feature.fromGeometry(point)
        feature.addStringProperty("feature_id", "default-status")
        feature.addStringProperty("name", "Test POI")

        val pin = feature.toPin()

        assertNotNull(pin)
        assertEquals(PinStatus.ALLOWED, pin!!.status)
    }

    @Test
    fun `converts list of Pins to list of Features`() {
        val pin1 = testPin
        val pin2 = testPin.copy(id = "test-456")
        val pins = listOf(pin1, pin2)

        val features = pins.toFeatures()

        assertEquals(2, features.size)
        assertEquals(pin1.id, features[0].getStringProperty("feature_id"))
        assertEquals(pin2.id, features[1].getStringProperty("feature_id"))
    }

    @Test
    fun `converts list of Features to list of Pins`() {
        val features = listOf(testPin.toFeature(), testPin.copy(id = "456").toFeature())

        val pins = features.toPins()

        assertEquals(2, pins.size)
        assertEquals(testPin.id, pins[0].id)
        assertEquals("456", pins[1].id)
    }

    @Test
    fun `filters out invalid Features when converting to Pins`() {
        val validFeature = testPin.toFeature()
        val invalidFeature = Feature.fromGeometry(null as Point?) // No geometry
        val features = listOf(validFeature, invalidFeature)

        val pins = features.toPins()

        assertEquals(1, pins.size)
        assertEquals(testPin.id, pins[0].id)
    }

    @Test
    fun `converts empty list of Pins to empty list of Features`() {
        val pins = emptyList<Pin>()
        val features = pins.toFeatures()

        assertEquals(0, features.size)
    }

    @Test
    fun `converts empty list of Features to empty list of Pins`() {
        val features = emptyList<Feature>()
        val pins = features.toPins()

        assertEquals(0, pins.size)
    }

    @Test
    fun `maps all PinStatus values correctly`() {
        val allowedPin = testPin.copy(status = PinStatus.ALLOWED)
        val uncertainPin = testPin.copy(status = PinStatus.UNCERTAIN)
        val noGunPin = testPin.copy(status = PinStatus.NO_GUN)

        assertEquals(0, allowedPin.toFeature().getNumberProperty("color_state")?.toInt())
        assertEquals(1, uncertainPin.toFeature().getNumberProperty("color_state")?.toInt())
        assertEquals(2, noGunPin.toFeature().getNumberProperty("color_state")?.toInt())
    }
}
