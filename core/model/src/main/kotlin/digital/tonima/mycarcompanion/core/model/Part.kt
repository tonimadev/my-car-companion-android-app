package digital.tonima.mycarcompanion.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Part(
    val id: Long = 0,
    val vehicleId: Long,
    val name: String,
    val lifeSpanMileage: Double,
    val lastMaintenanceOdometer: Double
)
