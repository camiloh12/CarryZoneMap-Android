package com.carryzonemap.app.di

import android.content.Context
import androidx.room.Room
import com.carryzonemap.app.data.local.dao.PinDao
import com.carryzonemap.app.data.local.database.CarryZoneDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCarryZoneDatabase(
        @ApplicationContext context: Context
    ): CarryZoneDatabase {
        return Room.databaseBuilder(
            context,
            CarryZoneDatabase::class.java,
            CarryZoneDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
    }

    @Provides
    @Singleton
    fun providePinDao(database: CarryZoneDatabase): PinDao {
        return database.pinDao()
    }
}
