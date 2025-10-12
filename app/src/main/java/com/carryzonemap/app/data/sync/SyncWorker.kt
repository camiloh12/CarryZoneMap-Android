package com.carryzonemap.app.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker for periodic synchronization with remote server.
 *
 * This worker is scheduled to run periodically (e.g., every 15 minutes)
 * when the device has network connectivity. It triggers a full sync
 * with the remote server to upload pending changes and download updates.
 *
 * Uses Hilt for dependency injection with WorkManager.
 *
 * @property context Application context (injected by WorkManager)
 * @property params Worker parameters (injected by WorkManager)
 * @property syncManager SyncManager to perform synchronization
 */
@HiltWorker
class SyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val syncManager: SyncManager,
    ) : CoroutineWorker(context, params) {
        companion object {
            private const val TAG = "SyncWorker"

            /**
             * Unique name for periodic sync work.
             */
            const val WORK_NAME = "pin_sync_worker"

            /**
             * Tag for this work type.
             */
            const val WORK_TAG = "sync"
        }

        override suspend fun doWork(): Result {
            Log.d(TAG, "Starting background sync")

            return try {
                // Attempt to sync with remote
                val result = syncManager.syncWithRemote()

                if (result.isSuccess) {
                    Log.d(TAG, "Background sync succeeded")
                    Result.success()
                } else {
                    Log.w(TAG, "Background sync failed: ${result.exceptionOrNull()?.message}")
                    // Retry on failure (WorkManager will handle backoff)
                    Result.retry()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Background sync error", e)
                // Retry on exception
                Result.retry()
            }
        }
    }
