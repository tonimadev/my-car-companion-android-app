package digital.tonima.mycarcompanion.core.database

import androidx.room.Embedded
import androidx.room.Relation

data class MaintenanceRecordWithPart(
    @Embedded val record: MaintenanceEntity,
    @Relation(
        parentColumn = "partId",
        entityColumn = "id"
    )
    val part: PartEntity
)
