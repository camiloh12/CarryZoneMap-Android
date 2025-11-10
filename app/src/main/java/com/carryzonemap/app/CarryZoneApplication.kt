package com.carryzonemap.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.carryzonemap.app.data.sync.SyncScheduler
import com.carryzonemap.app.util.UsBoundaryValidator
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for CarryZoneMap.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * Implements Configuration.Provider to provide custom WorkManager configuration with Hilt support.
 */
@HiltAndroidApp
class CarryZoneApplication : Application(), Configuration.Provider {
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

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("Application starting")

        // Initialize US boundary validator with accurate boundaries
        UsBoundaryValidator.initialize(this)
        Timber.d("US boundary validator initialized")

        // Schedule periodic sync with remote server
        syncScheduler.schedulePeriodicSync()
        Timber.d("Background sync scheduled")
    }
}
