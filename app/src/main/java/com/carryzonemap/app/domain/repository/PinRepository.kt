package com.carryzonemap.app.domain.repository

import com.carryzonemap.app.domain.model.Pin
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for pin data operations.
 * Provides an abstraction layer between the domain layer and data sources.
 */
interface PinRepository {

    /**
     * Get all pins as a reactive Flow.
     * Emits a new list whenever pins are added, updated, or deleted.
     */
    fun getAllPins(): Flow<List<Pin>>

    /**
     * Get a single pin by its ID.
     * Returns null if the pin doesn't exist.
     */
    suspend fun getPinById(pinId: String): Pin?

    /**
     * Add a new pin to the repository.
     */
    suspend fun addPin(pin: Pin)

    /**
     * Update an existing pin.
     */
    suspend fun updatePin(pin: Pin)

    /**
     * Delete a specific pin.
     */
    suspend fun deletePin(pin: Pin)

    /**
     * Delete all pins (useful for clearing data).
     */
    suspend fun deleteAllPins()

    /**
     * Cycle the status of a pin (ALLOWED -> UNCERTAIN -> NO_GUN -> ALLOWED).
     * Returns true if successful, false if pin not found.
     */
    suspend fun cyclePinStatus(pinId: String): Boolean

    /**
     * Get the total count of pins.
     */
    suspend fun getPinCount(): Int
}
