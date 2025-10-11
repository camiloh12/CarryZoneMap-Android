package com.carryzonemap.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PinStatusTest {

    @Test
    fun `next cycles from ALLOWED to UNCERTAIN`() {
        val status = PinStatus.ALLOWED
        assertEquals(PinStatus.UNCERTAIN, status.next())
    }

    @Test
    fun `next cycles from UNCERTAIN to NO_GUN`() {
        val status = PinStatus.UNCERTAIN
        assertEquals(PinStatus.NO_GUN, status.next())
    }

    @Test
    fun `next cycles from NO_GUN to ALLOWED`() {
        val status = PinStatus.NO_GUN
        assertEquals(PinStatus.ALLOWED, status.next())
    }

    @Test
    fun `next completes full cycle`() {
        var status = PinStatus.ALLOWED
        status = status.next() // UNCERTAIN
        status = status.next() // NO_GUN
        status = status.next() // ALLOWED

        assertEquals(PinStatus.ALLOWED, status)
    }

    @Test
    fun `fromColorCode returns ALLOWED for code 0`() {
        assertEquals(PinStatus.ALLOWED, PinStatus.fromColorCode(0))
    }

    @Test
    fun `fromColorCode returns UNCERTAIN for code 1`() {
        assertEquals(PinStatus.UNCERTAIN, PinStatus.fromColorCode(1))
    }

    @Test
    fun `fromColorCode returns NO_GUN for code 2`() {
        assertEquals(PinStatus.NO_GUN, PinStatus.fromColorCode(2))
    }

    @Test
    fun `fromColorCode returns ALLOWED for invalid code`() {
        assertEquals(PinStatus.ALLOWED, PinStatus.fromColorCode(999))
        assertEquals(PinStatus.ALLOWED, PinStatus.fromColorCode(-1))
    }

    @Test
    fun `status has correct color codes`() {
        assertEquals(0, PinStatus.ALLOWED.colorCode)
        assertEquals(1, PinStatus.UNCERTAIN.colorCode)
        assertEquals(2, PinStatus.NO_GUN.colorCode)
    }

    @Test
    fun `status has correct display names`() {
        assertEquals("Allowed", PinStatus.ALLOWED.displayName)
        assertEquals("Uncertain", PinStatus.UNCERTAIN.displayName)
        assertEquals("No Guns", PinStatus.NO_GUN.displayName)
    }
}
