package digital.tonima.mycarcompanion.core.data

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import digital.tonima.mycarcompanion.core.model.FuelRecord
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import androidx.annotation.RequiresApi
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point for Android App Functions.
 * This class exposes vehicle management capabilities to AI agents and voice assistants.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "MccAppFunctionService",
    appFunctionXmlFileName = "mcc_functions"
)
@AndroidEntryPoint
abstract class BaseMccAppFunctionService : AppFunctionService() {

    @Inject
    lateinit var vehicleRepository: VehicleRepository
    
    @Inject
    lateinit var fuelRepository: FuelRepository

    /**
     * Updates the current vehicle's odometer with a new mileage reading.
     *
     * @param newMileage The updated total distance shown on the vehicle's odometer in kilometers.
     */
    @AppFunction
    suspend fun updateOdometer(newMileage: Double) {
        val currentVehicle = vehicleRepository.getCurrentVehicle().first()
        if (currentVehicle != null) {
            vehicleRepository.updateVehicle(currentVehicle.copy(currentOdometer = newMileage))
        }
    }

    /**
     * Logs a new fuel fill-up event for the active vehicle.
     *
     * @param liters The volume of fuel added during the refueling, measured in liters.
     * @param totalCost The total monetary cost of the fuel purchase.
     * @param mileage The vehicle's odometer reading at the time of refueling in kilometers.
     */
    @AppFunction
    suspend fun addFuelRecord(liters: Double, totalCost: Double, mileage: Double) {
        val currentVehicle = vehicleRepository.getCurrentVehicle().first()
        if (currentVehicle != null) {
            val record = FuelRecord(
                vehicleId = currentVehicle.id,
                date = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                liters = liters,
                totalCost = totalCost,
                mileage = mileage,
                fuelType = "Gasolina"
            )
            fuelRepository.insertFuelRecord(record)
            if (mileage > currentVehicle.currentOdometer) {
                vehicleRepository.updateVehicle(currentVehicle.copy(currentOdometer = mileage))
            }
        }
    }
}