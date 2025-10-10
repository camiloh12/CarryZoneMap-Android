package com.carryzonemap.app.map

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class PersistedFeatureTest {

    @Test
    fun `toJSONObject should serialize correctly`() {
        // Arrange
        val feature = PersistedFeature(
            id = "test-id",
            lon = -122.4194,
            lat = 37.7749,
            state = 1
        )

        // Act
        val json = feature.toJSONObject()

        // Assert
        assertEquals("test-id", json.getString("id"))
        assertEquals(-122.4194, json.getDouble("lon"), 0.0001)
        assertEquals(37.7749, json.getDouble("lat"), 0.0001)
        assertEquals(1, json.getInt("state"))
    }

    @Test
    fun `fromJSONObject should deserialize correctly`() {
        // Arrange
        val json = JSONObject().apply {
            put("id", "test-id-2")
            put("lon", -74.0060)
            put("lat", 40.7128)
            put("state", 2)
        }

        // Act
        val feature = PersistedFeature.fromJSONObject(json)

        // Assert
        assertEquals("test-id-2", feature.id)
        assertEquals(-74.0060, feature.lon, 0.0001)
        assertEquals(40.7128, feature.lat, 0.0001)
        assertEquals(2, feature.state)
    }
}
