package digital.tonima.mycarcompanion.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import digital.tonima.mycarcompanion.core.model.OdometerRecord
import kotlinx.datetime.Instant

@Entity(
    tableName = "odometer_records",
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
data class OdometerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: Instant,
    val odometerValue: Double
)

fun OdometerEntity.asExternalModel() = OdometerRecord(
    id = id,
    vehicleId = vehicleId,
    date = date,
    odometerValue = odometerValue
)

fun OdometerRecord.asEntity() = OdometerEntity(
    id = id,
    vehicleId = vehicleId,
    date = date,
    odometerValue = odometerValue
)
