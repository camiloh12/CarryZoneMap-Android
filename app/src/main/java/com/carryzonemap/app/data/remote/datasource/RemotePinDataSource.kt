package com.carryzonemap.app.data.remote.datasource

import com.carryzonemap.app.domain.model.Pin
import kotlinx.coroutines.flow.Flow

/**
 * Interface for remote pin data operations.
 *
 * This abstraction allows for different remote data source implementations
 * (e.g., Supabase, Firebase, custom REST API) while keeping the repository
 * implementation decoupled from the specific backend.
 */
interface RemotePinDataSource {

    /**
     * Retrieves all pins from the remote database.
     *
     * @return Result containing list of pins or error
     */
    suspend fun getAllPins(): Result<List<Pin>>

    /**
     * Retrieves a specific pin by ID from the remote database.
     *
     * @param pinId Unique identifier of the pin
     * @return Result containing the pin or null if not found, or error
     */
    suspend fun getPinById(pinId: String): Result<Pin?>

    /**
     * Inserts a new pin into the remote database.
     *
     * @param pin The pin to insert
     * @return Result containing the inserted pin (with server-assigned fields) or error
     */
    suspend fun insertPin(pin: Pin): Result<Pin>

    /**
     * Updates an existing pin in the remote database.
     *
     * @param pin The pin with updated data
     * @return Result containing the updated pin or error
     */
    suspend fun updatePin(pin: Pin): Result<Pin>

    /**
     * Deletes a pin from the remote database.
     *
     * @param pinId ID of the pin to delete
     * @return Result indicating success or error
     */
    suspend fun deletePin(pinId: String): Result<Unit>

    /**
     * Retrieves pins within a geographic bounding box.
     *
     * This is useful for fetching only pins visible in the current map viewport.
     *
     * @param minLat Minimum latitude (south)
     * @param maxLat Maximum latitude (north)
     * @param minLng Minimum longitude (west)
     * @param maxLng Maximum longitude (east)
     * @return Result containing list of pins within the bounding box or error
     */
    suspend fun getPinsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
    ): Result<List<Pin>>

    /**
     * Subscribes to real-time changes for pins.
     *
     * Returns a Flow that emits pin changes (inserts, updates, deletes) as they happen.
     *
     * @return Flow of pin change events
     */
    fun subscribeToChanges(): Flow<PinChangeEvent>
}

/**
 * Represents a change event for a pin from the real-time subscription.
 */
sealed class PinChangeEvent {
    /**
     * A new pin was inserted.
     */
    data class Insert(val pin: Pin) : PinChangeEvent()

    /**
     * An existing pin was updated.
     */
    data class Update(val pin: Pin) : PinChangeEvent()

    /**
     * A pin was deleted.
     */
    data class Delete(val pinId: String) : PinChangeEvent()
}
