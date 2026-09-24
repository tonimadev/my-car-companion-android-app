package digital.tonima.mycarcompanion.feature.home

import android.app.Activity
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val proUserProvider = mockk<ProUserProvider>(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { userPreferencesRepository.distanceUnit } returns MutableStateFlow(DistanceUnit.MILES)
        every { proUserProvider.isProUser } returns MutableStateFlow(true)
        every { proUserProvider.isAiUser } returns MutableStateFlow(true)
        viewModel = SettingsViewModel(userPreferencesRepository, proUserProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exposes preferences and entitlements`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.distanceUnit.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.isProUser.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.isAiUser.collect {} }

        assertEquals(DistanceUnit.MILES, viewModel.distanceUnit.value)
        assertTrue(viewModel.isProUser.value)
        assertTrue(viewModel.isAiUser.value)
    }

    @Test
    fun `setDistanceUnit persists preference`() = runTest {
        viewModel.setDistanceUnit(DistanceUnit.KM)

        coVerify { userPreferencesRepository.setDistanceUnit(DistanceUnit.KM) }
    }

    @Test
    fun `purchase actions delegate to pro user provider`() {
        val activity = mockk<Activity>()

        viewModel.refreshPurchases()
        viewModel.purchasePro(activity)
        viewModel.subscribeAi(activity)

        verify { proUserProvider.refresh() }
        verify { proUserProvider.launchPurchasePro(activity) }
        verify { proUserProvider.launchSubscribeAi(activity) }
    }
}
