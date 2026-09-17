package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleAiContextTest {

    private val vehicle = Vehicle(id = 1, name = "Civic 2020", currentOdometer = 50000.0)
    private val part = Part(
        id = 1,
        vehicleId = 1,
        name = "Óleo do motor",
        lifeSpanMileage = 10000.0,
        lastMaintenanceOdometer = 45000.0
    )

    @Test
    fun `build includes vehicle name and odometer in km`() {
        val context = VehicleAiContext.build(
            vehicle = vehicle,
            parts = listOf(part),
            predictions = emptyMap(),
            distanceUnit = DistanceUnit.KM
        )

        assertTrue(context.contains("Civic 2020"))
        assertTrue(context.contains("50000 km"))
    }

    @Test
    fun `build lists part remaining distance until next maintenance`() {
        val context = VehicleAiContext.build(
            vehicle = vehicle,
            parts = listOf(part),
            predictions = emptyMap(),
            distanceUnit = DistanceUnit.KM
        )

        // 45000 + 10000 - 50000 = 5000 km remaining
        assertTrue(context.contains("Óleo do motor: faltam aproximadamente 5000 km"))
    }

    @Test
    fun `build clamps remaining distance to zero when part is overdue`() {
        val overduePart = part.copy(lastMaintenanceOdometer = 30000.0)
        val context = VehicleAiContext.build(
            vehicle = vehicle,
            parts = listOf(overduePart),
            predictions = emptyMap(),
            distanceUnit = DistanceUnit.KM
        )

        assertTrue(context.contains("faltam aproximadamente 0 km"))
    }

    @Test
    fun `build reports no parts registered`() {
        val context = VehicleAiContext.build(
            vehicle = vehicle,
            parts = emptyList(),
            predictions = emptyMap(),
            distanceUnit = DistanceUnit.KM
        )

        assertTrue(context.contains("Nenhuma peça cadastrada."))
    }

    @Test
    fun `build includes fuel consumption and trend when provided`() {
        val context = VehicleAiContext.build(
            vehicle = vehicle,
            parts = emptyList(),
            predictions = emptyMap(),
            distanceUnit = DistanceUnit.KM,
            averageFuelConsumption = 12.5,
            fuelTrendLabel = "melhorando"
        )

        assertTrue(context.contains("12.5 km/l"))
        assertTrue(context.contains("melhorando"))
    }

    @Test
    fun `build reports missing fuel history`() {
        val context = VehicleAiContext.build(
            vehicle = vehicle,
            parts = emptyList(),
            predictions = emptyMap(),
            distanceUnit = DistanceUnit.KM
        )

        assertTrue(context.contains("Sem histórico de consumo suficiente."))
    }
}
