package digital.tonima.mycarcompanion.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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