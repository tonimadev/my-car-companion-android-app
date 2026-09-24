package digital.tonima.mycarcompanion.feature.home.assistant

import android.app.Activity
import android.content.Context
import digital.tonima.mycarcompanion.core.data.CarAiRepository
import digital.tonima.mycarcompanion.core.data.ChatRole
import digital.tonima.mycarcompanion.core.data.ChatTurn
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticChatViewModelTest {

    private val context = mockk<Context>()
    private val vehicleRepository = mockk<VehicleRepository>(relaxed = true)
    private val partRepository = mockk<PartRepository>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val proUserProvider = mockk<ProUserProvider>(relaxed = true)
    private val carAiRepository = mockk<CarAiRepository>()

    private val isAiUserFlow = MutableStateFlow(false)
    private val vehicle = Vehicle(id = 1, name = "Civic", currentOdometer = 1000.0, isCurrent = true)
    private val parts = listOf(Part(id = 2, vehicleId = 1, name = "Oil", lifeSpanMileage = 10000.0, lastMaintenanceOdometer = 0.0))

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { context.getString(any()) } returns "error"
        every { proUserProvider.isAiUser } returns isAiUserFlow
        every { userPreferencesRepository.distanceUnit } returns flowOf(DistanceUnit.KM)
        every { vehicleRepository.getCurrentVehicle() } returns flowOf(vehicle)
        every { partRepository.getPartsForVehicle(1) } returns flowOf(parts)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = DiagnosticChatViewModel(
        context,
        vehicleRepository,
        partRepository,
        userPreferencesRepository,
        proUserProvider,
        carAiRepository
    )

    @Test
    fun `reflects ai entitlement`() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.isAiUser)

        isAiUserFlow.value = true

        assertTrue(viewModel.uiState.value.isAiUser)
    }

    @Test
    fun `successful message appends user and model replies and sends vehicle context`() = runTest {
        isAiUserFlow.value = true
        coEvery { carAiRepository.diagnose(any(), any(), any()) } returns Result.success("Check the oil")
        val viewModel = viewModel()

        viewModel.onIntent(DiagnosticChatIntent.SendMessage("Strange noise"))

        val state = viewModel.uiState.value
        assertEquals(listOf(ChatRole.USER, ChatRole.MODEL), state.messages.map { it.role })
        assertEquals(listOf("Strange noise", "Check the oil"), state.messages.map { it.text })
        assertEquals(listOf(0L, 1L), state.messages.map { it.id })
        assertFalse(state.isSending)
        coVerify { carAiRepository.diagnose(match { it.contains("Civic") && it.contains("Oil") }, emptyList(), "Strange noise") }
    }

    @Test
    fun `follow up message carries conversation history`() = runTest {
        coEvery { carAiRepository.diagnose(any(), any(), any()) } returns Result.success("reply")
        val viewModel = viewModel()

        viewModel.onIntent(DiagnosticChatIntent.SendMessage("first"))
        viewModel.onIntent(DiagnosticChatIntent.SendMessage("second"))

        coVerify {
            carAiRepository.diagnose(any(), listOf(ChatTurn(ChatRole.USER, "first"), ChatTurn(ChatRole.MODEL, "reply")), "second")
        }
    }

    @Test
    fun `failure shows error and can be dismissed`() = runTest {
        coEvery { carAiRepository.diagnose(any(), any(), any()) } returns Result.failure(RuntimeException())
        val viewModel = viewModel()

        viewModel.onIntent(DiagnosticChatIntent.SendMessage("hello"))

        assertEquals("error", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSending)
        assertEquals(1, viewModel.uiState.value.messages.size)

        viewModel.onIntent(DiagnosticChatIntent.DismissError)

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `blank messages and messages while sending are ignored`() = runTest {
        val pending = CompletableDeferred<Result<String>>()
        coEvery { carAiRepository.diagnose(any(), any(), any()) } coAnswers { pending.await() }
        val viewModel = viewModel()

        viewModel.onIntent(DiagnosticChatIntent.SendMessage("   "))
        assertTrue(viewModel.uiState.value.messages.isEmpty())

        viewModel.onIntent(DiagnosticChatIntent.SendMessage("first"))
        viewModel.onIntent(DiagnosticChatIntent.SendMessage("second"))
        assertTrue(viewModel.uiState.value.isSending)
        assertEquals(1, viewModel.uiState.value.messages.size)

        pending.complete(Result.success("done"))
        assertEquals(2, viewModel.uiState.value.messages.size)
        coVerify(exactly = 1) { carAiRepository.diagnose(any(), any(), any()) }
    }

    @Test
    fun `subscribeAi delegates to pro user provider`() {
        val activity = mockk<Activity>()

        viewModel().subscribeAi(activity)

        verify { proUserProvider.launchSubscribeAi(activity) }
    }
}
