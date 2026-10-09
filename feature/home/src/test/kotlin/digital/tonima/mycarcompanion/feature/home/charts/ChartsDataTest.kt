package digital.tonima.mycarcompanion.feature.home.charts

import digital.tonima.mycarcompanion.core.designsystem.model.MaintenanceStatus
import digital.tonima.mycarcompanion.core.model.FuelRecord
import digital.tonima.mycarcompanion.core.model.MaintenanceRecord
import digital.tonima.mycarcompanion.core.model.Part
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class ChartsDataTest {

    private val now = Instant.parse("2026-10-09T12:00:00Z")

    private fun part(id: Long, name: String) =
        Part(id = id, vehicleId = 1, name = name, lifeSpanMileage = 10000.0, lastMaintenanceOdometer = 0.0)

    private fun fuel(date: Instant, mileage: Double, liters: Double, cost: Double) =
        FuelRecord(vehicleId = 1, date = date, mileage = mileage, liters = liters, totalCost = cost, fuelType = "gasoline")

    @Test
    fun `upcoming maintenance is sorted by date and classified by days left`() {
        val parts = listOf(part(1, "Oil"), part(2, "Belt"), part(3, "Tires"), part(4, "No date"))
        val predictions = mapOf(
            1L to (now + 100.days).toEpochMilliseconds(),
            2L to (now - 5.days).toEpochMilliseconds(),
            3L to (now + 10.days).toEpochMilliseconds(),
            4L to null,
        )

        val result = upcomingMaintenance(parts, predictions, now)

        assertEquals(listOf("Belt", "Tires", "Oil"), result.map { it.partName })
        assertEquals(
            listOf(MaintenanceStatus.CRITICAL, MaintenanceStatus.WARNING, MaintenanceStatus.OK),
            result.map { it.status }
        )
        assertEquals(10L, result[1].daysLeft)
    }

    @Test
    fun `monthly costs cover the last months including empty ones`() {
        val fuels = listOf(
            fuel(Instant.parse("2026-10-02T10:00:00Z"), 1000.0, 40.0, 240.0),
            fuel(Instant.parse("2026-10-08T10:00:00Z"), 1400.0, 40.0, 250.0),
            fuel(Instant.parse("2026-08-15T10:00:00Z"), 600.0, 30.0, 180.0),
        )
        val maintenance = listOf(
            MaintenanceRecord(partId = 1, date = Instant.parse("2026-10-05T10:00:00Z"), odometerAtMaintenance = 1200.0, cost = 300.0),
        )

        val result = monthlyCosts(fuels, maintenance, now, ZoneOffset.UTC, months = 3)

        assertEquals(listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10)), result.map { it.month })
        assertEquals(listOf(180.0, 0.0, 490.0), result.map { it.fuel })
        assertEquals(listOf(0.0, 0.0, 300.0), result.map { it.maintenance })
    }

    @Test
    fun `consumption series uses distance since the previous refuel and skips invalid pairs`() {
        val fuels = listOf(
            fuel(now - 30.days, 1000.0, 40.0, 0.0),
            fuel(now - 20.days, 1400.0, 40.0, 0.0),
            fuel(now - 10.days, 1400.0, 30.0, 0.0), // no distance travelled
            fuel(now, 1700.0, 30.0, 0.0),
        )

        val result = consumptionSeries(fuels.shuffled())

        assertEquals(listOf(10.0, 10.0), result.map { it.kmPerLiter })
        assertEquals(listOf((now - 20.days).toEpochMilliseconds(), now.toEpochMilliseconds()), result.map { it.dateMillis })
    }
}
