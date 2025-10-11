package com.carryzonemap.app.data.repository

import com.carryzonemap.app.data.local.dao.PinDao
import com.carryzonemap.app.data.mapper.EntityMapper.toDomain
import com.carryzonemap.app.data.mapper.EntityMapper.toDomainModels
import com.carryzonemap.app.data.mapper.EntityMapper.toEntity
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.repository.PinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PinRepository using Room database as the data source.
 *
 * @property pinDao The Data Access Object for pin database operations
 */
@Singleton
class PinRepositoryImpl
    @Inject
    constructor(
        private val pinDao: PinDao,
    ) : PinRepository {
        override fun getAllPins(): Flow<List<Pin>> {
            return pinDao.getAllPins().map { entities ->
                entities.toDomainModels()
            }
        }

        override suspend fun getPinById(pinId: String): Pin? {
            return pinDao.getPinById(pinId)?.toDomain()
        }

        override suspend fun addPin(pin: Pin) {
            pinDao.insertPin(pin.toEntity())
        }

        override suspend fun updatePin(pin: Pin) {
            pinDao.updatePin(pin.toEntity())
        }

        override suspend fun deletePin(pin: Pin) {
            pinDao.deletePin(pin.toEntity())
        }

        override suspend fun deleteAllPins() {
            pinDao.deleteAllPins()
        }

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
