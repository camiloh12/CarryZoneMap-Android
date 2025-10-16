package com.carryzonemap.app.data.remote.mapper

import com.carryzonemap.app.data.remote.dto.SupabasePinDto
import com.carryzonemap.app.data.remote.mapper.SupabaseMapper.toDomain
import com.carryzonemap.app.data.remote.mapper.SupabaseMapper.toDomainModels
import com.carryzonemap.app.data.remote.mapper.SupabaseMapper.toSupabaseDto
import com.carryzonemap.app.data.remote.mapper.SupabaseMapper.toSupabaseDtos
import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.model.PinMetadata
import com.carryzonemap.app.domain.model.PinStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for SupabaseMapper.
 *
 * Tests conversion between Supabase DTOs and domain models, including:
 * - Individual conversions (DTO ↔ Domain)
 * - Bulk conversions (List<DTO> ↔ List<Domain>)
 * - Round-trip conversions (data preservation)
 * - Timestamp parsing and formatting
 * - Null field handling
 */
class SupabaseMapperTest {

    // Test data constants
    private val testId = "test-pin-id-123"
    private val testLongitude = -122.4194
    private val testLatitude = 37.7749
    private val testPhotoUri = "https://example.com/photo.jpg"
    private val testNotes = "Test notes"
    private val testVotes = 42
    private val testCreatedBy = "user-123"
    private val testTimestamp = "2023-10-15T12:30:45.123Z"
    private val testTimestampMillis = Instant.parse(testTimestamp).toEpochMilli()

    @Test
    fun `toDomain converts SupabasePinDto to Pin correctly`() {
        // Given
        val dto = SupabasePinDto(
            id = testId,
            name = "Test POI",
            longitude = testLongitude,
            latitude = testLatitude,
            status = 0, // ALLOWED
            photoUri = testPhotoUri,
            notes = testNotes,
            votes = testVotes,
            createdBy = testCreatedBy,
            createdAt = testTimestamp,
            lastModified = testTimestamp,
        )

        // When
        val pin = dto.toDomain()

        // Then
        assertEquals(testId, pin.id)
        assertEquals(testLongitude, pin.location.longitude, 0.0001)
        assertEquals(testLatitude, pin.location.latitude, 0.0001)
        assertEquals(PinStatus.ALLOWED, pin.status)
        assertEquals(testPhotoUri, pin.metadata.photoUri)
        assertEquals(testNotes, pin.metadata.notes)
        assertEquals(testVotes, pin.metadata.votes)
        assertEquals(testCreatedBy, pin.metadata.createdBy)
        assertEquals(testTimestampMillis, pin.metadata.createdAt)
        assertEquals(testTimestampMillis, pin.metadata.lastModified)
    }

    @Test
    fun `toDomain handles all status codes correctly`() {
        // Test all PinStatus values
        val statusCases = listOf(
            0 to PinStatus.ALLOWED,
            1 to PinStatus.UNCERTAIN,
            2 to PinStatus.NO_GUN
        )

        for ((statusCode, expectedStatus) in statusCases) {
            // Given
            val dto = createTestDto(status = statusCode)

            // When
            val pin = dto.toDomain()

            // Then
            assertEquals(
                "Status code $statusCode should map to $expectedStatus",
                expectedStatus,
                pin.status
            )
        }
    }

    @Test
    fun `toDomain handles null optional fields`() {
        // Given
        val dto = SupabasePinDto(
            id = testId,
            name = "Test POI",
            longitude = testLongitude,
            latitude = testLatitude,
            status = 0,
            photoUri = null,
            notes = null,
            votes = 0,
            createdBy = null,
            createdAt = testTimestamp,
            lastModified = testTimestamp,
        )

        // When
        val pin = dto.toDomain()

        // Then
        assertNull(pin.metadata.photoUri)
        assertNull(pin.metadata.notes)
        assertEquals(0, pin.metadata.votes)
        assertNull(pin.metadata.createdBy)
    }

