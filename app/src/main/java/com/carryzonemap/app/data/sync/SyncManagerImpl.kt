package com.carryzonemap.app.data.sync

import android.util.Log
import com.carryzonemap.app.data.local.dao.PinDao
import com.carryzonemap.app.data.local.dao.SyncQueueDao
import com.carryzonemap.app.data.local.entity.SyncQueueEntity
import com.carryzonemap.app.data.mapper.EntityMapper.toDomain
import com.carryzonemap.app.data.mapper.EntityMapper.toEntity
import com.carryzonemap.app.data.network.NetworkMonitor
import com.carryzonemap.app.data.remote.datasource.RemotePinDataSource
import com.carryzonemap.app.data.sync.SyncOperation.Companion.toTypeString
import com.carryzonemap.app.domain.model.Pin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
            private const val TAG = "SyncManager"
            private const val MAX_RETRIES = 3
        }

        private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
        override val syncStatus: Flow<SyncStatus> = _syncStatus.asStateFlow()

        override suspend fun queuePinForUpload(pin: Pin) {
            Log.d(TAG, "Queueing pin for upload: ${pin.id}")
            val operation =
                SyncQueueEntity(
                    pinId = pin.id,
                    operationType = SyncOperation.TYPE_CREATE,
                    timestamp = System.currentTimeMillis(),
                )
            syncQueueDao.insertOperation(operation)
        }

        override suspend fun queuePinForUpdate(pin: Pin) {
            Log.d(TAG, "Queueing pin for update: ${pin.id}")
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
            Log.d(TAG, "Queueing pin for deletion: $pinId")
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
            Log.d(TAG, "Starting sync with remote")

            // Check if we're online
            if (!networkMonitor.isOnline.first()) {
                Log.w(TAG, "Sync skipped: device is offline")
                _syncStatus.value = SyncStatus.Error("Device is offline", retryable = true)
                return Result.failure(Exception("Device is offline"))
            }

            return try {
                _syncStatus.value = SyncStatus.Syncing(syncQueueDao.getOperationCount())

                // Step 1: Upload pending operations
                val uploadCount = uploadPendingOperations()

                // Step 2: Download remote changes
                val downloadCount = downloadRemoteChanges()

                Log.d(TAG, "Sync completed: uploaded=$uploadCount, downloaded=$downloadCount")
                _syncStatus.value = SyncStatus.Success(uploadCount, downloadCount)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                _syncStatus.value = SyncStatus.Error(e.message ?: "Unknown error", retryable = true)
                Result.failure(e)
            }
        }

        override suspend fun getPendingOperationCount(): Int {
            return syncQueueDao.getOperationCount()
        }

        override suspend fun clearQueue() {
            Log.d(TAG, "Clearing sync queue")
            syncQueueDao.deleteAllOperations()
        }

        /**
         * Upload all pending operations from the queue to the remote server.
         *
         * @return Number of operations successfully uploaded
         */
        private suspend fun uploadPendingOperations(): Int {
            val operations = syncQueueDao.getAllOperations()
            Log.d(TAG, "Uploading ${operations.size} pending operations")

            var successCount = 0

            for (operation in operations) {
                try {
                    when (operation.operationType) {
                        SyncOperation.TYPE_CREATE -> {
                            val pin = pinDao.getPinById(operation.pinId)?.toDomain()
                            if (pin != null) {
                                remoteDataSource.insertPin(pin).getOrThrow()
                                syncQueueDao.deleteOperation(operation.id)
                                successCount++
                                Log.d(TAG, "Uploaded CREATE for pin: ${pin.id}")
                            } else {
                                // Pin no longer exists locally, remove from queue
                                syncQueueDao.deleteOperation(operation.id)
                                Log.w(TAG, "Pin ${operation.pinId} not found locally, removing from queue")
                            }
                        }

                        SyncOperation.TYPE_UPDATE -> {
                            val pin = pinDao.getPinById(operation.pinId)?.toDomain()
                            if (pin != null) {
                                remoteDataSource.updatePin(pin).getOrThrow()
                                syncQueueDao.deleteOperation(operation.id)
                                successCount++
                                Log.d(TAG, "Uploaded UPDATE for pin: ${pin.id}")
                            } else {
                                // Pin no longer exists locally, remove from queue
                                syncQueueDao.deleteOperation(operation.id)
                                Log.w(TAG, "Pin ${operation.pinId} not found locally, removing from queue")
                            }
                        }

                        SyncOperation.TYPE_DELETE -> {
                            remoteDataSource.deletePin(operation.pinId).getOrThrow()
                            syncQueueDao.deleteOperation(operation.id)
                            successCount++
                            Log.d(TAG, "Uploaded DELETE for pin: ${operation.pinId}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload operation for pin ${operation.pinId}", e)

                    // Update retry count
                    val updatedOperation =
                        operation.copy(
                            retryCount = operation.retryCount + 1,
                            lastError = e.message,
                        )
                    syncQueueDao.updateOperation(updatedOperation)

                    // If max retries reached, remove from queue (could also implement exponential backoff)
                    if (updatedOperation.retryCount >= MAX_RETRIES) {
                        Log.w(TAG, "Max retries reached for pin ${operation.pinId}, removing from queue")
                        syncQueueDao.deleteOperation(operation.id)
                    }
                }
            }

            return successCount
        }

        /**
         * Download all pins from the remote server and merge with local data.
         *
         * Uses last-write-wins conflict resolution based on lastModified timestamp.
         *
         * @return Number of pins downloaded and merged
         */
        private suspend fun downloadRemoteChanges(): Int {
            Log.d(TAG, "Downloading remote changes")

            val result = remoteDataSource.getAllPins()
            if (result.isFailure) {
                Log.e(TAG, "Failed to download remote pins", result.exceptionOrNull())
                return 0
            }

            val remotePins = result.getOrNull() ?: emptyList()
            Log.d(TAG, "Downloaded ${remotePins.size} pins from remote")

            var mergedCount = 0

            for (remotePin in remotePins) {
                try {
                    val localPin = pinDao.getPinById(remotePin.id)?.toDomain()

                    if (localPin == null) {
                        // Pin doesn't exist locally, insert it
                        pinDao.insertPin(remotePin.toEntity())
                        mergedCount++
                        Log.d(TAG, "Inserted new pin from remote: ${remotePin.id}")
                    } else {
                        // Pin exists locally, resolve conflict using last-write-wins
                        if (remotePin.metadata.lastModified > localPin.metadata.lastModified) {
                            // Remote is newer, update local
                            pinDao.updatePin(remotePin.toEntity())
                            mergedCount++
                            Log.d(TAG, "Updated pin from remote (newer): ${remotePin.id}")
                        } else {
                            // Local is newer or same, keep local version
                            // (It should already be in the upload queue if modified)
                            Log.d(TAG, "Kept local pin (newer or same): ${remotePin.id}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to merge pin ${remotePin.id}", e)
                }
            }

            return mergedCount
        }
    }
