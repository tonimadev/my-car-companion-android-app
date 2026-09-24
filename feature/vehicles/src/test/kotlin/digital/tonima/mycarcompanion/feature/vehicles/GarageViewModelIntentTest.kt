package digital.tonima.mycarcompanion.feature.vehicles

import android.content.Context
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.designsystem.model.VehicleUi
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Vehicle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GarageViewModelIntentTest {

    private val context = mockk<Context>()
    private val vehicleRepository = mockk<VehicleRepository>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>()
    private val proUserProvider = mockk<ProUserProvider>()

    private lateinit var viewModel: GarageViewModel

    private val vehicleUi = VehicleUi(id = 1, name = "Civic", currentOdometer = 1000.0, tankCapacity = 50.0, isCurrent = true)
    private val vehicle = Vehicle(id = 1, name = "Civic", currentOdometer = 1000.0, tankCapacity = 50.0, isCurrent = true)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { context.getString(any()) } returns "fallback"
        every { vehicleRepository.getVehicles() } returns flowOf(emptyList())
        every { userPreferencesRepository.distanceUnit } returns flowOf(DistanceUnit.KM)
        every { proUserProvider.isProUser } returns MutableStateFlow(false)
        viewModel = GarageViewModel(context, vehicleRepository, userPreferencesRepository, proUserProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `add vehicle inserts new vehicle`() = runTest {
        viewModel.handleIntent(GarageIntent.AddVehicle("Civic", 1000.0, 50.0))

        coVerify { vehicleRepository.insertVehicle(Vehicle(name = "Civic", currentOdometer = 1000.0, tankCapacity = 50.0)) }
    }

    @Test
    fun `update delete and select delegate to repository`() = runTest {
        viewModel.handleIntent(GarageIntent.UpdateVehicle(vehicleUi))
        viewModel.handleIntent(GarageIntent.DeleteVehicle(vehicleUi))
        viewModel.handleIntent(GarageIntent.SetCurrentVehicle(1))

        coVerify { vehicleRepository.updateVehicle(vehicle) }
        coVerify { vehicleRepository.deleteVehicle(vehicle) }
        coVerify { vehicleRepository.setCurrentVehicle(1) }
    }

    @Test
    fun `repository failure surfaces exception message`() = runTest {
        coEvery { vehicleRepository.insertVehicle(any()) } throws IllegalStateException("boom")

        viewModel.handleIntent(GarageIntent.AddVehicle("Civic", 1000.0, null))

        assertEquals(GarageUiEffect.ShowError("boom"), viewModel.state.value.effect)
    }

    @Test
    fun `repository failure without message falls back to string resource`() = runTest {
        coEvery { vehicleRepository.updateVehicle(any()) } throws IllegalStateException()
        coEvery { vehicleRepository.deleteVehicle(any()) } throws IllegalStateException()
        coEvery { vehicleRepository.setCurrentVehicle(any()) } throws IllegalStateException()

        viewModel.handleIntent(GarageIntent.UpdateVehicle(vehicleUi))
        assertEquals(GarageUiEffect.ShowError("fallback"), viewModel.state.value.effect)
        viewModel.handleIntent(GarageIntent.ConsumeEffect)

        viewModel.handleIntent(GarageIntent.DeleteVehicle(vehicleUi))
        assertEquals(GarageUiEffect.ShowError("fallback"), viewModel.state.value.effect)
        viewModel.handleIntent(GarageIntent.ConsumeEffect)

        viewModel.handleIntent(GarageIntent.SetCurrentVehicle(1))
        assertEquals(GarageUiEffect.ShowError("fallback"), viewModel.state.value.effect)
    }

    @Test
    fun `navigate to parts emits effect that can be consumed`() = runTest {
        viewModel.onNavigateToParts(7)
        assertEquals(GarageUiEffect.NavigateToParts(7), viewModel.state.value.effect)

        viewModel.handleIntent(GarageIntent.ConsumeEffect)
        assertNull(viewModel.state.value.effect)
    }
}
