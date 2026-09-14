package digital.tonima.mycarcompanion.core.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class OdometerRecord(
    val id: Long = 0,
    val vehicleId: Long,
    val date: Instant,
    val odometerValue: Double
)
