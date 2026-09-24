package digital.tonima.mycarcompanion.core.model

enum class DistanceUnit(val symbol: String) {
    KM("km"), MILES("mi");

    fun toKm(value: Double): Double = when (this) {
        KM -> value
        MILES -> value / 0.621371
    }

    fun fromKm(value: Double): Double = when (this) {
        KM -> value
        MILES -> value * 0.621371
    }

    companion object {
        private val MILES_REGIONS = setOf("US", "GB", "LR", "MM")

        /** Unit customarily used for road distances in the given ISO 3166 [countryCode]. */
        fun defaultForRegion(countryCode: String): DistanceUnit =
            if (countryCode.uppercase() in MILES_REGIONS) MILES else KM
    }
}
