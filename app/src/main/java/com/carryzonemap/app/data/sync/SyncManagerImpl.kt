package com.carryzonemap.app.data.sync

import com.carryzonemap.app.data.local.dao.PinDao
import com.carryzonemap.app.data.local.dao.SyncQueueDao
import com.carryzonemap.app.data.local.entity.SyncQueueEntity
import com.carryzonemap.app.data.mapper.EntityMapper.toDomain
import com.carryzonemap.app.data.mapper.EntityMapper.toEntity
import com.carryzonemap.app.data.network.NetworkMonitor
import com.carryzonemap.app.data.remote.datasource.PinChangeEvent
import com.carryzonemap.app.data.remote.datasource.RemotePinDataSource
import com.carryzonemap.app.domain.model.Pin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SyncManager for offline-first synchronization.
 *
 * This class coordinates between local (Room) and remote (Supabase) data sources,
 * ensuring data consistency across devices while maintaining offline functionality.
 *
 * @property pinDao Local database DAO for pins
 * @property syncQueueDao Local database DAO for sync queue
 * @property remoteDataSource Remote data source (Supabase)
 * @property networkMonitor Network connectivity monitor
 */
@Singleton
class SyncManagerImpl
    @Inject
    constructor(
        private val pinDao: PinDao,
        private val syncQueueDao: SyncQueueDao,
        private val remoteDataSource: RemotePinDataSource,
        private val networkMonitor: NetworkMonitor,
    ) : SyncManager {
        companion object {
            private const val MAX_RETRIES = 3
        }

        private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
        override val syncStatus: Flow<SyncStatus> = _syncStatus.asStateFlow()

        override suspend fun queuePinForUpload(pin: Pin) {
            Timber.d("Queueing pin for upload: ${pin.id}")
            val operation =
                SyncQueueEntity(
                    pinId = pin.id,
                    operationType = SyncOperation.TYPE_CREATE,
                    timestamp = System.currentTimeMillis(),
                )
            syncQueueDao.insertOperation(operation)
            val queueCount = syncQueueDao.getOperationCount()
            Timber.d("Pin queued successfully. Total operations in queue: $queueCount")
        }

        override suspend fun queuePinForUpdate(pin: Pin) {
            Timber.d("Queueing pin for update: ${pin.id}")
            // Remove any existing operations for this pin
            syncQueueDao.deleteOperationsForPin(pin.id)
            // Queue the update
            val operation =
                SyncQueueEntity(
                    pinId = pin.id,
                    operationType = SyncOperation.TYPE_UPDATE,
                    timestamp = System.currentTimeMillis(),
                )
            syncQueueDao.insertOperation(operation)
        }

        override suspend fun queuePinForDeletion(pinId: String) {
            Timber.d("Queueing pin for deletion: $pinId")
            // Remove any existing operations for this pin
            syncQueueDao.deleteOperationsForPin(pinId)
            // Queue the deletion
            val operation =
                SyncQueueEntity(
                    pinId = pinId,
                    operationType = SyncOperation.TYPE_DELETE,
                    timestamp = System.currentTimeMillis(),
                )
            syncQueueDao.insertOperation(operation)
        }

        override suspend fun syncWithRemote(): Result<Unit> {
            val pendingCount = syncQueueDao.getOperationCount()
            Timber.d("=== SYNC STARTED === Pending operations: $pendingCount")

            // Check if we're online
            val isOnline = networkMonitor.isOnline.first()
            Timber.d("Network status: ${if (isOnline) "ONLINE" else "OFFLINE"}")

            if (!isOnline) {
                Timber.w("Sync skipped: device is offline")
                _syncStatus.value = SyncStatus.Error("Device is offline", retryable = true)
                return Result.failure(Exception("Device is offline"))
            }

            return try {
                _syncStatus.value = SyncStatus.Syncing(pendingCount)

                // Step 1: Upload pending operations
                Timber.d("Phase 1: Uploading $pendingCount pending operations...")
                val uploadCount = uploadPendingOperations()
                Timber.d("Phase 1 complete: $uploadCount operations uploaded successfully")

                // Step 2: Download remote changes
                Timber.d("Phase 2: Downloading remote changes...")
                val downloadCount = downloadRemoteChanges()
                Timber.d("Phase 2 complete: $downloadCount pins downloaded/merged")

                Timber.d("=== SYNC COMPLETED === Uploaded: $uploadCount, Downloaded: $downloadCount")
                _syncStatus.value = SyncStatus.Success(uploadCount, downloadCount)
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "=== SYNC FAILED === Error: ${e.message}")
                _syncStatus.value = SyncStatus.Error(e.message ?: "Unknown error", retryable = true)
                Result.failure(e)
            }
        }

        override suspend fun getPendingOperationCount(): Int {
            return syncQueueDao.getOperationCount()
        }

        override suspend fun clearQueue() {
            Timber.d("Clearing sync queue")
            syncQueueDao.deleteAllOperations()
        }

        /**
         * Upload all pending operations from the queue to the remote server.
         *
         * @return Number of operations successfully uploaded
         */
        private suspend fun uploadPendingOperations(): Int {
            val operations = syncQueueDao.getAllOperations()
            Timber.d("Uploading ${operations.size} pending operations")

            var successCount = 0

            for (operation in operations) {
                try {
                    val uploaded = processUploadOperation(operation)
                    if (uploaded) successCount++
                } catch (e: Exception) {
                    handleUploadFailure(operation, e)
                }
            }

            return successCount
        }

        /**
         * Process a single upload operation.
         *
         * @return true if the operation was successfully uploaded
         */
        private suspend fun processUploadOperation(operation: SyncQueueEntity): Boolean {
            return when (operation.operationType) {
                SyncOperation.TYPE_CREATE -> uploadCreateOperation(operation)
                SyncOperation.TYPE_UPDATE -> uploadUpdateOperation(operation)
                SyncOperation.TYPE_DELETE -> uploadDeleteOperation(operation)
                else -> false
            }
        }

        private suspend fun uploadCreateOperation(operation: SyncQueueEntity): Boolean {
            val pin = pinDao.getPinById(operation.pinId)?.toDomain()
            return if (pin != null) {
                remoteDataSource.insertPin(pin).getOrThrow()
                syncQueueDao.deleteOperation(operation.id)
                Timber.d("Uploaded CREATE for pin: ${pin.id}")
                true
            } else {
                syncQueueDao.deleteOperation(operation.id)
                Timber.w("Pin ${operation.pinId} not found locally, removing from queue")
                false
            }
        }

        private suspend fun uploadUpdateOperation(operation: SyncQueueEntity): Boolean {
            val pin = pinDao.getPinById(operation.pinId)?.toDomain()
            return if (pin != null) {
                remoteDataSource.updatePin(pin).getOrThrow()
                syncQueueDao.deleteOperation(operation.id)
                Timber.d("Uploaded UPDATE for pin: ${pin.id}")
                true
            } else {
                syncQueueDao.deleteOperation(operation.id)
                Timber.w("Pin ${operation.pinId} not found locally, removing from queue")
                false
            }
        }

        private suspend fun uploadDeleteOperation(operation: SyncQueueEntity): Boolean {
            remoteDataSource.deletePin(operation.pinId).getOrThrow()
            syncQueueDao.deleteOperation(operation.id)
            Timber.d("Uploaded DELETE for pin: ${operation.pinId}")
            return true
        }

        private suspend fun handleUploadFailure(
            operation: SyncQueueEntity,
            error: Exception,
        ) {
            Timber.e(error, "Failed to upload operation for pin ${operation.pinId}")

            val updatedOperation =
                operation.copy(
                    retryCount = operation.retryCount + 1,
                    lastError = error.message,
                )
            syncQueueDao.updateOperation(updatedOperation)

            if (updatedOperation.retryCount >= MAX_RETRIES) {
                Timber.w("Max retries reached for pin ${operation.pinId}, removing from queue")
                syncQueueDao.deleteOperation(operation.id)
            }
        }

        /**
         * Download all pins from the remote server and merge with local data.
         *
         * Uses last-write-wins conflict resolution based on lastModified timestamp.
         *
         * @return Number of pins downloaded and merged
         */
        private suspend fun downloadRemoteChanges(): Int {
            Timber.d("Downloading remote changes")

            val result = remoteDataSource.getAllPins()
            if (result.isFailure) {
                Timber.e(result.exceptionOrNull(), "Failed to download remote pins")
                return 0
            }

            val remotePins = result.getOrNull() ?: emptyList()
            Timber.d("Downloaded ${remotePins.size} pins from remote")

            var mergedCount = 0

            for (remotePin in remotePins) {
                try {
                    if (mergeRemotePin(remotePin)) {
                        mergedCount++
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to merge pin ${remotePin.id}")
                }
            }

            return mergedCount
        }

        /**
         * Merge a remote pin with local data using last-write-wins conflict resolution.
         *
         * @return true if the pin was merged (inserted or updated)
         */
        private suspend fun mergeRemotePin(remotePin: Pin): Boolean {
            val localPin = pinDao.getPinById(remotePin.id)?.toDomain()

            return if (localPin == null) {
                pinDao.insertPin(remotePin.toEntity())
                Timber.d("Inserted new pin from remote: ${remotePin.id}")
                true
            } else {
                mergeExistingPin(remotePin, localPin)
            }
        }

        private suspend fun mergeExistingPin(
            remotePin: Pin,
            localPin: Pin,
        ): Boolean {
            return if (remotePin.metadata.lastModified > localPin.metadata.lastModified) {
                pinDao.updatePin(remotePin.toEntity())
                Timber.d("Updated pin from remote (newer): ${remotePin.id}")
                true
            } else {
                Timber.d("Kept local pin (newer or same): ${remotePin.id}")
                false
            }
        }

        override fun startRealtimeSubscription(): Flow<String> =
            flow {
                Timber.d("Starting real-time subscription to remote changes")
                emit("Real-time subscription started")

                try {
                    remoteDataSource
                        .subscribeToChanges()
                        .collect { event ->
                            try {
                                when (event) {
                                    is PinChangeEvent.Insert -> {
                                        handleRealtimeInsert(event.pin)
                                        emit("Real-time: Inserted pin ${event.pin.id}")
                                    }
                                    is PinChangeEvent.Update -> {
                                        handleRealtimeUpdate(event.pin)
                                        emit("Real-time: Updated pin ${event.pin.id}")
                                    }
                                    is PinChangeEvent.Delete -> {
                                        handleRealtimeDelete(event.pinId)
                                        emit("Real-time: Deleted pin ${event.pinId}")
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Error handling real-time event")
                                emit("Real-time: Error - ${e.message}")
                            }
                        }
                } catch (e: Exception) {
                    Timber.e(e, "Real-time subscription error")
                    emit("Real-time subscription error: ${e.message}")
                }
            }

        /**
         * Handle a pin insert from real-time subscription.
         *
         * Only insert if the pin doesn't exist locally (to avoid conflicts with local creates).
         */
        private suspend fun handleRealtimeInsert(pin: Pin) {
            Timber.d("Real-time INSERT: ${pin.id}")

            val existingPin = pinDao.getPinById(pin.id)?.toDomain()
            if (existingPin == null) {
                // Pin doesn't exist locally, insert it
                pinDao.insertPin(pin.toEntity())
                Timber.d("Inserted pin from real-time: ${pin.id}")
            } else {
                // Pin already exists, treat as update instead
                Timber.d("Pin ${pin.id} already exists locally, treating as update")
                handleRealtimeUpdate(pin)
            }
        }

        /**
         * Handle a pin update from real-time subscription.
         *
         * Uses last-write-wins conflict resolution.
         */
        private suspend fun handleRealtimeUpdate(pin: Pin) {
            Timber.d("Real-time UPDATE: ${pin.id}")

            val localPin = pinDao.getPinById(pin.id)?.toDomain()
            if (localPin == null) {
                // Pin doesn't exist locally, insert it
                pinDao.insertPin(pin.toEntity())
                Timber.d("Inserted pin from real-time update: ${pin.id}")
            } else {
                // Resolve conflict: last-write-wins
                if (pin.metadata.lastModified > localPin.metadata.lastModified) {
                    // Remote is newer, update local
                    pinDao.updatePin(pin.toEntity())
                    Timber.d("Updated pin from real-time (newer): ${pin.id}")
                } else {
                    // Local is newer, keep local version
                    Timber.d("Kept local pin (newer): ${pin.id}")
                }
            }
        }

        /**
         * Handle a pin deletion from real-time subscription.
         *
         * Delete the pin locally if it exists.
         */
        private suspend fun handleRealtimeDelete(pinId: String) {
            Timber.d("Real-time DELETE: $pinId")

            val localPin = pinDao.getPinById(pinId)
            if (localPin != null) {
                pinDao.deletePin(localPin)
                Timber.d("Deleted pin from real-time: $pinId")
            } else {
                Timber.d("Pin $pinId doesn't exist locally, nothing to delete")
            }
        }
    }
