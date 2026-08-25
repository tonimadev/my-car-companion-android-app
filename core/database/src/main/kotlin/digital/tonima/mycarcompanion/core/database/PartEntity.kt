package digital.tonima.mycarcompanion.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import digital.tonima.mycarcompanion.core.model.Part

@Entity(
    tableName = "parts",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vehicleId"])]
)
data class PartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val name: String,
    val lifeSpanMileage: Double,
    val lastMaintenanceOdometer: Double
)

fun PartEntity.asExternalModel() = Part(
    id = id,
    vehicleId = vehicleId,
    name = name,
    lifeSpanMileage = lifeSpanMileage,
    lastMaintenanceOdometer = lastMaintenanceOdometer
)

fun Part.asEntity() = PartEntity(
    id = id,
    vehicleId = vehicleId,
    name = name,
    lifeSpanMileage = lifeSpanMileage,
    lastMaintenanceOdometer = lastMaintenanceOdometer
)
