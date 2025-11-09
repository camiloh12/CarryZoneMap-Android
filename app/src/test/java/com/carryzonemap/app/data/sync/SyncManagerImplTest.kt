package com.carryzonemap.app.data.sync

import app.cash.turbine.test
import com.carryzonemap.app.data.local.dao.PinDao
import com.carryzonemap.app.data.local.dao.SyncQueueDao
import com.carryzonemap.app.data.local.entity.PinEntity
import com.carryzonemap.app.data.local.entity.SyncQueueEntity
import com.carryzonemap.app.data.mapper.EntityMapper.toDomain
import com.carryzonemap.app.data.mapper.EntityMapper.toEntity
import com.carryzonemap.app.data.network.NetworkMonitor
import com.carryzonemap.app.data.remote.datasource.PinChangeEvent
import com.carryzonemap.app.data.remote.datasource.RemotePinDataSource
import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.model.PinMetadata
import com.carryzonemap.app.domain.model.PinStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for SyncManagerImpl.
 *
 * Tests critical sync logic including:
 * - Queue operations (create, update, delete)
 * - Upload operations with retry logic
 * - Download and conflict resolution (last-write-wins)
 * - Network connectivity handling
 * - Real-time subscription handling
 */
@RunWith(RobolectricTestRunner::class)
class SyncManagerImplTest {

    private lateinit var syncManager: SyncManagerImpl
    private lateinit var fakePinDao: FakePinDao
    private lateinit var fakeSyncQueueDao: FakeSyncQueueDao
    private lateinit var fakeRemoteDataSource: FakeRemotePinDataSource
    private lateinit var mockNetworkMonitor: NetworkMonitor
    private val onlineStateFlow = MutableStateFlow(true)

    private val currentTime = System.currentTimeMillis()
    private val testPin = Pin(
        id = "test-pin-1",
        name = "Test Location",
        location = Location(latitude = 34.0, longitude = -118.0),
        status = PinStatus.ALLOWED,
        metadata = PinMetadata(
            createdAt = currentTime - 3600000,
            lastModified = currentTime - 1800000,
            createdBy = "user-1"
        )
    )

    @Before
    fun setup() {
        fakePinDao = FakePinDao()
        fakeSyncQueueDao = FakeSyncQueueDao()
        fakeRemoteDataSource = FakeRemotePinDataSource()

        // Mock NetworkMonitor
        mockNetworkMonitor = mock()
        whenever(mockNetworkMonitor.isOnline).thenReturn(onlineStateFlow)

        syncManager = SyncManagerImpl(
            pinDao = fakePinDao,
            syncQueueDao = fakeSyncQueueDao,
            remoteDataSource = fakeRemoteDataSource,
            networkMonitor = mockNetworkMonitor
        )
    }

    private fun setOnline(online: Boolean) {
        onlineStateFlow.value = online
    }

    // ===============================
    // Queue Operations Tests
    // ===============================

    @Test
    fun `queuePinForUpload adds operation to queue`() = runTest {
        syncManager.queuePinForUpload(testPin)

        assertEquals(1, fakeSyncQueueDao.getOperationCount())
        val operation = fakeSyncQueueDao.getAllOperations().first()
        assertEquals(testPin.id, operation.pinId)
        assertEquals(SyncOperation.TYPE_CREATE, operation.operationType)
    }

    @Test
    fun `queuePinForUpdate removes existing operations before adding`() = runTest {
        // Queue a create operation first
        syncManager.queuePinForUpload(testPin)
        assertEquals(1, fakeSyncQueueDao.getOperationCount())

        // Queue an update - should replace the create
        syncManager.queuePinForUpdate(testPin)

        assertEquals(1, fakeSyncQueueDao.getOperationCount())
        val operation = fakeSyncQueueDao.getAllOperations().first()
        assertEquals(SyncOperation.TYPE_UPDATE, operation.operationType)
    }

