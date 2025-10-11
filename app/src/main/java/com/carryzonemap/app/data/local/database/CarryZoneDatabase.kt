package com.carryzonemap.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.carryzonemap.app.data.local.dao.PinDao
import com.carryzonemap.app.data.local.entity.PinEntity

/**
 * Room database for CarryZoneMap application.
 *
 * Version 1: Initial schema with pins table
 */
@Database(
    entities = [PinEntity::class],
    version = 1,
    exportSchema = false, // Set to false for now; enable later with proper schema location
)
abstract class CarryZoneDatabase : RoomDatabase() {
    /**
     * Provides access to pin database operations.
     */
    abstract fun pinDao(): PinDao

    companion object {
        const val DATABASE_NAME = "carry_zone_db"
    }
}
