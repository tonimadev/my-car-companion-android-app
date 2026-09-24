package digital.tonima.mycarcompanion.core.designsystem.model

import digital.tonima.mycarcompanion.core.model.FuelRecord
import digital.tonima.mycarcompanion.core.model.MaintenanceRecord
import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class UiModelsTest {

    private val date = Instant.fromEpochMilliseconds(1_000L)
    private val part = Part(id = 2, vehicleId = 1, name = "Oil", lifeSpanMileage = 10000.0, lastMaintenanceOdometer = 20000.0)

    @Test
    fun `part status is OK below 90 percent of life span`() {
        assertEquals(MaintenanceStatus.OK, part.toUi(currentOdometer = 28999.0).status)
    }

    @Test
    fun `part status is WARNING from 90 percent of life span`() {
        assertEquals(MaintenanceStatus.WARNING, part.toUi(currentOdometer = 29000.0).status)
        assertEquals(MaintenanceStatus.WARNING, part.toUi(currentOdometer = 29999.0).status)
    }

    @Test
    fun `part status is CRITICAL once life span is reached`() {
        assertEquals(MaintenanceStatus.CRITICAL, part.toUi(currentOdometer = 30000.0).status)
    }

    @Test
    fun `part toUi copies all fields`() {
        val full = part.copy(lifeSpanMonths = 12, lastMaintenanceDate = date)
        assertEquals(
            PartUi(2, 1, "Oil", 10000.0, 20000.0, 12, date, MaintenanceStatus.OK),
            full.toUi(currentOdometer = 20000.0)
        )
    }

    @Test
    fun `part list maps every part with the given odometer`() {
        val result = listOf(part, part.copy(id = 3, lastMaintenanceOdometer = 0.0)).toPartUiModels(currentOdometer = 25000.0)
        assertEquals(listOf(MaintenanceStatus.OK, MaintenanceStatus.CRITICAL), result.map { it.status })
    }

    @Test
    fun `vehicle maps to ui model`() {
        val vehicle = Vehicle(id = 1, name = "Civic", currentOdometer = 100.0, tankCapacity = 50.0, isCurrent = true)
        val expected = VehicleUi(1, "Civic", 100.0, 50.0, true, MaintenanceStatus.OK)
        assertEquals(expected, vehicle.toUi())
        assertEquals(listOf(expected), listOf(vehicle).toUiModels())
    }

    @Test
    fun `fuel record maps to ui model with consumption`() {
        val record = FuelRecord(id = 5, vehicleId = 1, date = date, mileage = 100.0, liters = 40.0, totalCost = 200.0, fuelType = "Gas")
        assertEquals(FuelRecordUi(5, 1, date, 100.0, 40.0, 200.0, "Gas", 12.5), record.toUi(12.5))
        assertEquals(null, record.toUi().consumptionKmPerL)
    }

    @Test
    fun `maintenance records map with part name`() {
        val record = MaintenanceRecord(id = 3, partId = 2, date = date, odometerAtMaintenance = 900.0, cost = 150.0, notes = "n")
        assertEquals(
            listOf(MaintenanceRecordUi(3, 2, "Oil", date, 900.0, 150.0, "n")),
            listOf(record to "Oil").toMaintenanceUiModels()
        )
    }
}
