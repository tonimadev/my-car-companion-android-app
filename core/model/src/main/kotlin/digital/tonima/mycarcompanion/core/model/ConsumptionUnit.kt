package digital.tonima.mycarcompanion.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ConsumptionUnit {
    KM_L, L_100KM, MPG;

    fun format(value: Double): String = when (this) {
        KM_L -> "%.1f km/L".format(value)
        L_100KM -> {
            val l100 = if (value > 0) 100.0 / value else 0.0
            "%.1f L/100km".format(l100)
        }
        MPG -> {
            val mpg = value * 2.35215
            "%.1f MPG".format(mpg)
        }
    }
}
