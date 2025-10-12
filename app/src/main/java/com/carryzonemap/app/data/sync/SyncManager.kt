package com.carryzonemap.app.data.sync

import com.carryzonemap.app.domain.model.Pin
import kotlinx.coroutines.flow.Flow

/**
 * Manages synchronization between local (Room) and remote (Supabase) data sources.
 *
 * Implements offline-first pattern:
 * - Operations are performed on local database immediately
 * - Changes are queued and synced to remote when online
 * - Remote changes are downloaded and merged with local data
 * - Conflicts are resolved using last-write-wins strategy
 */
interface SyncManager {
    /**
     * Observe the current sync status.
     *
     * Emits status changes as sync operations progress.
     */
    val syncStatus: Flow<SyncStatus>

    /**
     * Queue a pin for upload to remote (create operation).
     *
     * This should be called after successfully creating a pin locally.
     */
    suspend fun queuePinForUpload(pin: Pin)

    /**
     * Queue a pin for update on remote.
     *
     * This should be called after successfully updating a pin locally.
     */
    suspend fun queuePinForUpdate(pin: Pin)

    /**
     * Queue a pin for deletion from remote.
     *
     * This should be called after successfully deleting a pin locally.
     */
    suspend fun queuePinForDeletion(pinId: String)

    /**
     * Trigger a full synchronization with the remote server.
     *
     * This will:
     * 1. Upload all pending operations from the queue
     * 2. Download new/updated pins from remote
     * 3. Resolve any conflicts using last-write-wins
     *
     * @return Result indicating success or failure
     */
    suspend fun syncWithRemote(): Result<Unit>

    /**
     * Get the number of pending operations in the sync queue.
     */
    suspend fun getPendingOperationCount(): Int

    /**
     * Clear all pending operations (useful for testing or reset).
     */
    suspend fun clearQueue()
}
