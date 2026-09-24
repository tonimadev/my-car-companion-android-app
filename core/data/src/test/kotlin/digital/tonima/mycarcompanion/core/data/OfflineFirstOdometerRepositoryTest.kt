package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.database.OdometerDao
import digital.tonima.mycarcompanion.core.database.OdometerEntity
import digital.tonima.mycarcompanion.core.model.OdometerRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class OfflineFirstOdometerRepositoryTest {

    private val odometerDao = mockk<OdometerDao>(relaxUnitFun = true)
    private val repository = OfflineFirstOdometerRepository(odometerDao)

    private val date = Instant.fromEpochMilliseconds(1_000L)
    private val entity = OdometerEntity(id = 4, vehicleId = 1, date = date, odometerValue = 1200.0)
    private val record = OdometerRecord(id = 4, vehicleId = 1, date = date, odometerValue = 1200.0)

    @Test
    fun `getOdometerRecordsForVehicle maps entities to models`() = runTest {
        every { odometerDao.getOdometerRecordsForVehicle(1) } returns flowOf(listOf(entity))

        assertEquals(listOf(record), repository.getOdometerRecordsForVehicle(1).first())
    }

    @Test
    fun `write operations delegate mapped entity to dao`() = runTest {
        coEvery { odometerDao.insertOdometerRecord(entity) } returns 4L

        assertEquals(4L, repository.insertOdometerRecord(record))
        repository.updateOdometerRecord(record)
        repository.deleteOdometerRecord(record)

        coVerify { odometerDao.updateOdometerRecord(entity) }
        coVerify { odometerDao.deleteOdometerRecord(entity) }
    }
}
