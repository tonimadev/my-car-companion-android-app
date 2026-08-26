package digital.tonima.mycarcompanion.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import digital.tonima.mycarcompanion.core.model.FuelRecord
import kotlin.time.Instant

@Entity(
    tableName = "fuel_records",
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
data class FuelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: Instant,
    val mileage: Double,
    val liters: Double,
    val totalCost: Double,
    val fuelType: String
)

fun FuelEntity.asExternalModel() = FuelRecord(
    id = id,
    vehicleId = vehicleId,
    date = date,
    mileage = mileage,
    liters = liters,
    totalCost = totalCost,
    fuelType = fuelType
)

fun FuelRecord.asEntity() = FuelEntity(
    id = id,
    vehicleId = vehicleId,
    date = date,
    mileage = mileage,
    liters = liters,
    totalCost = totalCost,
    fuelType = fuelType
)