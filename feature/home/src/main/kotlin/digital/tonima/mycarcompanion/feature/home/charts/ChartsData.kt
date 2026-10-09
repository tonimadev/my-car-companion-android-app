package digital.tonima.mycarcompanion.feature.home.charts

import androidx.compose.runtime.Immutable
import digital.tonima.mycarcompanion.core.designsystem.model.MaintenanceStatus
import digital.tonima.mycarcompanion.core.model.FuelRecord
import digital.tonima.mycarcompanion.core.model.MaintenanceRecord
import digital.tonima.mycarcompanion.core.model.Part
import java.time.YearMonth
import java.time.ZoneId
import kotlin.time.Instant

@Immutable
data class UpcomingMaintenance(
    val partName: String,
    val dueDateMillis: Long,
    val daysLeft: Long,
    val status: MaintenanceStatus,
)

@Immutable
data class MonthlyCost(val month: YearMonth, val fuel: Double, val maintenance: Double)

/** A fuel economy sample, in km/L, taken at a refuel. */
@Immutable
data class ConsumptionPoint(val dateMillis: Long, val kmPerLiter: Double)

private const val WARNING_DAYS = 30L

/** Parts with a predicted date, soonest first. Parts without a prediction are left out. */
internal fun upcomingMaintenance(
    parts: List<Part>,
    predictions: Map<Long, Long?>,
    now: Instant,
): List<UpcomingMaintenance> = parts.mapNotNull { part ->
    val due = predictions[part.id] ?: return@mapNotNull null
    val daysLeft = (Instant.fromEpochMilliseconds(due) - now).inWholeDays
    UpcomingMaintenance(
        partName = part.name,
        dueDateMillis = due,
        daysLeft = daysLeft,
        status = when {
            daysLeft < 0 -> MaintenanceStatus.CRITICAL
            daysLeft <= WARNING_DAYS -> MaintenanceStatus.WARNING
            else -> MaintenanceStatus.OK
        },
    )
}.sortedBy { it.dueDateMillis }

/** Fuel and maintenance spending for the last [months] calendar months, oldest first, including empty months. */
internal fun monthlyCosts(
    fuels: List<FuelRecord>,
    maintenance: List<MaintenanceRecord>,
    now: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
    months: Int = 6,
): List<MonthlyCost> {
    fun Instant.toMonth(): YearMonth = YearMonth.from(java.time.Instant.ofEpochMilli(toEpochMilliseconds()).atZone(zone))

    val current = now.toMonth()
    val fuelByMonth = fuels.groupBy { it.date.toMonth() }.mapValues { (_, records) -> records.sumOf { it.totalCost } }
    val maintenanceByMonth = maintenance.groupBy { it.date.toMonth() }.mapValues { (_, records) -> records.sumOf { it.cost } }

    return (months - 1 downTo 0).map { offset ->
        val month = current.minusMonths(offset.toLong())
        MonthlyCost(month, fuelByMonth[month] ?: 0.0, maintenanceByMonth[month] ?: 0.0)
    }
}

/**
 * Consumption per refuel in km/L, oldest first: distance since the previous refuel divided by the
 * liters of the current one (same rule as `FuelRepository.calculateFuelConsumption`).
 */
internal fun consumptionSeries(fuels: List<FuelRecord>): List<ConsumptionPoint> =
    fuels.sortedBy { it.date }.zipWithNext().mapNotNull { (previous, current) ->
        val distance = current.mileage - previous.mileage
        if (current.liters <= 0 || distance <= 0) {
            null
        } else {
            ConsumptionPoint(current.date.toEpochMilliseconds(), distance / current.liters)
        }
    }
