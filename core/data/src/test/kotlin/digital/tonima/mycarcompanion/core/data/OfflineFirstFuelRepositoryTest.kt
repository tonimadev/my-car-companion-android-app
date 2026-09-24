package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.database.FuelDao
import digital.tonima.mycarcompanion.core.database.FuelEntity
import digital.tonima.mycarcompanion.core.model.FuelRecord
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
import kotlin.time.Instant

class OfflineFirstFuelRepositoryTest {

    private val fuelDao = mockk<FuelDao>(relaxUnitFun = true)
    private val repository = OfflineFirstFuelRepository(fuelDao)

    private fun record(mileage: Double, liters: Double, epochMillis: Long = 0L) = FuelRecord(
        vehicleId = 1,
        date = Instant.fromEpochMilliseconds(epochMillis),
        mileage = mileage,
        liters = liters,
        totalCost = 100.0,
        fuelType = "Gasolina"
    )

    private fun stubPreviousRecord(currentDate: Long, previous: FuelRecord?) {
        every {
            fuelDao.getPreviousFuelRecord(vehicleId = 1, currentDate = currentDate)
        } returns flowOf(previous?.let {
            FuelEntity(
                vehicleId = it.vehicleId,
                date = it.date,
                mileage = it.mileage,
                liters = it.liters,
                totalCost = it.totalCost,
                fuelType = it.fuelType
            )
        })
    }

    @Test
    fun `calculateFuelConsumption returns km per liter based on distance since previous fill-up`() = runTest {
        val previous = record(mileage = 1000.0, liters = 30.0, epochMillis = 1_000L)
        val current = record(mileage = 1500.0, liters = 50.0, epochMillis = 2_000L)
        stubPreviousRecord(current.date.toEpochMilliseconds(), previous)

        val result = repository.calculateFuelConsumption(current)

        // 500 km traveled / 50 liters = 10 km/L
        assertEquals(10.0, result)
    }

    @Test
    fun `calculateFuelConsumption returns null when there is no previous record`() = runTest {
        val current = record(mileage = 1500.0, liters = 50.0, epochMillis = 2_000L)
        stubPreviousRecord(current.date.toEpochMilliseconds(), null)

        val result = repository.calculateFuelConsumption(current)

        assertNull(result)
    }

    @Test
    fun `calculateFuelConsumption returns null when current liters is zero`() = runTest {
        val previous = record(mileage = 1000.0, liters = 30.0, epochMillis = 1_000L)
        val current = record(mileage = 1500.0, liters = 0.0, epochMillis = 2_000L)
        stubPreviousRecord(current.date.toEpochMilliseconds(), previous)

        val result = repository.calculateFuelConsumption(current)

        assertNull(result)
    }

    @Test
    fun `calculateFuelConsumption returns null when distance traveled is zero`() = runTest {
        val previous = record(mileage = 1500.0, liters = 30.0, epochMillis = 1_000L)
        val current = record(mileage = 1500.0, liters = 50.0, epochMillis = 2_000L)
        stubPreviousRecord(current.date.toEpochMilliseconds(), previous)

        val result = repository.calculateFuelConsumption(current)

        assertNull(result)
    }

    @Test
    fun `calculateFuelConsumption returns null when odometer data is inconsistent (negative distance)`() = runTest {
        val previous = record(mileage = 2000.0, liters = 30.0, epochMillis = 1_000L)
        val current = record(mileage = 1500.0, liters = 50.0, epochMillis = 2_000L)
        stubPreviousRecord(current.date.toEpochMilliseconds(), previous)

        val result = repository.calculateFuelConsumption(current)

        assertNull(result)
    }

    @Test
    fun `read operations map entities to models`() = runTest {
        val current = record(mileage = 1500.0, liters = 50.0, epochMillis = 2_000L)
        val entity = FuelEntity(
            vehicleId = 1,
            date = current.date,
            mileage = 1500.0,
            liters = 50.0,
            totalCost = 100.0,
            fuelType = "Gasolina"
        )
        every { fuelDao.getFuelRecord(0) } returns flowOf(entity)
        every { fuelDao.getFuelRecordsForVehicle(1) } returns flowOf(listOf(entity))
        every { fuelDao.getLastFuelRecordForVehicle(1) } returns flowOf(entity)
        every { fuelDao.getTotalFuelCostForVehicle(1) } returns flowOf(100.0)
        stubPreviousRecord(3_000L, current)

        assertEquals(current, repository.getFuelRecord(0).first())
        assertEquals(listOf(current), repository.getFuelRecordsForVehicle(1).first())
        assertEquals(current, repository.getLastFuelRecordForVehicle(1).first())
        assertEquals(current, repository.getPreviousFuelRecord(1, 3_000L).first())
        assertEquals(100.0, repository.getTotalFuelCostForVehicle(1).first())
    }

    @Test
    fun `write operations delegate mapped entity to dao`() = runTest {
        val current = record(mileage = 1500.0, liters = 50.0, epochMillis = 2_000L)
        coEvery { fuelDao.insertFuelRecord(any()) } returns 9L

        assertEquals(9L, repository.insertFuelRecord(current))
        repository.updateFuelRecord(current)
        repository.deleteFuelRecord(current)

        coVerify { fuelDao.updateFuelRecord(match { it.mileage == 1500.0 && it.liters == 50.0 }) }
        coVerify { fuelDao.deleteFuelRecord(match { it.mileage == 1500.0 && it.liters == 50.0 }) }
    }
}
