package digital.tonima.mycarcompanion.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class MaintenanceRecord(
    val id: Long = 0,
    val partId: Long,
    val date: Instant,
    val odometerAtMaintenance: Double,
    val cost: Double,
    val notes: String = ""
)
