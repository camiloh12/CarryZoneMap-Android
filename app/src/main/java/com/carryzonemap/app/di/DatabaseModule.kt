package com.carryzonemap.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.carryzonemap.app.data.local.dao.PinDao
import com.carryzonemap.app.data.local.dao.SyncQueueDao
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
    /**
     * Migration from version 1 to version 2.
     * Adds the sync_queue table for offline-first sync functionality.
     */
    private val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create sync_queue table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        pin_id TEXT NOT NULL,
                        operation_type TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        retry_count INTEGER NOT NULL DEFAULT 0,
                        last_error TEXT
                    )
                    """.trimIndent(),
                )

                // Create index on pin_id for faster queries
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_pin_id ON sync_queue(pin_id)")
            }
        }

    @Provides
    @Singleton
    fun provideCarryZoneDatabase(
        @ApplicationContext context: Context,
    ): CarryZoneDatabase {
        return Room.databaseBuilder(
            context,
            CarryZoneDatabase::class.java,
            CarryZoneDatabase.DATABASE_NAME,
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
    }

    @Provides
    @Singleton
    fun providePinDao(database: CarryZoneDatabase): PinDao {
        return database.pinDao()
    }

    @Provides
    @Singleton
    fun provideSyncQueueDao(database: CarryZoneDatabase): SyncQueueDao {
        return database.syncQueueDao()
    }
}
