package com.carryzonemap.app.di

import com.carryzonemap.app.data.sync.SyncManager
import com.carryzonemap.app.data.sync.SyncManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing synchronization-related dependencies.
 *
 * This module binds the SyncManager interface to its implementation,
 * enabling dependency injection of sync functionality throughout the app.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    /**
     * Binds SyncManager interface to SyncManagerImpl.
     *
     * This allows components to depend on the SyncManager interface
     * while Hilt provides the concrete implementation.
     */
    @Binds
    @Singleton
    abstract fun bindSyncManager(impl: SyncManagerImpl): SyncManager
}
