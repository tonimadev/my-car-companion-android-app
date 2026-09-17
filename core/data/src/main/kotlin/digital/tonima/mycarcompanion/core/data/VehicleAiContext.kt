package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle
import java.util.Locale
import kotlin.time.Instant

/**
 * Builds the plain-text vehicle summary sent to the AI as context, shared by the diagnostic
 * chat and the maintenance insight prompt.
 */
object VehicleAiContext {

    fun build(
        vehicle: Vehicle,
        parts: List<Part>,
        predictions: Map<Long, Long?>,
        distanceUnit: DistanceUnit,
        averageFuelConsumption: Double? = null,
        fuelTrendLabel: String? = null,
    ): String {
        val unit = if (distanceUnit == DistanceUnit.KM) "km" else "milhas"
        val odometer = distanceUnit.fromKm(vehicle.currentOdometer)

        val partsSection = if (parts.isEmpty()) {
            "Nenhuma peça cadastrada."
        } else {
            parts.joinToString("\n") { part ->
                val remaining = (part.lastMaintenanceOdometer + part.lifeSpanMileage) - vehicle.currentOdometer
                val remainingInUnit = distanceUnit.fromKm(remaining).let { if (it < 0) 0.0 else it }
                val predictedDate = predictions[part.id]?.let { epochMillis ->
                    Instant.fromEpochMilliseconds(epochMillis).toString().substringBefore("T")
                }
                buildString {
                    append("- ${part.name}: faltam aproximadamente ${remainingInUnit.toLong()} $unit")
                    if (predictedDate != null) append(" (previsão: $predictedDate)")
                }
            }
        }

        val consumptionSection = if (averageFuelConsumption != null) {
            "Consumo médio: ${String.format(Locale.US, "%.1f", averageFuelConsumption)} km/l" +
                (fuelTrendLabel?.let { " (tendência: $it)" } ?: "")
        } else {
            "Sem histórico de consumo suficiente."
        }

        return """
            Veículo: ${vehicle.name}
            Odômetro atual: ${odometer.toLong()} $unit

            Peças e próximas manutenções:
            $partsSection

            Combustível:
            $consumptionSection
        """.trimIndent()
    }
}
