package digital.tonima.mycarcompanion.feature.home

import android.content.Context
import digital.tonima.mycarcompanion.core.data.FuelRepository
import digital.tonima.mycarcompanion.core.data.MaintenanceRepository
import digital.tonima.mycarcompanion.core.data.OdometerRepository
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Vehicle
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val vehicleRepository = mockk<VehicleRepository>(relaxed = true)
    private val partRepository = mockk<PartRepository>(relaxed = true)
    private val maintenanceRepository = mockk<MaintenanceRepository>(relaxed = true)
    private val odometerRepository = mockk<OdometerRepository>(relaxed = true)
    private val fuelRepository = mockk<FuelRepository>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val proUserProvider = mockk<ProUserProvider>(relaxed = true)

    private lateinit var viewModel: HomeViewModel

    private val vehiclesFlow = MutableStateFlow<List<Vehicle>>(emptyList())
    private val currentVehicleFlow = MutableStateFlow<Vehicle?>(null)
    private val distanceUnitFlow = MutableStateFlow(DistanceUnit.KM)
    private val isProUserFlow = MutableStateFlow(false)
    private val isAiUserFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { vehicleRepository.getVehicles() } returns vehiclesFlow
        every { vehicleRepository.getCurrentVehicle() } returns currentVehicleFlow
        every { userPreferencesRepository.distanceUnit } returns distanceUnitFlow
        every { proUserProvider.isProUser } returns isProUserFlow
        every { proUserProvider.isAiUser } returns isAiUserFlow
        
        viewModel = HomeViewModel(
            context,
            vehicleRepository,
            partRepository,
            maintenanceRepository,
            odometerRepository,
            fuelRepository,
            userPreferencesRepository,
            proUserProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `uiState updates when vehicle is loaded`() = runTest {
        val vehicle = Vehicle(id = 1, name = "Test Car", currentOdometer = 100.0, isCurrent = true)
        
        every { partRepository.getPartsForVehicle(1) } returns flowOf(emptyList())
        every { odometerRepository.getOdometerRecordsForVehicle(1) } returns flowOf(emptyList())
        every { fuelRepository.getFuelRecordsForVehicle(1) } returns flowOf(emptyList())
        every { maintenanceRepository.getTotalMaintenanceCostForVehicle(1) } returns flowOf(0.0)
        every { fuelRepository.getTotalFuelCostForVehicle(1) } returns flowOf(0.0)

        // Start collecting to trigger stateIn
        val job = viewModel.uiState.onEach { }.launchIn(this)

        vehiclesFlow.value = listOf(vehicle)
        currentVehicleFlow.value = vehicle
        
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(vehicle, state.currentVehicle)
        assertEquals(1, state.vehicles.size)
        
        job.cancel()
    }
    
    private fun assertTrue(value: Boolean) {
        org.junit.Assert.assertTrue(value)
    }
}
