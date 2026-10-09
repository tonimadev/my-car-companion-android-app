package digital.tonima.mycarcompanion.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val id: Long = 0,
    val name: String,
    val currentOdometer: Double,
    val tankCapacity: Double? = null,
    /** Typical fuel economy of the model in km/L, used until real refuel data exists. */
    val estimatedConsumption: Double? = null,
    val isCurrent: Boolean = false
)
