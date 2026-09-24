package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.database.MaintenanceDao
import digital.tonima.mycarcompanion.core.database.MaintenanceEntity
import digital.tonima.mycarcompanion.core.database.MaintenanceRecordWithPart
import digital.tonima.mycarcompanion.core.database.PartEntity
import digital.tonima.mycarcompanion.core.model.MaintenanceRecord
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

class OfflineFirstMaintenanceRepositoryTest {

    private val maintenanceDao = mockk<MaintenanceDao>(relaxUnitFun = true)
    private val repository = OfflineFirstMaintenanceRepository(maintenanceDao)

    private val date = Instant.fromEpochMilliseconds(1_000L)
    private val entity = MaintenanceEntity(id = 3, partId = 2, date = date, odometerAtMaintenance = 900.0, cost = 150.0, notes = "n")
    private val record = MaintenanceRecord(id = 3, partId = 2, date = date, odometerAtMaintenance = 900.0, cost = 150.0, notes = "n")

    @Test
    fun `read operations map entities to models`() = runTest {
        every { maintenanceDao.getMaintenanceRecord(3) } returns flowOf(entity)
        every { maintenanceDao.getMaintenanceRecordsForPart(2) } returns flowOf(listOf(entity))
        every { maintenanceDao.getMaintenanceRecordsForVehicle(1) } returns flowOf(listOf(entity))
        every { maintenanceDao.getTotalMaintenanceCostForVehicle(1) } returns flowOf(150.0)

        assertEquals(record, repository.getMaintenanceRecord(3).first())
        assertEquals(listOf(record), repository.getMaintenanceRecordsForPart(2).first())
        assertEquals(listOf(record), repository.getMaintenanceRecordsForVehicle(1).first())
        assertEquals(150.0, repository.getTotalMaintenanceCostForVehicle(1).first())
    }

    @Test
    fun `getMaintenanceRecordsWithPartForVehicle pairs record with part name`() = runTest {
        val part = PartEntity(id = 2, vehicleId = 1, name = "Oil", lifeSpanMileage = 1.0, lastMaintenanceOdometer = 0.0)
        every { maintenanceDao.getMaintenanceRecordsWithPartForVehicle(1) } returns
            flowOf(listOf(MaintenanceRecordWithPart(entity, part)))

        assertEquals(listOf(record to "Oil"), repository.getMaintenanceRecordsWithPartForVehicle(1).first())
    }

    @Test
    fun `write operations delegate mapped entity to dao`() = runTest {
        coEvery { maintenanceDao.insertMaintenanceRecord(entity) } returns 3L

        assertEquals(3L, repository.insertMaintenanceRecord(record))
        repository.updateMaintenanceRecord(record)
        repository.deleteMaintenanceRecord(record)

        coVerify { maintenanceDao.updateMaintenanceRecord(entity) }
        coVerify { maintenanceDao.deleteMaintenanceRecord(entity) }
    }
}
