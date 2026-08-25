package digital.tonima.mycarcompanion.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles")
    fun getVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicle(id: Long): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentVehicle(): Flow<VehicleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)
    
    @Query("UPDATE vehicles SET isCurrent = 0")
    suspend fun clearCurrentVehicle()
    
    @Query("UPDATE vehicles SET isCurrent = 1 WHERE id = :id")
    suspend fun setCurrentVehicle(id: Long)
}
