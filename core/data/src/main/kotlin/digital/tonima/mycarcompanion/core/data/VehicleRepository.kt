package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun getVehicles(): Flow<List<Vehicle>>
    suspend fun getVehicle(id: Long): Vehicle?
    fun getCurrentVehicle(): Flow<Vehicle?>
    suspend fun insertVehicle(vehicle: Vehicle): Long
    suspend fun updateVehicle(vehicle: Vehicle)
    suspend fun deleteVehicle(vehicle: Vehicle)
    suspend fun setCurrentVehicle(id: Long)
    suspend fun updateActiveVehicleOdometer(incrementInKm: Double)
}
