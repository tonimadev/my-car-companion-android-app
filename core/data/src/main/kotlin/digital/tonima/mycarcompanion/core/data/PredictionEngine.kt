package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.model.OdometerRecord
import digital.tonima.mycarcompanion.core.model.Part
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

object PredictionEngine {

    /**
     * Calcula a data estimada para a próxima troca de uma peça considerando tanto
     * o limite de tempo (meses) quanto a quilometragem estimada por regressão linear.
     * Retorna a data mais próxima (o que vencer primeiro).
     */
    fun estimateNextMaintenanceDate(part: Part, odometerRecords: List<OdometerRecord>): Instant? {
        val lifeSpanMonths = part.lifeSpanMonths
        val lastMaintenanceDate = part.lastMaintenanceDate

        val dateBasedEstimate: Instant? = if (lifeSpanMonths != null && lastMaintenanceDate != null) {
            // Aproximação de 30.4375 dias por mês
            val daysToAdd = (lifeSpanMonths * 30.4375).toLong()
            Instant.fromEpochMilliseconds(
                lastMaintenanceDate.toEpochMilliseconds() + daysToAdd.days.inWholeMilliseconds
            )
        } else {
            null
        }

        val mileageBasedEstimate: Instant? = estimateMileageBasedDate(part, odometerRecords)

        return when {
            dateBasedEstimate != null && mileageBasedEstimate != null -> {
                if (dateBasedEstimate < mileageBasedEstimate) dateBasedEstimate else mileageBasedEstimate
            }
            dateBasedEstimate != null -> dateBasedEstimate
            else -> mileageBasedEstimate
        }
    }

    private fun estimateMileageBasedDate(part: Part, odometerRecords: List<OdometerRecord>): Instant? {
        if (odometerRecords.size < 2) return null

        // Regressão Linear Simples: y = ax + b
        // x = tempo (millis), y = hodômetro
        val x = odometerRecords.map { it.date.toEpochMilliseconds().toDouble() }
        val y = odometerRecords.map { it.odometerValue }

        val n = x.size
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y).sumOf { it.first * it.second }
        val sumXX = x.sumOf { it * it }

        val denominator = n * sumXX - sumX * sumX
        if (denominator == 0.0) return null

        val a = (n * sumXY - sumX * sumY) / denominator // km por millisecond
        val b = (sumY - a * sumX) / n // hodômetro inicial (interceptor)

        if (a <= 0) return null // Sem uso do carro ou dados inconsistentes

        val targetOdometer = part.lastMaintenanceOdometer + part.lifeSpanMileage
        val targetTimeMillis = (targetOdometer - b) / a

        if (targetTimeMillis <= 0 || targetTimeMillis.isNaN()) return null
        return Instant.fromEpochMilliseconds(targetTimeMillis.toLong())
    }
}
