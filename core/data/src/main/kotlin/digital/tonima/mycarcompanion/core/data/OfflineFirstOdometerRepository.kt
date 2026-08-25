package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.database.OdometerDao
import digital.tonima.mycarcompanion.core.database.asEntity
import digital.tonima.mycarcompanion.core.database.asExternalModel
import digital.tonima.mycarcompanion.core.model.OdometerRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineFirstOdometerRepository @Inject constructor(
    private val odometerDao: OdometerDao
) : OdometerRepository {
    override fun getOdometerRecordsForVehicle(vehicleId: Long): Flow<List<OdometerRecord>> =
        odometerDao.getOdometerRecordsForVehicle(vehicleId).map { entities ->
            entities.map { it.asExternalModel() }
        }

    override suspend fun insertOdometerRecord(record: OdometerRecord): Long =
        odometerDao.insertOdometerRecord(record.asEntity())

    override suspend fun updateOdometerRecord(record: OdometerRecord) =
        odometerDao.updateOdometerRecord(record.asEntity())

    override suspend fun deleteOdometerRecord(record: OdometerRecord) =
        odometerDao.deleteOdometerRecord(record.asEntity())
}
