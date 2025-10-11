package com.carryzonemap.app.data.repository

import app.cash.turbine.test
import com.carryzonemap.app.data.local.dao.PinDao
import com.carryzonemap.app.data.local.entity.PinEntity
import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.model.PinStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinRepositoryImplTest {
    private lateinit var fakeDao: FakePinDao
    private lateinit var repository: PinRepositoryImpl

    private val testPin =
        Pin(
            id = "test-123",
            location = Location(latitude = 40.7128, longitude = -74.0060),
            status = PinStatus.ALLOWED,
        )

    @Before
    fun setup() {
        fakeDao = FakePinDao()
        repository = PinRepositoryImpl(fakeDao)
    }

    @Test
    fun `getAllPins returns empty list initially`() =
        runTest {
            repository.getAllPins().test {
                val pins = awaitItem()
                assertEquals(0, pins.size)
            }
        }

    @Test
    fun `addPin inserts pin and emits in getAllPins flow`() =
        runTest {
            repository.getAllPins().test {
                val initialPins = awaitItem()
                assertEquals(0, initialPins.size)

                repository.addPin(testPin)

                val updatedPins = awaitItem()
                assertEquals(1, updatedPins.size)
                assertEquals(testPin.id, updatedPins[0].id)
            }
        }

    @Test
    fun `getPinById returns null when pin does not exist`() =
        runTest {
            val pin = repository.getPinById("nonexistent")
            assertNull(pin)
        }

    @Test
    fun `getPinById returns pin when it exists`() =
        runTest {
            repository.addPin(testPin)

            val pin = repository.getPinById(testPin.id)

            assertNotNull(pin)
            assertEquals(testPin.id, pin!!.id)
            assertEquals(testPin.location, pin.location)
            assertEquals(testPin.status, pin.status)
        }

    @Test
    fun `updatePin updates existing pin`() =
        runTest {
            repository.addPin(testPin)

            val updatedPin = testPin.copy(status = PinStatus.NO_GUN)
            repository.updatePin(updatedPin)

            val retrievedPin = repository.getPinById(testPin.id)

            assertNotNull(retrievedPin)
            assertEquals(PinStatus.NO_GUN, retrievedPin!!.status)
        }

    @Test
    fun `deletePin removes pin from database`() =
        runTest {
            repository.addPin(testPin)

            val pinBeforeDelete = repository.getPinById(testPin.id)
            assertNotNull(pinBeforeDelete)

            repository.deletePin(testPin)

            val pinAfterDelete = repository.getPinById(testPin.id)
            assertNull(pinAfterDelete)
        }

    @Test
    fun `deleteAllPins removes all pins`() =
        runTest {
            repository.addPin(testPin)
            repository.addPin(testPin.copy(id = "test-456"))

            repository.deleteAllPins()

            val count = repository.getPinCount()
            assertEquals(0, count)
        }

    @Test
    fun `getPinCount returns correct count`() =
        runTest {
            assertEquals(0, repository.getPinCount())

            repository.addPin(testPin)
            assertEquals(1, repository.getPinCount())

            repository.addPin(testPin.copy(id = "test-456"))
            assertEquals(2, repository.getPinCount())
        }

    @Test
    fun `cyclePinStatus updates pin status correctly`() =
        runTest {
            repository.addPin(testPin) // ALLOWED

            val success1 = repository.cyclePinStatus(testPin.id)
            assertTrue(success1)

            val pin1 = repository.getPinById(testPin.id)
            assertEquals(PinStatus.UNCERTAIN, pin1!!.status)

            val success2 = repository.cyclePinStatus(testPin.id)
            assertTrue(success2)

            val pin2 = repository.getPinById(testPin.id)
            assertEquals(PinStatus.NO_GUN, pin2!!.status)

            val success3 = repository.cyclePinStatus(testPin.id)
            assertTrue(success3)

            val pin3 = repository.getPinById(testPin.id)
            assertEquals(PinStatus.ALLOWED, pin3!!.status)
        }

    @Test
    fun `cyclePinStatus returns false when pin does not exist`() =
        runTest {
            val success = repository.cyclePinStatus("nonexistent")
            assertFalse(success)
        }

    @Test
    fun `getAllPins emits updates when pins are added`() =
        runTest {
            repository.getAllPins().test {
                awaitItem() // Initial empty list

                repository.addPin(testPin)
                val pins1 = awaitItem()
                assertEquals(1, pins1.size)

                repository.addPin(testPin.copy(id = "test-456"))
                val pins2 = awaitItem()
                assertEquals(2, pins2.size)
            }
        }

    @Test
    fun `getAllPins emits updates when pins are deleted`() =
        runTest {
            repository.addPin(testPin)

            repository.getAllPins().test {
                val pins1 = awaitItem()
                assertEquals(1, pins1.size)

                repository.deletePin(testPin)
                val pins2 = awaitItem()
                assertEquals(0, pins2.size)
            }
        }

    /**
     * Fake DAO implementation for testing.
     * Uses in-memory storage with MutableStateFlow to simulate Room's reactive behavior.
     */
    private class FakePinDao : PinDao {
        private val pins = mutableMapOf<String, PinEntity>()
        private val pinsFlow = MutableStateFlow<List<PinEntity>>(emptyList())

        override fun getAllPins(): Flow<List<PinEntity>> = pinsFlow

        override suspend fun getPinById(pinId: String): PinEntity? = pins[pinId]

        override suspend fun insertPin(pin: PinEntity) {
            pins[pin.id] = pin
            emitPins()
        }

        override suspend fun insertPins(pins: List<PinEntity>) {
            pins.forEach { this.pins[it.id] = it }
            emitPins()
        }

        override suspend fun updatePin(pin: PinEntity) {
            if (pins.containsKey(pin.id)) {
                pins[pin.id] = pin
                emitPins()
            }
        }

        override suspend fun deletePin(pin: PinEntity) {
            pins.remove(pin.id)
            emitPins()
        }

        override suspend fun deleteAllPins() {
            pins.clear()
            emitPins()
        }

        override suspend fun getPinCount(): Int = pins.size

        private fun emitPins() {
            pinsFlow.update { pins.values.toList().sortedByDescending { it.createdAt } }
        }
    }
}
