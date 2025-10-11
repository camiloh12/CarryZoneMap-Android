package com.carryzonemap.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationTest {

    @Test
    fun `creates location with valid coordinates`() {
        val location = Location(latitude = 40.7128, longitude = -74.0060)

        assertEquals(40.7128, location.latitude, 0.0001)
        assertEquals(-74.0060, location.longitude, 0.0001)
    }

    @Test
    fun `creates location from lng lat with reversed order`() {
        val location = Location.fromLngLat(longitude = -74.0060, latitude = 40.7128)

        assertEquals(40.7128, location.latitude, 0.0001)
        assertEquals(-74.0060, location.longitude, 0.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws exception when latitude exceeds 90`() {
        Location(latitude = 91.0, longitude = 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws exception when latitude is below -90`() {
        Location(latitude = -91.0, longitude = 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws exception when longitude exceeds 180`() {
        Location(latitude = 0.0, longitude = 181.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws exception when longitude is below -180`() {
        Location(latitude = 0.0, longitude = -181.0)
    }

    @Test
    fun `accepts boundary latitude values`() {
        val north = Location(latitude = 90.0, longitude = 0.0)
        val south = Location(latitude = -90.0, longitude = 0.0)

        assertEquals(90.0, north.latitude, 0.0001)
        assertEquals(-90.0, south.latitude, 0.0001)
    }

    @Test
    fun `accepts boundary longitude values`() {
        val east = Location(latitude = 0.0, longitude = 180.0)
        val west = Location(latitude = 0.0, longitude = -180.0)

        assertEquals(180.0, east.longitude, 0.0001)
        assertEquals(-180.0, west.longitude, 0.0001)
    }
}
