package com.carryzonemap.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a pending sync operation.
 *
 * Stores operations that need to be executed on the remote server
 * when network connectivity is available.
 *
 * @property id Auto-generated primary key
 * @property pinId ID of the pin to sync
 * @property operationType Type of operation (CREATE, UPDATE, DELETE)
 * @property timestamp When the operation was queued (epoch milliseconds)
 * @property retryCount Number of times this operation has been retried
 * @property lastError Error message from last failed attempt (if any)
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "pin_id")
    val pinId: String,
    // CREATE, UPDATE, DELETE
    @ColumnInfo(name = "operation_type")
    val operationType: String,
    // Epoch milliseconds when queued
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
)
