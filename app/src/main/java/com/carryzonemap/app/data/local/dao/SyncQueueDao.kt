package com.carryzonemap.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.carryzonemap.app.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for sync queue operations.
 *
 * Manages pending operations that need to be synced to the remote server.
 */
@Dao
interface SyncQueueDao {
    /**
     * Get all pending operations, ordered by timestamp (FIFO).
     */
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getAllOperations(): List<SyncQueueEntity>

    /**
     * Get all pending operations as a Flow (reactive).
     */
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    fun observeOperations(): Flow<List<SyncQueueEntity>>

    /**
     * Get pending operations for a specific pin.
     */
    @Query("SELECT * FROM sync_queue WHERE pin_id = :pinId ORDER BY timestamp ASC")
    suspend fun getOperationsForPin(pinId: String): List<SyncQueueEntity>

    /**
     * Insert a new operation into the queue.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: SyncQueueEntity): Long

    /**
     * Update an existing operation (used for retry count and error messages).
     */
    @Update
    suspend fun updateOperation(operation: SyncQueueEntity)

    /**
     * Delete an operation after successful sync.
     */
    @Query("DELETE FROM sync_queue WHERE id = :operationId")
    suspend fun deleteOperation(operationId: Long)

    /**
     * Delete operations for a specific pin.
     */
    @Query("DELETE FROM sync_queue WHERE pin_id = :pinId")
    suspend fun deleteOperationsForPin(pinId: String)

    /**
     * Delete all operations (useful for testing or resetting).
     */
    @Query("DELETE FROM sync_queue")
    suspend fun deleteAllOperations()

    /**
     * Get count of pending operations.
     */
    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getOperationCount(): Int

    /**
     * Get operations that have failed multiple times (retry_count > threshold).
     */
    @Query("SELECT * FROM sync_queue WHERE retry_count >= :threshold ORDER BY timestamp ASC")
    suspend fun getFailedOperations(threshold: Int = 3): List<SyncQueueEntity>
}
