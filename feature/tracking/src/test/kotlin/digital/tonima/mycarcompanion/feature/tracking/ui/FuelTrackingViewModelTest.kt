package digital.tonima.mycarcompanion.feature.tracking.ui

import digital.tonima.mycarcompanion.core.data.FuelRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.model.FuelRecord
import digital.tonima.mycarcompanion.core.model.Vehicle
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FuelTrackingViewModelTest {

    private val fuelRepository = mockk<FuelRepository>(relaxed = true)
    private val vehicleRepository = mockk<VehicleRepository>(relaxed = true)
    private val currentVehicleFlow = MutableStateFlow<Vehicle?>(null)

    private val vehicle = Vehicle(id = 1, name = "Civic", currentOdometer = 1000.0, isCurrent = true)
    private val date = Instant.fromEpochMilliseconds(5_000L)

    private lateinit var viewModel: FuelTrackingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { vehicleRepository.getCurrentVehicle() } returns currentVehicleFlow
        viewModel = FuelTrackingViewModel(fuelRepository, vehicleRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.observeVehicle(value: Vehicle?) {
        currentVehicleFlow.value = value
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.currentVehicle.collect {} }
    }

    @Test
    fun `save is ignored without current vehicle`() = runTest {
        observeVehicle(null)

        viewModel.saveFuelRecord(liters = 40.0, totalCost = 200.0, fuelType = "Gas")

        coVerify(exactly = 0) { fuelRepository.insertFuelRecord(any()) }
    }

    @Test
    fun `new record with higher mileage updates vehicle odometer`() = runTest {
        observeVehicle(vehicle)

        viewModel.saveFuelRecord(liters = 40.0, totalCost = 200.0, fuelType = "Gas", mileage = 1500.0, date = date)

        coVerify {
            fuelRepository.insertFuelRecord(FuelRecord(vehicleId = 1, date = date, mileage = 1500.0, liters = 40.0, totalCost = 200.0, fuelType = "Gas"))
        }
        coVerify { vehicleRepository.updateVehicle(vehicle.copy(currentOdometer = 1500.0)) }
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun `new record without mileage uses vehicle odometer and keeps vehicle unchanged`() = runTest {
        observeVehicle(vehicle)

        viewModel.saveFuelRecord(liters = 40.0, totalCost = 200.0, fuelType = "Gas", date = date)

        coVerify { fuelRepository.insertFuelRecord(match { it.mileage == 1000.0 }) }
        coVerify(exactly = 0) { vehicleRepository.updateVehicle(any()) }
    }

    @Test
    fun `new record with lower mileage keeps vehicle unchanged`() = runTest {
        observeVehicle(vehicle)

        viewModel.saveFuelRecord(liters = 40.0, totalCost = 200.0, fuelType = "Gas", mileage = 900.0, date = date)

        coVerify(exactly = 0) { vehicleRepository.updateVehicle(any()) }
    }

    @Test
    fun `existing record is updated instead of inserted`() = runTest {
        observeVehicle(vehicle)

        viewModel.saveFuelRecord(liters = 40.0, totalCost = 200.0, fuelType = "Gas", mileage = 1500.0, date = date, id = 9)

        coVerify { fuelRepository.updateFuelRecord(match { it.id == 9L && it.mileage == 1500.0 }) }
        coVerify(exactly = 0) { fuelRepository.insertFuelRecord(any()) }
        coVerify(exactly = 0) { vehicleRepository.updateVehicle(any()) }
    }

    @Test
    fun `loadRecord exposes existing record`() = runTest {
        val record = FuelRecord(id = 9, vehicleId = 1, date = date, mileage = 1500.0, liters = 40.0, totalCost = 200.0, fuelType = "Gas")
        every { fuelRepository.getFuelRecord(9) } returns flowOf(record)

        viewModel.loadRecord(9)

        assertEquals(record, viewModel.existingRecord.value)
    }
}
