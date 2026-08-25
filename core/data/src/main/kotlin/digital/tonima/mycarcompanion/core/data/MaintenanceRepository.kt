package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.model.MaintenanceRecord
import kotlinx.coroutines.flow.Flow

interface MaintenanceRepository {
    fun getMaintenanceRecordsForPart(partId: Long): Flow<List<MaintenanceRecord>>
    suspend fun insertMaintenanceRecord(record: MaintenanceRecord): Long
    suspend fun updateMaintenanceRecord(record: MaintenanceRecord)
    suspend fun deleteMaintenanceRecord(record: MaintenanceRecord)
}