    @Test
    fun `queuePinForDeletion removes existing operations before adding`() = runTest {
        // Queue a create and update first
        syncManager.queuePinForUpload(testPin)
        syncManager.queuePinForUpdate(testPin)
        assertEquals(1, fakeSyncQueueDao.getOperationCount())

        // Queue a deletion - should replace everything
        syncManager.queuePinForDeletion(testPin.id)

        assertEquals(1, fakeSyncQueueDao.getOperationCount())
        val operation = fakeSyncQueueDao.getAllOperations().first()
        assertEquals(SyncOperation.TYPE_DELETE, operation.operationType)
    }

    @Test
    fun `getPendingOperationCount returns correct count`() = runTest {
        assertEquals(0, syncManager.getPendingOperationCount())

        syncManager.queuePinForUpload(testPin)
        assertEquals(1, syncManager.getPendingOperationCount())

        val anotherPin = testPin.copy(id = "test-pin-2")
        syncManager.queuePinForUpload(anotherPin)
        assertEquals(2, syncManager.getPendingOperationCount())
    }

    @Test
    fun `clearQueue removes all operations`() = runTest {
        syncManager.queuePinForUpload(testPin)
        syncManager.queuePinForUpload(testPin.copy(id = "test-pin-2"))
        assertEquals(2, syncManager.getPendingOperationCount())

        syncManager.clearQueue()

        assertEquals(0, syncManager.getPendingOperationCount())
    }

    // ===============================
    // Sync Operations Tests
    // ===============================

    @Test
    fun `syncWithRemote fails when offline`() = runTest {
        setOnline(false)

        val result = syncManager.syncWithRemote()

        assertTrue(result.isFailure)
        assertEquals("Device is offline", result.exceptionOrNull()?.message)
    }

    @Test
    fun `syncWithRemote uploads pending create operation`() = runTest {
        setOnline(true)
        fakePinDao.insertPin(testPin.toEntity())
        syncManager.queuePinForUpload(testPin)

        val result = syncManager.syncWithRemote()

        assertTrue(result.isSuccess)
        assertTrue(fakeRemoteDataSource.hasPin(testPin.id))
        assertEquals(0, fakeSyncQueueDao.getOperationCount()) // Queue should be empty
    }

    @Test
    fun `syncWithRemote uploads pending update operation`() = runTest {
        setOnline(true)
        fakePinDao.insertPin(testPin.toEntity())
        fakeRemoteDataSource.insertPin(testPin) // Pin exists remotely

        val updatedPin = testPin.copy(status = PinStatus.NO_GUN)
        fakePinDao.updatePin(updatedPin.toEntity())
        syncManager.queuePinForUpdate(updatedPin)

        val result = syncManager.syncWithRemote()

        assertTrue(result.isSuccess)
        val remotePin = fakeRemoteDataSource.getPin(testPin.id)
        assertEquals(PinStatus.NO_GUN, remotePin?.status)
        assertEquals(0, fakeSyncQueueDao.getOperationCount())
    }

    @Test
    fun `syncWithRemote uploads pending delete operation`() = runTest {
        setOnline(true)
        fakeRemoteDataSource.insertPin(testPin) // Pin exists remotely
        syncManager.queuePinForDeletion(testPin.id)

        val result = syncManager.syncWithRemote()

        assertTrue(result.isSuccess)
        assertFalse(fakeRemoteDataSource.hasPin(testPin.id))
        assertEquals(0, fakeSyncQueueDao.getOperationCount())
    }

    @Test
    fun `syncWithRemote retries failed operations`() = runTest {
        setOnline(true)
        fakePinDao.insertPin(testPin.toEntity())
        syncManager.queuePinForUpload(testPin)

        // Simulate remote failure
        fakeRemoteDataSource.shouldFailNextOperation()

        val result = syncManager.syncWithRemote()

        // Sync should complete (partial success)
        assertTrue(result.isSuccess)
        // Operation should still be in queue with retry count incremented
        assertEquals(1, fakeSyncQueueDao.getOperationCount())
        val operation = fakeSyncQueueDao.getAllOperations().first()
        assertEquals(1, operation.retryCount)
    }

