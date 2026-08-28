package digital.tonima.mycarcompanion.feature.tracking.domain

import digital.tonima.mycarcompanion.core.data.VehicleRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateMileageUseCaseTest {

    private val vehicleRepository = mockk<VehicleRepository>(relaxed = true)
    private val useCase = UpdateMileageUseCase(vehicleRepository)

    @Test
    fun `invoke calls updateActiveVehicleOdometer`() = runTest {
        useCase(10.0)
        coVerify { vehicleRepository.updateActiveVehicleOdometer(10.0) }
    }
}
