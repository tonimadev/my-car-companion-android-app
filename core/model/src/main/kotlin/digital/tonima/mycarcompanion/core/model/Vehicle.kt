package digital.tonima.mycarcompanion.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val id: Long = 0,
    val name: String,
    val currentOdometer: Double,
    val isCurrent: Boolean = false
)
