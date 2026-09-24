package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.model.ConsumptionUnit
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle
import java.util.Locale
import kotlin.time.Instant

/**
 * Builds the plain-text vehicle summary sent to the AI as context, shared by the diagnostic
 * chat and the maintenance insight prompt. The summary is written in English for the model and
 * states the user's language and units, so the answer can follow them.
 */
object VehicleAiContext {

    fun build(
        vehicle: Vehicle,
        parts: List<Part>,
        predictions: Map<Long, Long?>,
        distanceUnit: DistanceUnit,
        consumptionUnit: ConsumptionUnit = ConsumptionUnit.KM_L,
        averageFuelConsumption: Double? = null,
        fuelTrendLabel: String? = null,
        userLocale: Locale = Locale.getDefault(),
    ): String {
        val unit = distanceUnit.symbol
        val odometer = distanceUnit.fromKm(vehicle.currentOdometer)

        val partsSection = if (parts.isEmpty()) {
            "No parts registered."
        } else {
            parts.joinToString("\n") { part ->
                val remaining = (part.lastMaintenanceOdometer + part.lifeSpanMileage) - vehicle.currentOdometer
                val remainingInUnit = distanceUnit.fromKm(remaining).let { if (it < 0) 0.0 else it }
                val predictedDate = predictions[part.id]?.let { epochMillis ->
                    Instant.fromEpochMilliseconds(epochMillis).toString().substringBefore("T")
                }
                buildString {
                    append("- ${part.name}: about ${remainingInUnit.toLong()} $unit remaining")
                    if (predictedDate != null) append(" (predicted: $predictedDate)")
                }
            }
        }

        val consumptionSection = if (averageFuelConsumption != null) {
            "Average consumption: ${consumptionUnit.format(averageFuelConsumption, Locale.US)}" +
                (fuelTrendLabel?.let { " (trend: $it)" } ?: "")
        } else {
            "Not enough fuel history."
        }

        return """
            User language: ${userLocale.getDisplayName(Locale.ENGLISH)} (${userLocale.toLanguageTag()})
            User units: distance in $unit, fuel economy in ${consumptionUnit.symbol}

            Vehicle: ${vehicle.name}
            Current odometer: ${odometer.toLong()} $unit

            Parts and upcoming maintenance:
            $partsSection

            Fuel:
            $consumptionSection
        """.trimIndent()
    }
}
