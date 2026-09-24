package digital.tonima.mycarcompanion.feature.parts

import android.content.Context
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.designsystem.model.MaintenanceStatus
import digital.tonima.mycarcompanion.core.designsystem.model.PartUi
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Part
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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PartsViewModelIntentTest {

    private val context = mockk<Context>()
    private val partRepository = mockk<PartRepository>(relaxed = true)
    private val vehicleRepository = mockk<VehicleRepository>()
    private val userPreferencesRepository = mockk<UserPreferencesRepository>()
    private val proUserProvider = mockk<ProUserProvider>()

    private lateinit var viewModel: PartsViewModel

    private val date = Instant.fromEpochMilliseconds(1_000L)
    private val part = Part(id = 2, vehicleId = 1, name = "Oil", lifeSpanMileage = 10000.0, lastMaintenanceOdometer = 500.0, lifeSpanMonths = 6, lastMaintenanceDate = date)
    private val partUi = PartUi(2, 1, "Oil", 10000.0, 500.0, 6, date, MaintenanceStatus.OK)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { context.getString(any()) } returns "fallback"
        coEvery { vehicleRepository.getVehicle(1) } returns Vehicle(id = 1, name = "Civic", currentOdometer = 1000.0)
        every { partRepository.getPartsForVehicle(1) } returns flowOf(listOf(part))
        every { userPreferencesRepository.distanceUnit } returns flowOf(DistanceUnit.MILES)
        every { proUserProvider.isProUser } returns MutableStateFlow(true)
        viewModel = PartsViewModel(context, partRepository, vehicleRepository, userPreferencesRepository, proUserProvider, vehicleId = 1)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state combines vehicle parts and preferences`() {
        val state = viewModel.state.value

        assertEquals(listOf(partUi), state.parts)
        assertEquals(DistanceUnit.MILES, state.distanceUnit)
        assertEquals(true, state.isProUser)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `add part inserts part for this vehicle`() = runTest {
        viewModel.handleIntent(PartsIntent.AddPart("Oil", 10000.0, 500.0, 6, date))

        coVerify { partRepository.insertPart(part.copy(id = 0)) }
    }

    @Test
    fun `update and delete convert ui model back to domain part`() = runTest {
        viewModel.handleIntent(PartsIntent.UpdatePart(partUi))
        viewModel.handleIntent(PartsIntent.DeletePart(partUi))

        coVerify { partRepository.updatePart(part) }
        coVerify { partRepository.deletePart(part) }
    }

    @Test
    fun `repository failure surfaces exception message`() = runTest {
        coEvery { partRepository.insertPart(any()) } throws IllegalStateException("boom")

        viewModel.handleIntent(PartsIntent.AddPart("Oil", 10000.0, 500.0))

        assertEquals(PartsUiEffect.ShowError("boom"), viewModel.state.value.effect)
    }

    @Test
    fun `repository failure without message falls back to string resource`() = runTest {
        coEvery { partRepository.updatePart(any()) } throws IllegalStateException()
        coEvery { partRepository.deletePart(any()) } throws IllegalStateException()

        viewModel.handleIntent(PartsIntent.UpdatePart(partUi))
        assertEquals(PartsUiEffect.ShowError("fallback"), viewModel.state.value.effect)
        viewModel.handleIntent(PartsIntent.ConsumeEffect)
        assertNull(viewModel.state.value.effect)

        viewModel.handleIntent(PartsIntent.DeletePart(partUi))
        assertEquals(PartsUiEffect.ShowError("fallback"), viewModel.state.value.effect)
    }
}
