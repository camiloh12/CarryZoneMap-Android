package com.carryzonemap.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import javax.inject.Singleton

/**
 * Hilt module providing HTTP client for network requests.
 *
 * This provides a general-purpose HttpClient for making HTTP requests
 * (e.g., to Overpass API for POI data).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides a singleton HttpClient instance using the Android engine.
     *
     * This client is used for general HTTP requests outside of Supabase
     * (e.g., fetching POI data from OpenStreetMap's Overpass API).
     */
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(Android)
    }
}
