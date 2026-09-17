package digital.tonima.mycarcompanion.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class MaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val vehicleRepository: VehicleRepository,
    private val partRepository: PartRepository,
    private val notificationHelper: MaintenanceNotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val vehicles = vehicleRepository.getVehicles().first()

        vehicles.forEach { vehicle ->
            val parts = partRepository.getPartsForVehicle(vehicle.id).first()
            parts.forEach { part ->
                if (MaintenanceAlertPolicy.isMaintenanceDue(part, vehicle)) {
                    // Use a unique ID for each vehicle/part combination
                    val notificationId = (vehicle.id.toString() + part.id.toString()).hashCode()
                    notificationHelper.showMaintenanceAlert(vehicle.name, part.name, notificationId)
                }
            }
        }

        return Result.success()
    }
}
