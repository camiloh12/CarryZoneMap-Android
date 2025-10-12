package com.carryzonemap.app.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for background synchronization tasks.
 *
 * This class is responsible for scheduling periodic sync operations
 * using WorkManager. It ensures sync happens regularly when the device
 * has network connectivity.
 */
@Singleton
class SyncScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private const val TAG = "SyncScheduler"
            private const val SYNC_INTERVAL_MINUTES = 15L
        }

        /**
         * Schedule periodic sync with remote server.
         *
         * This will run every 15 minutes when the device has network connectivity.
         * Uses KEEP policy to avoid rescheduling if already scheduled.
         */
        fun schedulePeriodicSync() {
            Log.d(TAG, "Scheduling periodic sync (every $SYNC_INTERVAL_MINUTES minutes)")

            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val syncWorkRequest =
                PeriodicWorkRequestBuilder<SyncWorker>(
                    SYNC_INTERVAL_MINUTES,
                    TimeUnit.MINUTES,
                ).setConstraints(constraints)
                    .addTag(SyncWorker.WORK_TAG)
                    .build()

            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    SyncWorker.WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncWorkRequest,
                )

            Log.d(TAG, "Periodic sync scheduled successfully")
        }

        /**
         * Cancel all scheduled sync work.
         * Useful for testing or when user logs out.
         */
        fun cancelPeriodicSync() {
            Log.d(TAG, "Cancelling periodic sync")
            WorkManager
                .getInstance(context)
                .cancelUniqueWork(SyncWorker.WORK_NAME)
        }
    }
