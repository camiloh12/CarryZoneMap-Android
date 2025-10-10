package com.carryzonemap.app.di

import com.carryzonemap.app.data.repository.PinRepositoryImpl
import com.carryzonemap.app.domain.repository.PinRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing repository bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds the PinRepositoryImpl to the PinRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindPinRepository(
        pinRepositoryImpl: PinRepositoryImpl
    ): PinRepository
}
