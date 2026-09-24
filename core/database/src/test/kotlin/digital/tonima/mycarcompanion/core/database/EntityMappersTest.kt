package digital.tonima.mycarcompanion.core.database

import digital.tonima.mycarcompanion.core.model.FuelRecord
import digital.tonima.mycarcompanion.core.model.MaintenanceRecord
import digital.tonima.mycarcompanion.core.model.OdometerRecord
import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class EntityMappersTest {

    private val date = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    @Test
    fun `vehicle maps to entity and back`() {
        val vehicle = Vehicle(id = 1, name = "Civic", currentOdometer = 42000.0, tankCapacity = 50.0, isCurrent = true)
        val entity = vehicle.asEntity()
        assertEquals(VehicleEntity(1, "Civic", 42000.0, 50.0, true), entity)
        assertEquals(vehicle, entity.asExternalModel())
    }

    @Test
    fun `part maps to entity and back`() {
        val part = Part(
            id = 2,
            vehicleId = 1,
            name = "Oil",
            lifeSpanMileage = 10000.0,
            lastMaintenanceOdometer = 40000.0,
            lifeSpanMonths = 12,
            lastMaintenanceDate = date
        )
        val entity = part.asEntity()
        assertEquals(PartEntity(2, 1, "Oil", 10000.0, 40000.0, 12, date), entity)
        assertEquals(part, entity.asExternalModel())
    }

    @Test
    fun `part without optional fields maps to entity and back`() {
        val part = Part(vehicleId = 1, name = "Tires", lifeSpanMileage = 50000.0, lastMaintenanceOdometer = 0.0)
        assertEquals(part, part.asEntity().asExternalModel())
    }

    @Test
    fun `maintenance record maps to entity and back`() {
        val record = MaintenanceRecord(id = 3, partId = 2, date = date, odometerAtMaintenance = 40000.0, cost = 250.0, notes = "Synthetic")
        val entity = record.asEntity()
        assertEquals(MaintenanceEntity(3, 2, date, 40000.0, 250.0, "Synthetic"), entity)
        assertEquals(record, entity.asExternalModel())
    }

    @Test
    fun `odometer record maps to entity and back`() {
        val record = OdometerRecord(id = 4, vehicleId = 1, date = date, odometerValue = 42100.0)
        val entity = record.asEntity()
        assertEquals(OdometerEntity(4, 1, date, 42100.0), entity)
        assertEquals(record, entity.asExternalModel())
    }

    @Test
    fun `fuel record maps to entity and back`() {
        val record = FuelRecord(id = 5, vehicleId = 1, date = date, mileage = 42000.0, liters = 40.0, totalCost = 240.0, fuelType = "Gasoline")
        val entity = record.asEntity()
        assertEquals(FuelEntity(5, 1, date, 42000.0, 40.0, 240.0, "Gasoline"), entity)
        assertEquals(record, entity.asExternalModel())
    }
}
