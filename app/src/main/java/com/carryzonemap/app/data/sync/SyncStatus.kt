package com.carryzonemap.app.data.sync

/**
 * Represents the current synchronization status.
 */
sealed class SyncStatus {
    /**
     * No sync operation in progress.
     */
    data object Idle : SyncStatus()

    /**
     * Sync operation is in progress.
     *
     * @property pendingCount Number of operations waiting to be synced
     */
    data class Syncing(val pendingCount: Int) : SyncStatus()

    /**
     * Sync completed successfully.
     *
     * @property uploadedCount Number of local changes uploaded to remote
     * @property downloadedCount Number of remote changes downloaded to local
     */
    data class Success(
        val uploadedCount: Int,
        val downloadedCount: Int,
    ) : SyncStatus()

    /**
     * Sync failed with an error.
     *
     * @property message Error message describing what went wrong
     * @property retryable Whether the operation can be retried
     */
    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : SyncStatus()
}
