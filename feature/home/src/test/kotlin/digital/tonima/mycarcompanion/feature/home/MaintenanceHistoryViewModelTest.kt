package digital.tonima.mycarcompanion.feature.home

import digital.tonima.mycarcompanion.core.data.MaintenanceRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.designsystem.model.MaintenanceRecordUi
import digital.tonima.mycarcompanion.core.model.MaintenanceRecord
import digital.tonima.mycarcompanion.core.model.Vehicle
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceHistoryViewModelTest {

    private val maintenanceRepository = mockk<MaintenanceRepository>(relaxed = true)
    private val vehicleRepository = mockk<VehicleRepository>(relaxed = true)
    private val currentVehicleFlow = MutableStateFlow<Vehicle?>(null)

    private val date = Instant.fromEpochMilliseconds(1_000L)
    private val record = MaintenanceRecord(id = 3, partId = 2, date = date, odometerAtMaintenance = 900.0, cost = 150.0, notes = "n")
    private val recordUi = MaintenanceRecordUi(3, 2, "Oil", date, 900.0, 150.0, "n")

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { vehicleRepository.getCurrentVehicle() } returns currentVehicleFlow
        every { maintenanceRepository.getMaintenanceRecordsWithPartForVehicle(1) } returns flowOf(listOf(record to "Oil"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state is empty and loaded when there is no current vehicle`() = runTest {
        val viewModel = MaintenanceHistoryViewModel(maintenanceRepository, vehicleRepository)

        assertTrue(viewModel.state.value.items.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `state lists maintenance records of current vehicle`() = runTest {
        val viewModel = MaintenanceHistoryViewModel(maintenanceRepository, vehicleRepository)

        currentVehicleFlow.value = Vehicle(id = 1, name = "Civic", currentOdometer = 1000.0, isCurrent = true)

        assertEquals(listOf(recordUi), viewModel.state.value.items)
    }

    @Test
    fun `update and delete convert ui model back to domain record`() = runTest {
        val viewModel = MaintenanceHistoryViewModel(maintenanceRepository, vehicleRepository)

        viewModel.updateRecord(recordUi)
        viewModel.deleteRecord(recordUi)

        coVerify { maintenanceRepository.updateMaintenanceRecord(record) }
        coVerify { maintenanceRepository.deleteMaintenanceRecord(record) }
    }
}
