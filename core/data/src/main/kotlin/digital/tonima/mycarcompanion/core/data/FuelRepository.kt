package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.model.FuelRecord
import kotlinx.coroutines.flow.Flow

interface FuelRepository {
    fun getFuelRecord(id: Long): Flow<FuelRecord?>
    fun getFuelRecordsForVehicle(vehicleId: Long): Flow<List<FuelRecord>>
    fun getLastFuelRecordForVehicle(vehicleId: Long): Flow<FuelRecord?>
    fun getPreviousFuelRecord(vehicleId: Long, currentDate: Long): Flow<FuelRecord?>
    fun getTotalFuelCostForVehicle(vehicleId: Long): Flow<Double?>
    suspend fun insertFuelRecord(record: FuelRecord): Long
    suspend fun updateFuelRecord(record: FuelRecord)
    suspend fun deleteFuelRecord(record: FuelRecord)
    
    /**
     * Calcula o consumo médio (Km/L) entre o abastecimento atual (passado como parâmetro)
     * e o imediatamente anterior.
     */
    suspend fun calculateFuelConsumption(currentRecord: FuelRecord): Double?
}