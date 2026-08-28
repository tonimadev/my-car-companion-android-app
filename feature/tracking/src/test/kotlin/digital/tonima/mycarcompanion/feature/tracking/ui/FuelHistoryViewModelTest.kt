package digital.tonima.mycarcompanion.feature.tracking.ui

import digital.tonima.mycarcompanion.core.data.FuelRepository
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.model.FuelRecord
import digital.tonima.mycarcompanion.core.model.Vehicle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FuelHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fuelRepository = mockk<FuelRepository>(relaxed = true)
    private val vehicleRepository = mockk<VehicleRepository>(relaxed = true)
    private val proUserProvider = mockk<ProUserProvider>(relaxed = true)

    private lateinit var viewModel: FuelHistoryViewModel

    private val currentVehicleFlow = MutableStateFlow<Vehicle?>(null)
    private val fuelRecordsFlow = MutableStateFlow<List<FuelRecord>>(emptyList())
    private val isProUserFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { vehicleRepository.getCurrentVehicle() } returns currentVehicleFlow
        every { fuelRepository.getFuelRecordsForVehicle(any()) } returns fuelRecordsFlow
        every { proUserProvider.isProUser } returns isProUserFlow
        
        viewModel = FuelHistoryViewModel(fuelRepository, vehicleRepository, proUserProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        val job = viewModel.uiState.onEach { }.launchIn(this)
        org.junit.Assert.assertTrue(viewModel.uiState.value.isLoading)
        job.cancel()
    }

    @Test
    fun `uiState updates when fuel records are loaded`() = runTest {
        val vehicle = Vehicle(id = 1, name = "Test Car", currentOdometer = 100.0, isCurrent = true)
        val record = FuelRecord(id = 1, vehicleId = 1, fuelType = "Gas", liters = 10.0, mileage = 100.0, totalCost = 50.0, date = Instant.fromEpochMilliseconds(0))
        
        coEvery { fuelRepository.calculateFuelConsumption(record) } returns 10.0
        
        val job = viewModel.uiState.onEach { }.launchIn(this)
        
        currentVehicleFlow.value = vehicle
        fuelRecordsFlow.value = listOf(record)
        
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.items.size)
        assertEquals(10.0, state.averageConsumption)
        assertEquals(50.0, state.totalSpent, 0.1)
        
        job.cancel()
    }
}
