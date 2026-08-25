package digital.tonima.mycarcompanion.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import digital.tonima.mycarcompanion.core.model.MaintenanceRecord
import kotlinx.datetime.Instant

@Entity(
    tableName = "maintenance_records",
    foreignKeys = [
        ForeignKey(
            entity = PartEntity::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["partId"])]
)
data class MaintenanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partId: Long,
    val date: Instant,
    val odometerAtMaintenance: Double,
    val cost: Double,
    val notes: String
)

fun MaintenanceEntity.asExternalModel() = MaintenanceRecord(
    id = id,
    partId = partId,
    date = date,
    odometerAtMaintenance = odometerAtMaintenance,
    cost = cost,
    notes = notes
)

fun MaintenanceRecord.asEntity() = MaintenanceEntity(
    id = id,
    partId = partId,
    date = date,
    odometerAtMaintenance = odometerAtMaintenance,
    cost = cost,
    notes = notes
)
