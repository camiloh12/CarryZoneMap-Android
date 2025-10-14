package com.carryzonemap.app.data.repository

import com.carryzonemap.app.data.local.dao.PinDao
import com.carryzonemap.app.data.mapper.EntityMapper.toDomain
import com.carryzonemap.app.data.mapper.EntityMapper.toDomainModels
import com.carryzonemap.app.data.mapper.EntityMapper.toEntity
import com.carryzonemap.app.data.sync.SyncManager
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.repository.PinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PinRepository using hybrid offline-first architecture.
 *
 * Operations follow this pattern:
 * 1. Write to local database (Room) immediately - provides instant UI updates
 * 2. Queue operation for remote sync (Supabase) - syncs when online
 *
 * This ensures the app works offline while maintaining data consistency
 * across devices when online.
 *
 * @property pinDao The Data Access Object for pin database operations
 * @property syncManager Manages synchronization with remote server
 */
@Singleton
class PinRepositoryImpl
    @Inject
    constructor(
        private val pinDao: PinDao,
        private val syncManager: SyncManager,
    ) : PinRepository {

        /**
         * Reads from local database for instant UI updates.
         * Changes from remote sync will automatically flow through via Room's reactive queries.
         */
        override fun getAllPins(): Flow<List<Pin>> {
            return pinDao.getAllPins().map { entities ->
                entities.toDomainModels()
            }
        }

        override suspend fun getPinById(pinId: String): Pin? {
            return pinDao.getPinById(pinId)?.toDomain()
        }

        /**
         * Adds a pin using offline-first pattern:
         * 1. Save to local database immediately
         * 2. Queue for remote upload
         * 3. Trigger immediate sync if online
         */
        override suspend fun addPin(pin: Pin) {
            Timber.d("Adding pin: ${pin.id} at (${pin.location.longitude}, ${pin.location.latitude})")

            // Step 1: Save to local database immediately (optimistic update)
            pinDao.insertPin(pin.toEntity())
            Timber.d("Pin saved to local database: ${pin.id}")

            // Step 2: Queue for remote sync
            syncManager.queuePinForUpload(pin)
            Timber.d("Pin queued for upload: ${pin.id}")

            // Step 3: Trigger immediate sync (will only sync if online)
            val syncResult = syncManager.syncWithRemote()
            if (syncResult.isSuccess) {
                Timber.d("Immediate sync succeeded for pin: ${pin.id}")
            } else {
                Timber.w("Immediate sync failed for pin: ${pin.id}, will retry later: ${syncResult.exceptionOrNull()?.message}")
            }
        }

        /**
         * Updates a pin using offline-first pattern:
         * 1. Update local database immediately
         * 2. Queue for remote sync
         */
        override suspend fun updatePin(pin: Pin) {
            Timber.d("Updating pin: ${pin.id}")

            // Step 1: Update local database immediately
            pinDao.updatePin(pin.toEntity())

            // Step 2: Queue for remote sync
            syncManager.queuePinForUpdate(pin)
        }

        /**
         * Deletes a pin using offline-first pattern:
         * 1. Delete from local database immediately
         * 2. Queue for remote deletion
         */
        override suspend fun deletePin(pin: Pin) {
            Timber.d("Deleting pin: ${pin.id}")

            // Step 1: Delete from local database immediately
            pinDao.deletePin(pin.toEntity())

            // Step 2: Queue for remote deletion
            syncManager.queuePinForDeletion(pin.id)
        }

        /**
         * Deletes all pins from local database.
         * Note: This does NOT sync with remote (useful for testing/reset).
         */
        override suspend fun deleteAllPins() {
            Timber.d("Deleting all pins (local only)")
            pinDao.deleteAllPins()
        }

        /**
         * Cycles a pin's status through the available states.
         * Updates are synced to remote.
         */
        override suspend fun cyclePinStatus(pinId: String): Boolean {
            val pin = getPinById(pinId) ?: return false
            val updatedPin = pin.withNextStatus()
            updatePin(updatedPin)
            return true
        }

        override suspend fun getPinCount(): Int {
            return pinDao.getPinCount()
        }
    }
