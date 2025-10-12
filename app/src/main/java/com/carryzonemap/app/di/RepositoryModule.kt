package com.carryzonemap.app.di

import com.carryzonemap.app.data.remote.datasource.RemotePinDataSource
import com.carryzonemap.app.data.remote.datasource.SupabasePinDataSource
import com.carryzonemap.app.data.repository.PinRepositoryImpl
import com.carryzonemap.app.data.repository.SupabaseAuthRepository
import com.carryzonemap.app.domain.repository.AuthRepository
import com.carryzonemap.app.domain.repository.PinRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing repository and data source bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    /**
     * Binds the PinRepositoryImpl to the PinRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindPinRepository(pinRepositoryImpl: PinRepositoryImpl): PinRepository

    /**
     * Binds the SupabaseAuthRepository to the AuthRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(supabaseAuthRepository: SupabaseAuthRepository): AuthRepository

    /**
     * Binds the SupabasePinDataSource to the RemotePinDataSource interface.
     */
    @Binds
    @Singleton
    abstract fun bindRemotePinDataSource(supabasePinDataSource: SupabasePinDataSource): RemotePinDataSource
}
