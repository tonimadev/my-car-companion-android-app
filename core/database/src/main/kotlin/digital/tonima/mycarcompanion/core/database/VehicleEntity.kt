package digital.tonima.mycarcompanion.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import digital.tonima.mycarcompanion.core.model.Vehicle

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currentOdometer: Double,
    val isCurrent: Boolean
)

fun VehicleEntity.asExternalModel() = Vehicle(
    id = id,
    name = name,
    currentOdometer = currentOdometer,
    isCurrent = isCurrent
)

fun Vehicle.asEntity() = VehicleEntity(
    id = id,
    name = name,
    currentOdometer = currentOdometer,
    isCurrent = isCurrent
)
