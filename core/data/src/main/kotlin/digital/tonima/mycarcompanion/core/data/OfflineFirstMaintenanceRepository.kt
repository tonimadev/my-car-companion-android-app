package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.database.MaintenanceDao
import digital.tonima.mycarcompanion.core.database.asEntity
import digital.tonima.mycarcompanion.core.database.asExternalModel
import digital.tonima.mycarcompanion.core.model.MaintenanceRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineFirstMaintenanceRepository @Inject constructor(
    private val maintenanceDao: MaintenanceDao
) : MaintenanceRepository {
    override fun getMaintenanceRecord(id: Long): Flow<MaintenanceRecord?> =
        maintenanceDao.getMaintenanceRecord(id).map { it?.asExternalModel() }

    override fun getMaintenanceRecordsForPart(partId: Long): Flow<List<MaintenanceRecord>> =
        maintenanceDao.getMaintenanceRecordsForPart(partId).map { entities ->
            entities.map { it.asExternalModel() }
        }

    override fun getMaintenanceRecordsForVehicle(vehicleId: Long): Flow<List<MaintenanceRecord>> =
        maintenanceDao.getMaintenanceRecordsForVehicle(vehicleId).map { entities ->
            entities.map { it.asExternalModel() }
        }

    override fun getMaintenanceRecordsWithPartForVehicle(vehicleId: Long): Flow<List<Pair<MaintenanceRecord, String>>> =
        maintenanceDao.getMaintenanceRecordsWithPartForVehicle(vehicleId).map { entities ->
            entities.map { it.record.asExternalModel() to it.part.name }
        }

    override fun getTotalMaintenanceCostForVehicle(vehicleId: Long): Flow<Double?> =
        maintenanceDao.getTotalMaintenanceCostForVehicle(vehicleId)

    override suspend fun insertMaintenanceRecord(record: MaintenanceRecord): Long =
        maintenanceDao.insertMaintenanceRecord(record.asEntity())

    override suspend fun updateMaintenanceRecord(record: MaintenanceRecord) =
        maintenanceDao.updateMaintenanceRecord(record.asEntity())

    override suspend fun deleteMaintenanceRecord(record: MaintenanceRecord) =
        maintenanceDao.deleteMaintenanceRecord(record.asEntity())
}
