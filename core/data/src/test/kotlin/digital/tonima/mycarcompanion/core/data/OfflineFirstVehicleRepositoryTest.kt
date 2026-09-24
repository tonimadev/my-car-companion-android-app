package digital.tonima.mycarcompanion.core.data

import android.content.Context
import digital.tonima.mycarcompanion.core.database.PartDao
import digital.tonima.mycarcompanion.core.database.PartEntity
import digital.tonima.mycarcompanion.core.database.VehicleDao
import digital.tonima.mycarcompanion.core.database.VehicleEntity
import digital.tonima.mycarcompanion.core.model.Vehicle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineFirstVehicleRepositoryTest {

    private val vehicleDao = mockk<VehicleDao>(relaxUnitFun = true)
    private val partDao = mockk<PartDao>()
    private val context = mockk<Context>()
    private val repository = OfflineFirstVehicleRepository(vehicleDao, partDao, context)

    private val entity = VehicleEntity(id = 1, name = "Civic", currentOdometer = 1000.0, tankCapacity = 50.0, isCurrent = true)
    private val vehicle = Vehicle(id = 1, name = "Civic", currentOdometer = 1000.0, tankCapacity = 50.0, isCurrent = true)

    @Test
    fun `getVehicles maps entities to models`() = runTest {
        every { vehicleDao.getVehicles() } returns flowOf(listOf(entity))

        assertEquals(listOf(vehicle), repository.getVehicles().first())
    }

    @Test
    fun `getVehicle returns mapped model or null`() = runTest {
        coEvery { vehicleDao.getVehicle(1) } returns entity
        coEvery { vehicleDao.getVehicle(2) } returns null

        assertEquals(vehicle, repository.getVehicle(1))
        assertNull(repository.getVehicle(2))
    }

    @Test
    fun `getCurrentVehicle maps entity to model`() = runTest {
        every { vehicleDao.getCurrentVehicle() } returns flowOf(entity)

        assertEquals(vehicle, repository.getCurrentVehicle().first())
    }

    @Test
    fun `insertVehicle inserts vehicle and one default part per template`() = runTest {
        coEvery { vehicleDao.insertVehicle(any()) } returns 7L
        coEvery { partDao.insertPart(any()) } returns 1L
        every { context.getString(any()) } answers { "part-${firstArg<Int>()}" }

        val id = repository.insertVehicle(vehicle.copy(id = 0, currentOdometer = 5000.0))

        assertEquals(7L, id)
        DEFAULT_PARTS.forEach { defaultPart ->
            coVerify(exactly = 1) {
                partDao.insertPart(
                    PartEntity(
                        vehicleId = 7L,
                        name = "part-${defaultPart.nameResId}",
                        lifeSpanMileage = defaultPart.lifeSpanKm,
                        lastMaintenanceOdometer = 5000.0
                    )
                )
            }
        }
        coVerify(exactly = DEFAULT_PARTS.size) { partDao.insertPart(any()) }
    }

    @Test
    fun `update and delete delegate mapped entity to dao`() = runTest {
        repository.updateVehicle(vehicle)
        repository.deleteVehicle(vehicle)

        coVerify { vehicleDao.updateVehicle(entity) }
        coVerify { vehicleDao.deleteVehicle(entity) }
    }

    @Test
    fun `setCurrentVehicle clears previous selection before setting new one`() = runTest {
        repository.setCurrentVehicle(3)

        coVerifyOrder {
            vehicleDao.clearCurrentVehicle()
            vehicleDao.setCurrentVehicle(3)
        }
    }

    @Test
    fun `updateActiveVehicleOdometer adds increment to current vehicle`() = runTest {
        every { vehicleDao.getCurrentVehicle() } returns flowOf(entity)

        repository.updateActiveVehicleOdometer(12.5)

        coVerify { vehicleDao.updateVehicle(entity.copy(currentOdometer = 1012.5)) }
    }

    @Test
    fun `updateActiveVehicleOdometer does nothing without current vehicle`() = runTest {
        every { vehicleDao.getCurrentVehicle() } returns flowOf(null)

        repository.updateActiveVehicleOdometer(12.5)

        coVerify(exactly = 0) { vehicleDao.updateVehicle(any()) }
    }
}
