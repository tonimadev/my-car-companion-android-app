package digital.tonima.mycarcompanion.feature.tracking.domain

import digital.tonima.mycarcompanion.core.data.VehicleRepository
import javax.inject.Inject

class UpdateMileageUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke(increment: Double) {
        vehicleRepository.updateActiveVehicleOdometer(increment)
    }
}
