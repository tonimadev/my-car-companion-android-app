package digital.tonima.mycarcompanion.core.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class FuelRecord(
    val id: Long = 0,
    val vehicleId: Long,
    val date: Instant,
    val mileage: Double,
    val liters: Double,
    val totalCost: Double,
    val fuelType: String
)