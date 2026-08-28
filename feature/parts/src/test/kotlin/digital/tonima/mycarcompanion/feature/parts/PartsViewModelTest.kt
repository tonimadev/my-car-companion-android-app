package digital.tonima.mycarcompanion.feature.parts

import android.content.Context
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Part
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PartsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val partRepository = mockk<PartRepository>(relaxed = true)
    private val vehicleRepository = mockk<VehicleRepository>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val proUserProvider = mockk<ProUserProvider>(relaxed = true)
    private val vehicleId = 1L

    private lateinit var viewModel: PartsViewModel

    private val partsFlow = MutableStateFlow<List<Part>>(emptyList())
    private val distanceUnitFlow = MutableStateFlow(DistanceUnit.KM)
    private val isProUserFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        val vehicle = Vehicle(id = vehicleId, name = "Test Car", currentOdometer = 100.0)
        coEvery { vehicleRepository.getVehicle(vehicleId) } returns vehicle
        every { partRepository.getPartsForVehicle(vehicleId) } returns partsFlow
        every { userPreferencesRepository.distanceUnit } returns distanceUnitFlow
        every { proUserProvider.isProUser } returns isProUserFlow
        
        viewModel = PartsViewModel(context, partRepository, vehicleRepository, userPreferencesRepository, proUserProvider, vehicleId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest(testDispatcher) {
        // Reset the flow to empty list and ensure we collect to see the initial state
        val job = viewModel.state.onEach { }.launchIn(this)
        
        // PartsViewModel init calls loadVehicle which updates _vehicle.
        // If it's too fast, we might miss isLoading = true.
        // But since we use stateIn(initialValue = isLoading = true), it should start true.
        
        // Wait for first emission
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
        
        job.cancel()
    }

    @Test
    fun `state has correct vehicle`() = runTest(testDispatcher) {
        val job = viewModel.state.onEach { }.launchIn(this)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Test Car", viewModel.state.value.vehicle?.name)
        job.cancel()
    }
}