    @Test
    fun `toSupabaseDto converts Pin to SupabasePinDto correctly`() {
        // Given
        val pin = Pin(
            id = testId,
            name = "Test POI",
            location = Location.fromLngLat(testLongitude, testLatitude),
            status = PinStatus.UNCERTAIN,
            metadata = PinMetadata(
                photoUri = testPhotoUri,
                notes = testNotes,
                votes = testVotes,
                createdBy = testCreatedBy,
                createdAt = testTimestampMillis,
                lastModified = testTimestampMillis,
            ),
        )

        // When
        val dto = pin.toSupabaseDto()

        // Then
        assertEquals(testId, dto.id)
        assertEquals(testLongitude, dto.longitude, 0.0001)
        assertEquals(testLatitude, dto.latitude, 0.0001)
        assertEquals(1, dto.status) // UNCERTAIN = 1
        assertEquals(testPhotoUri, dto.photoUri)
        assertEquals(testNotes, dto.notes)
        assertEquals(testVotes, dto.votes)
        assertEquals(testCreatedBy, dto.createdBy)

        // Verify timestamp format is ISO 8601
        assertTrue(dto.createdAt.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")))
        assertTrue(dto.lastModified.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")))
    }

    @Test
    fun `toSupabaseDto handles all PinStatus values correctly`() {
        // Test all PinStatus values
        val statusCases = listOf(
            PinStatus.ALLOWED to 0,
            PinStatus.UNCERTAIN to 1,
            PinStatus.NO_GUN to 2
        )

        for ((pinStatus, expectedCode) in statusCases) {
            // Given
            val pin = createTestPin(status = pinStatus)

            // When
            val dto = pin.toSupabaseDto()

            // Then
            assertEquals(
                "$pinStatus should map to status code $expectedCode",
                expectedCode,
                dto.status
            )
        }
    }

    @Test
    fun `round trip conversion preserves all data`() {
        // Given
        val originalDto = SupabasePinDto(
            id = testId,
            name = "Test POI",
            longitude = testLongitude,
            latitude = testLatitude,
            status = 2, // NO_GUN
            photoUri = testPhotoUri,
            notes = testNotes,
            votes = testVotes,
            createdBy = testCreatedBy,
            createdAt = testTimestamp,
            lastModified = testTimestamp,
        )

        // When: DTO -> Domain -> DTO
        val pin = originalDto.toDomain()
        val resultDto = pin.toSupabaseDto()

        // Then: All important fields should match
        assertEquals(originalDto.id, resultDto.id)
        assertEquals(originalDto.longitude, resultDto.longitude, 0.0001)
        assertEquals(originalDto.latitude, resultDto.latitude, 0.0001)
        assertEquals(originalDto.status, resultDto.status)
        assertEquals(originalDto.photoUri, resultDto.photoUri)
        assertEquals(originalDto.notes, resultDto.notes)
        assertEquals(originalDto.votes, resultDto.votes)
        assertEquals(originalDto.createdBy, resultDto.createdBy)
    }

    @Test
    fun `round trip conversion with null fields preserves nulls`() {
        // Given
        val originalDto = SupabasePinDto(
            id = testId,
            name = "Test POI",
            longitude = testLongitude,
            latitude = testLatitude,
            status = 0,
            photoUri = null,
            notes = null,
            votes = 0,
            createdBy = null,
            createdAt = testTimestamp,
            lastModified = testTimestamp,
        )

        // When: DTO -> Domain -> DTO
        val pin = originalDto.toDomain()
        val resultDto = pin.toSupabaseDto()

        // Then
        assertNull(resultDto.photoUri)
        assertNull(resultDto.notes)
        assertEquals(0, resultDto.votes)
        assertNull(resultDto.createdBy)
    }

    @Test
    fun `toDomainModels converts list of DTOs correctly`() {
        // Given
        val dtos = listOf(
            createTestDto(id = "pin-1", status = 0),
            createTestDto(id = "pin-2", status = 1),
            createTestDto(id = "pin-3", status = 2),
        )

        // When
        val pins = dtos.toDomainModels()

        // Then
        assertEquals(3, pins.size)
        assertEquals("pin-1", pins[0].id)
        assertEquals(PinStatus.ALLOWED, pins[0].status)
        assertEquals("pin-2", pins[1].id)
        assertEquals(PinStatus.UNCERTAIN, pins[1].status)
        assertEquals("pin-3", pins[2].id)
        assertEquals(PinStatus.NO_GUN, pins[2].status)
    }

    @Test
    fun `toSupabaseDtos converts list of Pins correctly`() {
        // Given
        val pins = listOf(
            createTestPin(id = "pin-1", status = PinStatus.ALLOWED),
            createTestPin(id = "pin-2", status = PinStatus.UNCERTAIN),
            createTestPin(id = "pin-3", status = PinStatus.NO_GUN),
        )

        // When
        val dtos = pins.toSupabaseDtos()

        // Then
        assertEquals(3, dtos.size)
        assertEquals("pin-1", dtos[0].id)
        assertEquals(0, dtos[0].status)
        assertEquals("pin-2", dtos[1].id)
        assertEquals(1, dtos[1].status)
        assertEquals("pin-3", dtos[2].id)
        assertEquals(2, dtos[2].status)
    }

    @Test
    fun `toDomainModels handles empty list`() {
        // Given
        val emptyList = emptyList<SupabasePinDto>()

        // When
        val pins = emptyList.toDomainModels()

        // Then
        assertTrue(pins.isEmpty())
    }

    @Test
    fun `toSupabaseDtos handles empty list`() {
        // Given
        val emptyList = emptyList<Pin>()

        // When
        val dtos = emptyList.toSupabaseDtos()

        // Then
        assertTrue(dtos.isEmpty())
    }

    @Test
    fun `timestamp parsing handles various ISO 8601 formats`() {
        val timestampFormats = listOf(
            "2023-10-15T12:30:45.123Z",
            "2023-10-15T12:30:45.123456Z", // With microseconds
            "2023-10-15T12:30:45Z", // Without milliseconds
        )

        for (timestamp in timestampFormats) {
            // Given
            val dto = createTestDto(createdAt = timestamp, lastModified = timestamp)

            // When
            val pin = dto.toDomain()

            // Then
            assertNotNull(pin.metadata.createdAt)
            assertNotNull(pin.metadata.lastModified)
            assertTrue("Timestamp $timestamp should parse to a positive value", pin.metadata.createdAt > 0)
        }
    }

    @Test
    fun `timestamp formatting produces valid ISO 8601 format`() {
        // Given
        val pin = createTestPin()

        // When
        val dto = pin.toSupabaseDto()

        // Then
        val iso8601Pattern = Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")
        assertTrue(
            "createdAt should be in ISO 8601 format",
            dto.createdAt.matches(iso8601Pattern)
        )
        assertTrue(
            "lastModified should be in ISO 8601 format",
            dto.lastModified.matches(iso8601Pattern)
        )
    }

    // Helper functions

    private fun createTestDto(
        id: String = testId,
        longitude: Double = testLongitude,
        latitude: Double = testLatitude,
        status: Int = 0,
        photoUri: String? = testPhotoUri,
        notes: String? = testNotes,
        votes: Int = testVotes,
        createdBy: String? = testCreatedBy,
        createdAt: String = testTimestamp,
        lastModified: String = testTimestamp,
    ): SupabasePinDto {
        return SupabasePinDto(
            id = id,
            name = "Test POI",
            longitude = longitude,
            latitude = latitude,
            status = status,
            photoUri = photoUri,
            notes = notes,
            votes = votes,
            createdBy = createdBy,
            createdAt = createdAt,
            lastModified = lastModified,
        )
    }

    private fun createTestPin(
        id: String = testId,
        longitude: Double = testLongitude,
        latitude: Double = testLatitude,
        status: PinStatus = PinStatus.ALLOWED,
        photoUri: String? = testPhotoUri,
        notes: String? = testNotes,
        votes: Int = testVotes,
        createdBy: String? = testCreatedBy,
        createdAt: Long = testTimestampMillis,
        lastModified: Long = testTimestampMillis,
    ): Pin {
        return Pin(
            id = id,
            name = "Test POI",
            location = Location.fromLngLat(longitude, latitude),
            status = status,
            metadata = PinMetadata(
                photoUri = photoUri,
                notes = notes,
                votes = votes,
                createdBy = createdBy,
                createdAt = createdAt,
                lastModified = lastModified,
            ),
        )
    }
}
