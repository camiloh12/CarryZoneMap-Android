package com.carryzonemap.app.di

import android.content.Context
import com.carryzonemap.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

/**
 * Hilt module providing Supabase client and related dependencies.
 *
 * This module initializes the Supabase client with:
 * - Postgrest: For database operations (CRUD)
 * - Realtime: For real-time subscriptions and live updates
 * - Auth: For user authentication (email/password, OAuth)
 * - Storage: For file uploads (photos)
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    /**
     * Provides a singleton Supabase client instance.
     *
     * The client is configured with the URL and anon key from BuildConfig.
     * These values are loaded from local.properties at build time.
     *
     * Auth is configured with automatic session persistence using SharedPreferences.
     */
    @Provides
    @Singleton
    fun provideSupabaseClient(
        @ApplicationContext context: Context,
    ): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Postgrest)
            install(Realtime)
            install(Auth) {
                // Enable automatic session persistence
                // Sessions are saved to SharedPreferences and loaded on app restart
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
            install(Storage)
        }
    }

    /**
     * Provides the Postgrest module for database operations.
     *
     * Use this to perform CRUD operations on the database:
     * - Select, Insert, Update, Delete
     * - Filtering and ordering
     * - RPC (Remote Procedure Calls)
     */
    @Provides
    @Singleton
    fun provideSupabaseDatabase(client: SupabaseClient): Postgrest {
        return client.pluginManager.getPlugin(Postgrest)
    }

    /**
     * Provides the Realtime module for live updates.
     *
     * Use this to subscribe to database changes:
     * - Listen for INSERT, UPDATE, DELETE events
     * - Real-time collaboration
     * - Presence tracking
     */
    @Provides
    @Singleton
    fun provideSupabaseRealtime(client: SupabaseClient): Realtime {
        return client.pluginManager.getPlugin(Realtime)
    }

    /**
     * Provides the Auth module for authentication.
     *
     * Use this to manage user authentication:
     * - Sign up / Sign in with email
     * - OAuth providers (Google, Apple, etc.)
     * - Session management
     * - Password reset
     */
    @Provides
    @Singleton
    fun provideSupabaseAuth(client: SupabaseClient): Auth {
        return client.pluginManager.getPlugin(Auth)
    }

    /**
     * Provides the Storage module for file uploads.
     *
     * Use this to manage file storage:
     * - Upload photos
     * - Generate public URLs
     * - Delete files
     */
    @Provides
    @Singleton
    fun provideSupabaseStorage(client: SupabaseClient): Storage {
        return client.pluginManager.getPlugin(Storage)
    }
}
