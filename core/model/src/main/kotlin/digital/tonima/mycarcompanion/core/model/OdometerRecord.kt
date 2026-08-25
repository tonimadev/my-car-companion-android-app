package digital.tonima.mycarcompanion.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class OdometerRecord(
    val id: Long = 0,
    val vehicleId: Long,
    val date: Instant,
    val odometerValue: Double
)
