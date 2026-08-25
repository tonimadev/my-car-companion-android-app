package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.model.OdometerRecord
import kotlinx.coroutines.flow.Flow

interface OdometerRepository {
    fun getOdometerRecordsForVehicle(vehicleId: Long): Flow<List<OdometerRecord>>
    suspend fun insertOdometerRecord(record: OdometerRecord): Long
    suspend fun updateOdometerRecord(record: OdometerRecord)
    suspend fun deleteOdometerRecord(record: OdometerRecord)
}
