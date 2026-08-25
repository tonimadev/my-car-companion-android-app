package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.database.VehicleDao
import digital.tonima.mycarcompanion.core.database.asEntity
import digital.tonima.mycarcompanion.core.database.asExternalModel
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineFirstVehicleRepository @Inject constructor(
    private val vehicleDao: VehicleDao
) : VehicleRepository {
    override fun getVehicles(): Flow<List<Vehicle>> = 
        vehicleDao.getVehicles().map { it.map { entity -> entity.asExternalModel() } }

    override suspend fun getVehicle(id: Long): Vehicle? = 
        vehicleDao.getVehicle(id)?.asExternalModel()

    override fun getCurrentVehicle(): Flow<Vehicle?> = 
        vehicleDao.getCurrentVehicle().map { it?.asExternalModel() }

    override suspend fun insertVehicle(vehicle: Vehicle): Long = 
        vehicleDao.insertVehicle(vehicle.asEntity())

    override suspend fun updateVehicle(vehicle: Vehicle) = 
        vehicleDao.updateVehicle(vehicle.asEntity())

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
        }
    }
}
