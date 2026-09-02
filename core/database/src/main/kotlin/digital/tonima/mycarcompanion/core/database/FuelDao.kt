package digital.tonima.mycarcompanion.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getFuelRecordsForVehicle(vehicleId: Long): Flow<List<FuelEntity>>

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY date DESC LIMIT 1")
    fun getLastFuelRecordForVehicle(vehicleId: Long): Flow<FuelEntity?>

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId AND date < :currentDate ORDER BY date DESC LIMIT 1")
    fun getPreviousFuelRecord(vehicleId: Long, currentDate: Long): Flow<FuelEntity?>

    @Query("SELECT SUM(totalCost) FROM fuel_records WHERE vehicleId = :vehicleId")
    fun getTotalFuelCostForVehicle(vehicleId: Long): Flow<Double?>

    @Query("SELECT * FROM fuel_records WHERE id = :id")
    fun getFuelRecord(id: Long): Flow<FuelEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelRecord(record: FuelEntity): Long

    @Update
    suspend fun updateFuelRecord(record: FuelEntity)

    @Delete
    suspend fun deleteFuelRecord(record: FuelEntity)
}