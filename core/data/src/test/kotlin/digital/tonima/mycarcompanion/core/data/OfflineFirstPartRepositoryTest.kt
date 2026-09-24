package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.database.PartDao
import digital.tonima.mycarcompanion.core.database.PartEntity
import digital.tonima.mycarcompanion.core.model.Part
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineFirstPartRepositoryTest {

    private val partDao = mockk<PartDao>(relaxUnitFun = true)
    private val repository = OfflineFirstPartRepository(partDao)

    private val entity = PartEntity(id = 2, vehicleId = 1, name = "Oil", lifeSpanMileage = 10000.0, lastMaintenanceOdometer = 500.0)
    private val part = Part(id = 2, vehicleId = 1, name = "Oil", lifeSpanMileage = 10000.0, lastMaintenanceOdometer = 500.0)

    @Test
    fun `getPartsForVehicle maps entities to models`() = runTest {
        every { partDao.getPartsForVehicle(1) } returns flowOf(listOf(entity))

        assertEquals(listOf(part), repository.getPartsForVehicle(1).first())
    }

    @Test
    fun `getPart returns mapped model or null`() = runTest {
        coEvery { partDao.getPart(2) } returns entity
        coEvery { partDao.getPart(3) } returns null

        assertEquals(part, repository.getPart(2))
        assertNull(repository.getPart(3))
    }

    @Test
    fun `write operations delegate mapped entity to dao`() = runTest {
        coEvery { partDao.insertPart(entity) } returns 2L

        assertEquals(2L, repository.insertPart(part))
        repository.updatePart(part)
        repository.deletePart(part)

        coVerify { partDao.updatePart(entity) }
        coVerify { partDao.deletePart(entity) }
    }
}
