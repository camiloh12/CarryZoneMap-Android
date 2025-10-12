package com.carryzonemap.app.data.sync

/**
 * Represents a pending sync operation to be executed when online.
 *
 * These operations are queued in the local database and processed
 * when network connectivity is available.
 */
sealed class SyncOperation {
    abstract val pinId: String
    abstract val timestamp: Long

    /**
     * Create a new pin on the remote server.
     */
    data class Create(
        override val pinId: String,
        override val timestamp: Long,
    ) : SyncOperation()

    /**
     * Update an existing pin on the remote server.
     */
    data class Update(
        override val pinId: String,
        override val timestamp: Long,
    ) : SyncOperation()

    /**
     * Delete a pin from the remote server.
     */
    data class Delete(
        override val pinId: String,
        override val timestamp: Long,
    ) : SyncOperation()

    companion object {
        const val TYPE_CREATE = "CREATE"
        const val TYPE_UPDATE = "UPDATE"
        const val TYPE_DELETE = "DELETE"

        fun fromTypeString(
            type: String,
            pinId: String,
            timestamp: Long,
        ): SyncOperation =
            when (type) {
                TYPE_CREATE -> Create(pinId, timestamp)
                TYPE_UPDATE -> Update(pinId, timestamp)
                TYPE_DELETE -> Delete(pinId, timestamp)
                else -> throw IllegalArgumentException("Unknown sync operation type: $type")
            }

        fun SyncOperation.toTypeString(): String =
            when (this) {
                is Create -> TYPE_CREATE
                is Update -> TYPE_UPDATE
                is Delete -> TYPE_DELETE
            }
    }
}
