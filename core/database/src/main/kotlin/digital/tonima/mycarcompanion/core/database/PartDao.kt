package digital.tonima.mycarcompanion.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {
    @Query("SELECT * FROM parts WHERE vehicleId = :vehicleId")
    fun getPartsForVehicle(vehicleId: Long): Flow<List<PartEntity>>

    @Query("SELECT * FROM parts WHERE id = :id")
    suspend fun getPart(id: Long): PartEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: PartEntity): Long

    @Update
    suspend fun updatePart(part: PartEntity)

    @Delete
    suspend fun deletePart(part: PartEntity)
}