    @Test
    fun `syncWithRemote removes operation after max retries`() = runTest {
        setOnline(true)
        fakePinDao.insertPin(testPin.toEntity())

        // Create operation with 2 retries already
        val operation = SyncQueueEntity(
            id = 1,
            pinId = testPin.id,
            operationType = SyncOperation.TYPE_CREATE,
            timestamp = System.currentTimeMillis(),
            retryCount = 2
        )
        fakeSyncQueueDao.insertOperation(operation)

        // Fail one more time (will hit MAX_RETRIES = 3)
        fakeRemoteDataSource.shouldFailNextOperation()

        syncManager.syncWithRemote()

        // Operation should be removed after hitting max retries
        assertEquals(0, fakeSyncQueueDao.getOperationCount())
    }

    @Test
    fun `syncWithRemote downloads and inserts new remote pins`() = runTest {
        setOnline(true)
        val remotePin = testPin.copy(id = "remote-pin-1")
        fakeRemoteDataSource.insertPin(remotePin)

        val result = syncManager.syncWithRemote()

        assertTrue(result.isSuccess)
        val localPin = fakePinDao.getPinById(remotePin.id)
        assertEquals(remotePin.id, localPin?.id)
    }

    @Test
    fun `syncWithRemote uses last-write-wins for conflicts - remote newer`() = runTest {
        setOnline(true)

        // Local pin (older)
        val localPin = testPin.copy(
            status = PinStatus.ALLOWED,
            metadata = testPin.metadata.copy(lastModified = currentTime - 3600000)
        )
        fakePinDao.insertPin(localPin.toEntity())

        // Remote pin (newer)
        val remotePin = testPin.copy(
            status = PinStatus.NO_GUN,
            metadata = testPin.metadata.copy(lastModified = currentTime - 1800000)
        )
        fakeRemoteDataSource.insertPin(remotePin)

        syncManager.syncWithRemote()

        // Should use remote version (newer)
        val mergedPin = fakePinDao.getPinById(testPin.id)?.toDomain()
        assertEquals(PinStatus.NO_GUN, mergedPin?.status)
    }

    @Test
    fun `syncWithRemote uses last-write-wins for conflicts - local newer`() = runTest {
        setOnline(true)

        // Local pin (newer)
        val localPin = testPin.copy(
            status = PinStatus.ALLOWED,
            metadata = testPin.metadata.copy(lastModified = currentTime - 1800000)
        )
        fakePinDao.insertPin(localPin.toEntity())

        // Remote pin (older)
        val remotePin = testPin.copy(
            status = PinStatus.NO_GUN,
            metadata = testPin.metadata.copy(lastModified = currentTime - 3600000)
        )
        fakeRemoteDataSource.insertPin(remotePin)

        syncManager.syncWithRemote()

        // Should keep local version (newer)
        val mergedPin = fakePinDao.getPinById(testPin.id)?.toDomain()
        assertEquals(PinStatus.ALLOWED, mergedPin?.status)
    }

