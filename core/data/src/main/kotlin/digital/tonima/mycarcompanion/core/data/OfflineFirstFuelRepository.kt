package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.database.FuelDao
import digital.tonima.mycarcompanion.core.database.asEntity
import digital.tonima.mycarcompanion.core.database.asExternalModel
import digital.tonima.mycarcompanion.core.model.FuelRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineFirstFuelRepository @Inject constructor(
    private val fuelDao: FuelDao
) : FuelRepository {
    override fun getFuelRecordsForVehicle(vehicleId: Long): Flow<List<FuelRecord>> =
        fuelDao.getFuelRecordsForVehicle(vehicleId).map { entities ->
            entities.map { it.asExternalModel() }
        }

    override fun getLastFuelRecordForVehicle(vehicleId: Long): Flow<FuelRecord?> =
        fuelDao.getLastFuelRecordForVehicle(vehicleId).map { it?.asExternalModel() }

    override fun getPreviousFuelRecord(vehicleId: Long, currentDate: Long): Flow<FuelRecord?> =
        fuelDao.getPreviousFuelRecord(vehicleId, currentDate).map { it?.asExternalModel() }

    override fun getTotalFuelCostForVehicle(vehicleId: Long): Flow<Double?> =
        fuelDao.getTotalFuelCostForVehicle(vehicleId)

    override suspend fun insertFuelRecord(record: FuelRecord): Long =
        fuelDao.insertFuelRecord(record.asEntity())

    override suspend fun updateFuelRecord(record: FuelRecord) =
        fuelDao.updateFuelRecord(record.asEntity())

    override suspend fun deleteFuelRecord(record: FuelRecord) =
        fuelDao.deleteFuelRecord(record.asEntity())
        
    override suspend fun calculateFuelConsumption(currentRecord: FuelRecord): Double? {
        // Busca o abastecimento imediatamente anterior a este
        val previousRecord = fuelDao.getPreviousFuelRecord(
            vehicleId = currentRecord.vehicleId,
            currentDate = currentRecord.date.toEpochMilliseconds()
        ).firstOrNull()?.asExternalModel()
        
        if (previousRecord == null || currentRecord.liters <= 0) {
            return null
        }
        
        val distanceTraveled = currentRecord.mileage - previousRecord.mileage
        
        // Se a distância for negativa ou zero, os dados estão inconsistentes
        if (distanceTraveled <= 0) return null
        
        return distanceTraveled / currentRecord.liters
    }
}