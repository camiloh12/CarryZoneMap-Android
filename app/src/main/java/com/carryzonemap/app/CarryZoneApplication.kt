package com.carryzonemap.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for CarryZoneMap.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class CarryZoneApplication : Application()
