package com.carryzonemap.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.carryzonemap.app.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for CarryZoneMap.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * Implements Configuration.Provider to provide custom WorkManager configuration with Hilt support.
 */
@HiltAndroidApp
class CarryZoneApplication : Application(), Configuration.Provider {
    companion object {
        private const val TAG = "CarryZoneApp"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application starting")

        // Schedule periodic sync with remote server
        syncScheduler.schedulePeriodicSync()
        Log.d(TAG, "Background sync scheduled")
    }
}
