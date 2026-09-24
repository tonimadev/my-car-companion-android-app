package digital.tonima.mycarcompanion.core.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class ConsumptionUnit(val symbol: String) {
    KM_L("km/L"), L_100KM("L/100km"), MPG("MPG");

    /** Formats a consumption given in km/L as this unit, using [locale] for the number. */
    fun format(value: Double, locale: Locale = Locale.getDefault()): String = when (this) {
        KM_L -> "%.1f $symbol".format(locale, value)
        L_100KM -> {
            val l100 = if (value > 0) 100.0 / value else 0.0
            "%.1f $symbol".format(locale, l100)
        }
        MPG -> {
            val mpg = value * 2.35215
            "%.1f $symbol".format(locale, mpg)
        }
    }

    companion object {
        // MPG here is US gallons, so it is only the default where US gallons are used.
        private val MPG_REGIONS = setOf("US", "LR", "MM")
        private val KM_L_REGIONS = setOf("BR", "JP", "IN", "MX")

        /** Fuel economy unit customarily used in the given ISO 3166 [countryCode]. */
        fun defaultForRegion(countryCode: String): ConsumptionUnit = when (countryCode.uppercase()) {
            in MPG_REGIONS -> MPG
            in KM_L_REGIONS -> KM_L
            else -> L_100KM
        }
    }
}
