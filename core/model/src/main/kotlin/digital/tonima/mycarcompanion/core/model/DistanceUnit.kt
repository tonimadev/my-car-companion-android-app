package digital.tonima.mycarcompanion.core.model

enum class DistanceUnit {
    KM, MILES;

    fun toKm(value: Double): Double = when (this) {
        KM -> value
        MILES -> value / 0.621371
    }

    fun fromKm(value: Double): Double = when (this) {
        KM -> value
        MILES -> value * 0.621371
    }
}
