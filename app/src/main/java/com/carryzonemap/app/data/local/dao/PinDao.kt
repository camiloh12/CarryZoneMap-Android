package com.carryzonemap.app.data.local.dao

import androidx.room.*
import com.carryzonemap.app.data.local.entity.PinEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for pin database operations.
 */
@Dao
interface PinDao {

    /**
     * Get all pins as a reactive Flow.
     * The Flow will emit a new list whenever the database changes.
     */
    @Query("SELECT * FROM pins ORDER BY createdAt DESC")
    fun getAllPins(): Flow<List<PinEntity>>

    /**
     * Get a single pin by ID.
     */
    @Query("SELECT * FROM pins WHERE id = :pinId")
    suspend fun getPinById(pinId: String): PinEntity?

    /**
     * Insert a new pin. Replaces if conflict (same ID).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPin(pin: PinEntity)

    /**
     * Insert multiple pins.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPins(pins: List<PinEntity>)

    /**
     * Update an existing pin.
     */
    @Update
    suspend fun updatePin(pin: PinEntity)

    /**
     * Delete a specific pin.
     */
    @Delete
    suspend fun deletePin(pin: PinEntity)

    /**
     * Delete all pins (useful for testing or data reset).
     */
    @Query("DELETE FROM pins")
    suspend fun deleteAllPins()

    /**
     * Get count of all pins.
     */
    @Query("SELECT COUNT(*) FROM pins")
    suspend fun getPinCount(): Int
}