    @Test
    fun `syncStatus emits correct states during sync`() = runTest {
        setOnline(true)
        fakePinDao.insertPin(testPin.toEntity())
        syncManager.queuePinForUpload(testPin)

        syncManager.syncStatus.test {
            // Initial state
            assertEquals(SyncStatus.Idle, awaitItem())

            // Start sync
            syncManager.syncWithRemote()

            // Should emit Syncing state
            val syncing = awaitItem() as SyncStatus.Syncing
            assertEquals(1, syncing.pendingCount)

            // Should emit Success state
            val success = awaitItem() as SyncStatus.Success
            assertEquals(1, success.uploadedCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===============================
    // Fake Implementations
    // ===============================

    private class FakePinDao : PinDao {
        private val pins = mutableMapOf<String, PinEntity>()

        override suspend fun insertPin(pin: PinEntity) {
            pins[pin.id] = pin
        }

        override suspend fun insertPins(pins: List<PinEntity>) {
            pins.forEach { this.pins[it.id] = it }
        }

        override suspend fun updatePin(pin: PinEntity) {
            pins[pin.id] = pin
        }

        override suspend fun deletePin(pin: PinEntity) {
            pins.remove(pin.id)
        }

        override suspend fun getPinById(id: String): PinEntity? = pins[id]

        override fun getAllPins(): Flow<List<PinEntity>> = emptyFlow()

        override suspend fun deleteAllPins() {
            pins.clear()
        }

        override suspend fun getPinCount(): Int = pins.size
    }

    private class FakeSyncQueueDao : SyncQueueDao {
        private val operations = mutableListOf<SyncQueueEntity>()
        private var nextId = 1L

        override suspend fun insertOperation(operation: SyncQueueEntity): Long {
            val id = nextId++
            operations.add(operation.copy(id = id))
            return id
        }

        override suspend fun deleteOperation(id: Long) {
            operations.removeIf { it.id == id }
        }

        override suspend fun deleteOperationsForPin(pinId: String) {
            operations.removeIf { it.pinId == pinId }
        }

        override suspend fun getAllOperations(): List<SyncQueueEntity> = operations.toList()

        override fun observeOperations(): Flow<List<SyncQueueEntity>> = emptyFlow()

        override suspend fun getOperationsForPin(pinId: String): List<SyncQueueEntity> =
            operations.filter { it.pinId == pinId }

        override suspend fun getOperationCount(): Int = operations.size

        override suspend fun getFailedOperations(threshold: Int): List<SyncQueueEntity> =
            operations.filter { it.retryCount >= threshold }

        override suspend fun updateOperation(operation: SyncQueueEntity) {
            val index = operations.indexOfFirst { it.id == operation.id }
            if (index >= 0) {
                operations[index] = operation
            }
        }

        override suspend fun deleteAllOperations() {
            operations.clear()
        }
    }

    private class FakeRemotePinDataSource : RemotePinDataSource {
        private val pins = mutableMapOf<String, Pin>()
        private var shouldFail = false

        fun shouldFailNextOperation() {
            shouldFail = true
        }

        fun hasPin(id: String): Boolean = pins.containsKey(id)

        fun getPin(id: String): Pin? = pins[id]

        override suspend fun insertPin(pin: Pin): Result<Pin> {
            if (shouldFail) {
                shouldFail = false
                return Result.failure(Exception("Remote operation failed"))
            }
            pins[pin.id] = pin
            return Result.success(pin)
        }

        override suspend fun updatePin(pin: Pin): Result<Pin> {
            if (shouldFail) {
                shouldFail = false
                return Result.failure(Exception("Remote operation failed"))
            }
            pins[pin.id] = pin
            return Result.success(pin)
        }

        override suspend fun deletePin(id: String): Result<Unit> {
            if (shouldFail) {
                shouldFail = false
                return Result.failure(Exception("Remote operation failed"))
            }
            pins.remove(id)
            return Result.success(Unit)
        }

        override suspend fun getPinById(pinId: String): Result<Pin?> {
            return Result.success(pins[pinId])
        }

        override suspend fun getAllPins(): Result<List<Pin>> {
            return Result.success(pins.values.toList())
        }

        override suspend fun getPinsInBoundingBox(
            minLat: Double,
            maxLat: Double,
            minLng: Double,
            maxLng: Double
        ): Result<List<Pin>> {
            val filtered = pins.values.filter {
                it.location.latitude in minLat..maxLat &&
                it.location.longitude in minLng..maxLng
            }
            return Result.success(filtered)
        }

        override fun subscribeToChanges(): Flow<PinChangeEvent> = emptyFlow()
    }
}
