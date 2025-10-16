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
     * Adds createdBy field to pins table.
     */
    private val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add createdBy column to pins table
                db.execSQL("ALTER TABLE pins ADD COLUMN createdBy TEXT")

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

    /**
     * Migration from version 2 to version 3.
     * No-op migration to force schema consistency check and recreation
     * for databases that were created with incomplete v2 migration.
     */
    private val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No changes needed - this migration exists to trigger
                // fallbackToDestructiveMigration for inconsistent v2 databases
            }
        }

    /**
     * Migration from version 3 to version 4.
     * Adds name column to pins table for storing POI names.
     */
    private val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add name column to pins table with default empty string for existing pins
                db.execSQL("ALTER TABLE pins ADD COLUMN name TEXT NOT NULL DEFAULT ''")
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
