package digital.tonima.mycarcompanion.core.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class MaintenanceRecord(
    val id: Long = 0,
    val partId: Long,
    val date: Instant,
    val odometerAtMaintenance: Double,
    val cost: Double,
    val notes: String = ""
)
