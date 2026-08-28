package digital.tonima.mycarcompanion.feature.vehicles

import android.content.Context
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GarageViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val vehicleRepository = mockk<VehicleRepository>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val proUserProvider = mockk<ProUserProvider>(relaxed = true)

    private lateinit var viewModel: GarageViewModel

    private val vehiclesFlow = MutableStateFlow<List<Vehicle>>(emptyList())
    private val distanceUnitFlow = MutableStateFlow(DistanceUnit.KM)
    private val isProUserFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { vehicleRepository.getVehicles() } returns vehiclesFlow
        every { userPreferencesRepository.distanceUnit } returns distanceUnitFlow
        every { proUserProvider.isProUser } returns isProUserFlow
        
        viewModel = GarageViewModel(context, vehicleRepository, userPreferencesRepository, proUserProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest(testDispatcher) {
        val job = viewModel.state.onEach { }.launchIn(this)
        assertTrue(viewModel.state.value.isLoading)
        job.cancel()
    }

    @Test
    fun `state updates when vehicles are loaded`() = runTest(testDispatcher) {
        val vehicle = Vehicle(id = 1, name = "Test Car", currentOdometer = 100.0, isCurrent = true)
        
        val job = viewModel.state.onEach { }.launchIn(this)
        
        vehiclesFlow.value = listOf(vehicle)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(1, state.vehicles.size)
        assertEquals(vehicle, state.vehicles[0])
        
        job.cancel()
    }
}
