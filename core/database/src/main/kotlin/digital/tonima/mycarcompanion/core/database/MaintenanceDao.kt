package digital.tonima.mycarcompanion.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_records WHERE partId = :partId ORDER BY date DESC")
    fun getMaintenanceRecordsForPart(partId: Long): Flow<List<MaintenanceEntity>>

    @Query("""
        SELECT maintenance_records.* FROM maintenance_records 
        INNER JOIN parts ON maintenance_records.partId = parts.id 
        WHERE parts.vehicleId = :vehicleId 
        ORDER BY date DESC
    """)
    fun getMaintenanceRecordsForVehicle(vehicleId: Long): Flow<List<MaintenanceEntity>>

    @Query("""
        SELECT SUM(cost) FROM maintenance_records 
        INNER JOIN parts ON maintenance_records.partId = parts.id 
        WHERE parts.vehicleId = :vehicleId
    """)
    fun getTotalMaintenanceCostForVehicle(vehicleId: Long): Flow<Double?>

    @Insert
    suspend fun insertMaintenanceRecord(record: MaintenanceEntity): Long

    @Update
    suspend fun updateMaintenanceRecord(record: MaintenanceEntity)

    @Delete
    suspend fun deleteMaintenanceRecord(record: MaintenanceEntity)
}
