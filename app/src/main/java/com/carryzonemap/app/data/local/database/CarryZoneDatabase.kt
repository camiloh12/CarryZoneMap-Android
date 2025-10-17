package com.carryzonemap.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.carryzonemap.app.data.local.dao.PinDao
import com.carryzonemap.app.data.local.dao.SyncQueueDao
import com.carryzonemap.app.data.local.entity.PinEntity
import com.carryzonemap.app.data.local.entity.SyncQueueEntity

/**
 * Room database for CarryZoneMap application.
 *
 * Version 1: Initial schema with pins table
 * Version 2: Added sync_queue table and created_by field to pins
 * Version 3: Fixed migration - ensuring schema consistency
 * Version 4: Added name field to pins table for POI names
 */
@Database(
    entities = [PinEntity::class, SyncQueueEntity::class],
    version = 4,
    // Set to false for now; enable later with proper schema location
    exportSchema = false,
)
abstract class CarryZoneDatabase : RoomDatabase() {
    /**
     * Provides access to pin database operations.
     */
    abstract fun pinDao(): PinDao

    /**
     * Provides access to sync queue operations.
     */
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        const val DATABASE_NAME = "carry_zone_db"
    }
}
