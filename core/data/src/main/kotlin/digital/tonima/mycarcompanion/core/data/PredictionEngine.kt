package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.model.OdometerRecord
import digital.tonima.mycarcompanion.core.model.Part
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

object PredictionEngine {

    /**
     * Calcula a data estimada para a próxima troca de uma peça.
     * Retorna null se não houver dados históricos suficientes.
     */
    fun estimateNextMaintenanceDate(part: Part, odometerRecords: List<OdometerRecord>): Instant? {
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
        
        // targetOdometer = a * targetTime + b
        // targetTime = (targetOdometer - b) / a
        
        val targetTimeMillis = (targetOdometer - b) / a
        return Instant.fromEpochMilliseconds(targetTimeMillis.toLong())
    }
}
