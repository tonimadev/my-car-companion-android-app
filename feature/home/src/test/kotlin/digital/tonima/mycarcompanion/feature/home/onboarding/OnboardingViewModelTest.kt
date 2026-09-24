package digital.tonima.mycarcompanion.feature.home.onboarding

import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Vehicle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class OnboardingViewModelTest {

    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val vehicleRepository = mockk<VehicleRepository>(relaxed = true)
    private val distanceUnitFlow = MutableStateFlow(DistanceUnit.KM)

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { userPreferencesRepository.distanceUnit } returns distanceUnitFlow
        viewModel = OnboardingViewModel(userPreferencesRepository, vehicleRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setDistanceUnit persists preference`() = runTest {
        viewModel.setDistanceUnit(DistanceUnit.MILES)

        coVerify { userPreferencesRepository.setDistanceUnit(DistanceUnit.MILES) }
    }

    @Test
    fun `completeOnboarding without vehicle only marks onboarding as completed`() = runTest {
        var finished = false

        viewModel.completeOnboarding(vehicleName = "  ", onFinished = { finished = true })

        assertTrue(finished)
        assertFalse(viewModel.isCompleting.value)
        coVerify { userPreferencesRepository.setOnboardingCompleted(true) }
        coVerify(exactly = 0) { vehicleRepository.insertVehicle(any()) }
    }

    @Test
    fun `completeOnboarding creates trimmed vehicle and selects it`() = runTest {
        coEvery { vehicleRepository.insertVehicle(any()) } returns 10L
        var finished = false

        viewModel.completeOnboarding(vehicleName = " Civic ", initialOdometer = 1500.0, onFinished = { finished = true })

        assertTrue(finished)
        coVerify { vehicleRepository.insertVehicle(Vehicle(name = "Civic", currentOdometer = 1500.0, isCurrent = true)) }
        coVerify { vehicleRepository.setCurrentVehicle(10L) }
        coVerify { userPreferencesRepository.setOnboardingCompleted(true) }
    }

    @Test
    fun `completeOnboarding converts odometer from miles to km`() = runTest {
        distanceUnitFlow.value = DistanceUnit.MILES
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.distanceUnit.collect {} }
        assertEquals(DistanceUnit.MILES, viewModel.distanceUnit.value)

        viewModel.completeOnboarding(vehicleName = "Civic", initialOdometer = 100.0, onFinished = {})

        coVerify {
            vehicleRepository.insertVehicle(match { it.currentOdometer == DistanceUnit.MILES.toKm(100.0) })
        }
    }

    @Test
    fun `completeOnboarding defaults odometer to zero`() = runTest {
        viewModel.completeOnboarding(vehicleName = "Civic", onFinished = {})

        coVerify { vehicleRepository.insertVehicle(match { it.currentOdometer == 0.0 }) }
    }
}
