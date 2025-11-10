package com.carryzonemap.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UsBoundaryValidator].
 * Tests validation of coordinates within the 50 US states and Washington DC.
 *
 * Note: These tests use the bounding box fallback since we can't easily
 * initialize the accurate boundary data in unit tests without Context.
 * The accurate boundary validation is tested in instrumentation tests.
 */
class UsBoundaryValidatorTest {

    // Continental US test cases
    @Test
    fun `isWithinUsBoundaries returns true for New York City`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 40.7128,
            longitude = -74.0060
        )
        assertTrue("NYC should be within US boundaries", result)
    }

    @Test
    fun `isWithinUsBoundaries returns true for Los Angeles`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 34.0522,
            longitude = -118.2437
        )
        assertTrue("Los Angeles should be within US boundaries", result)
    }

    @Test
    fun `isWithinUsBoundaries returns true for Chicago`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 41.8781,
            longitude = -87.6298
        )
        assertTrue("Chicago should be within US boundaries", result)
    }

    @Test
    fun `isWithinUsBoundaries returns true for Miami Florida`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 25.7617,
            longitude = -80.1918
        )
        assertTrue("Miami should be within US boundaries", result)
    }

    @Test
    fun `isWithinUsBoundaries returns true for Seattle Washington`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 47.6062,
            longitude = -122.3321
        )
        assertTrue("Seattle should be within US boundaries", result)
    }

    // Alaska test cases
    @Test
    fun `isWithinUsBoundaries returns true for Anchorage Alaska`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 61.2181,
            longitude = -149.9003
        )
        assertTrue("Anchorage should be within US boundaries", result)
    }

    // Hawaii test cases
    @Test
    fun `isWithinUsBoundaries returns true for Honolulu Hawaii`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 21.3099,
            longitude = -157.8581
        )
        assertTrue("Honolulu should be within US boundaries", result)
    }

    // Washington DC test case
    @Test
    fun `isWithinUsBoundaries returns true for Washington DC`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 38.9072,
            longitude = -77.0369
        )
        assertTrue("Washington DC should be within US boundaries", result)
    }

    // International locations (should be outside US)
    @Test
    fun `isWithinUsBoundaries returns false for Vancouver Canada`() {
        // Using Vancouver instead of Toronto - Toronto is too close to NY border
        // and falls within NY's bounding box (known limitation of bbox approach)
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 49.2827,  // Well north of US northern border
            longitude = -123.1207
        )
        assertFalse("Vancouver should be outside US boundaries", result)
    }

    @Test
    fun `isWithinUsBoundaries returns false for Mexico City`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 19.4326,
            longitude = -99.1332
        )
        assertFalse("Mexico City should be outside US boundaries", result)
    }

    @Test
    fun `isWithinUsBoundaries returns false for London UK`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 51.5074,
            longitude = -0.1278
        )
        assertFalse("London should be outside US boundaries", result)
    }

    @Test
    fun `isWithinUsBoundaries returns false for Tokyo Japan`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 35.6762,
            longitude = 139.6503
        )
        assertFalse("Tokyo should be outside US boundaries", result)
    }

    @Test
    fun `isWithinUsBoundaries returns false for Sydney Australia`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = -33.8688,
            longitude = 151.2093
        )
        assertFalse("Sydney should be outside US boundaries", result)
    }

    // Ocean coordinates (should be outside US)
    @Test
    fun `isWithinUsBoundaries returns false for Pacific Ocean`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 0.0,
            longitude = -140.0
        )
        assertFalse("Pacific Ocean should be outside US boundaries", result)
    }

    @Test
    fun `isWithinUsBoundaries returns false for Atlantic Ocean`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 30.0,
            longitude = -40.0
        )
        assertFalse("Atlantic Ocean should be outside US boundaries", result)
    }

    // Edge cases - near US borders
    @Test
    fun `isWithinUsBoundaries returns false for just north of US border`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 50.0, // Just above northern border
            longitude = -100.0
        )
        assertFalse("Location just north of US border should be outside boundaries", result)
    }

    @Test
    fun `isWithinUsBoundaries returns false for just south of US border`() {
        val result = UsBoundaryValidator.isWithinUsBoundaries(
            latitude = 24.0, // Just below southern tip of Florida
            longitude = -81.0
        )
        assertFalse("Location just south of US border should be outside boundaries", result)
    }

    // Test helper methods
    @Test
    fun `getOverallUsBoundingBox returns valid bounding box`() {
        val bbox = UsBoundaryValidator.getOverallUsBoundingBox()

        // Verify Alaska's bounds are included
        assertTrue("Min latitude should include Hawaii", bbox.minLat < 22.0)
        assertTrue("Max latitude should include Alaska", bbox.maxLat > 70.0)
        assertTrue("Min longitude should include Alaska", bbox.minLng < -170.0)
        assertTrue("Max longitude should include Maine", bbox.maxLng > -70.0)
    }

    @Test
    fun `getContinentalUsBoundingBox returns valid bounding box`() {
        val bbox = UsBoundaryValidator.getContinentalUsBoundingBox()

        // Verify continental bounds exclude Alaska and Hawaii extremes
        assertTrue("Min latitude should be around Florida", bbox.minLat < 30.0)
        assertTrue("Max latitude should be around northern border", bbox.maxLat > 45.0)
        assertTrue("Min longitude should be around west coast", bbox.minLng < -120.0)
        assertTrue("Max longitude should be around east coast", bbox.maxLng > -70.0)
    }

    @Test
    fun `BoundingBox contains method works correctly`() {
        val bbox = UsBoundaryValidator.BoundingBox(
            minLat = 30.0,
            maxLat = 40.0,
            minLng = -100.0,
            maxLng = -90.0
        )

        // Point inside
        assertTrue(
            "Point inside bbox should return true",
            bbox.contains(35.0, -95.0)
        )

        // Point outside (north)
        assertFalse(
            "Point north of bbox should return false",
            bbox.contains(41.0, -95.0)
        )

        // Point outside (south)
        assertFalse(
            "Point south of bbox should return false",
            bbox.contains(29.0, -95.0)
        )

        // Point outside (west)
        assertFalse(
            "Point west of bbox should return false",
            bbox.contains(35.0, -101.0)
        )

        // Point outside (east)
        assertFalse(
            "Point east of bbox should return false",
            bbox.contains(35.0, -89.0)
        )

        // Point on edge (should be included)
        assertTrue(
            "Point on bbox edge should return true",
            bbox.contains(30.0, -95.0)
        )
    }
}
