package digital.tonima.mycarcompanion.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OdometerDao {
    @Query("SELECT * FROM odometer_records WHERE vehicleId = :vehicleId ORDER BY date ASC")
    fun getOdometerRecordsForVehicle(vehicleId: Long): Flow<List<OdometerEntity>>

    @Insert
    suspend fun insertOdometerRecord(record: OdometerEntity): Long

    @Update
    suspend fun updateOdometerRecord(record: OdometerEntity)

    @Delete
    suspend fun deleteOdometerRecord(record: OdometerEntity)
}
