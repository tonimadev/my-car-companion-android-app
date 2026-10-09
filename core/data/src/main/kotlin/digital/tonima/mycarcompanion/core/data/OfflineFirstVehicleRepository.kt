package digital.tonima.mycarcompanion.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mycarcompanion.core.database.OdometerDao
import digital.tonima.mycarcompanion.core.database.PartDao
import digital.tonima.mycarcompanion.core.database.PartEntity
import digital.tonima.mycarcompanion.core.database.VehicleDao
import digital.tonima.mycarcompanion.core.database.asEntity
import digital.tonima.mycarcompanion.core.database.asExternalModel
import digital.tonima.mycarcompanion.core.model.OdometerRecord
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Clock

class OfflineFirstVehicleRepository @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val partDao: PartDao,
    private val odometerDao: OdometerDao,
    @ApplicationContext private val context: Context
) : VehicleRepository {
    override fun getVehicles(): Flow<List<Vehicle>> = 
        vehicleDao.getVehicles().map { it.map { entity -> entity.asExternalModel() } }

    override suspend fun getVehicle(id: Long): Vehicle? = 
        vehicleDao.getVehicle(id)?.asExternalModel()

    override fun getCurrentVehicle(): Flow<Vehicle?> = 
        vehicleDao.getCurrentVehicle().map { it?.asExternalModel() }

    override suspend fun insertVehicle(vehicle: Vehicle): Long = insertVehicle(vehicle, emptyList())

    override suspend fun insertVehicle(vehicle: Vehicle, serviceIntervals: List<ServiceInterval>): Long {
        val vehicleId = vehicleDao.insertVehicle(vehicle.asEntity())
        val now = Clock.System.now()

        DEFAULT_PARTS.forEach { defaultPart ->
            val interval = serviceIntervals.firstOrNull { it.part.nameResId == defaultPart.nameResId }
            partDao.insertPart(
                PartEntity(
                    vehicleId = vehicleId,
                    name = context.getString(defaultPart.nameResId),
                    lifeSpanMileage = interval?.km ?: defaultPart.lifeSpanKm,
                    lastMaintenanceOdometer = vehicle.currentOdometer,
                    lifeSpanMonths = interval?.months,
                    // A new vehicle has no history, so the time interval starts counting at registration.
                    lastMaintenanceDate = if (interval?.months != null) now else null
                )
            )
        }
        recordOdometer(vehicleId, vehicle.currentOdometer)

        return vehicleId
    }

    override suspend fun updateVehicle(vehicle: Vehicle) {
        val previous = vehicleDao.getVehicle(vehicle.id)
        val previousOdometer = previous?.currentOdometer
        // Screens that edit a vehicle do not know the estimated consumption, so keep the stored one.
        val estimatedConsumption = vehicle.estimatedConsumption ?: previous?.estimatedConsumption
        vehicleDao.updateVehicle(vehicle.copy(estimatedConsumption = estimatedConsumption).asEntity())
        if (previousOdometer != vehicle.currentOdometer) recordOdometer(vehicle.id, vehicle.currentOdometer)
    }

    override suspend fun deleteVehicle(vehicle: Vehicle) = 
        vehicleDao.deleteVehicle(vehicle.asEntity())

    override suspend fun setCurrentVehicle(id: Long) {
        vehicleDao.clearCurrentVehicle()
        vehicleDao.setCurrentVehicle(id)
    }

    override suspend fun updateActiveVehicleOdometer(incrementInKm: Double) {
        val currentVehicle = vehicleDao.getCurrentVehicle().first()
        currentVehicle?.let { vehicle ->
            val newOdometer = vehicle.currentOdometer + incrementInKm
            vehicleDao.updateVehicle(vehicle.copy(currentOdometer = newOdometer))
            if (incrementInKm != 0.0) recordOdometer(vehicle.id, newOdometer)
        }
    }

    /** Keeps the odometer history that [PredictionEngine] uses to project the next maintenance by distance. */
    private suspend fun recordOdometer(vehicleId: Long, odometer: Double) {
        odometerDao.insertOdometerRecord(
            OdometerRecord(vehicleId = vehicleId, date = Clock.System.now(), odometerValue = odometer).asEntity()
        )
    }
}
