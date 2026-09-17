package digital.tonima.mycarcompanion.core.notifications

import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceAlertPolicyTest {

    private val vehicle = Vehicle(id = 1, name = "Civic 2020", currentOdometer = 9600.0)
    private val part = Part(
        id = 1,
        vehicleId = 1,
        name = "Óleo do motor",
        lifeSpanMileage = 10000.0,
        lastMaintenanceOdometer = 0.0
    )

    @Test
    fun `is not due when remaining mileage is well above the threshold`() {
        val farFromDue = vehicle.copy(currentOdometer = 1000.0) // 9000 km remaining

        assertFalse(MaintenanceAlertPolicy.isMaintenanceDue(part, farFromDue))
    }

    @Test
    fun `is due when remaining mileage drops below 500km`() {
        val almostDue = vehicle.copy(currentOdometer = 9600.0) // 400 km remaining

        assertTrue(MaintenanceAlertPolicy.isMaintenanceDue(part, almostDue))
    }

    @Test
    fun `is due when the part is already overdue`() {
        val overdue = vehicle.copy(currentOdometer = 11000.0) // -1000 km remaining

        assertTrue(MaintenanceAlertPolicy.isMaintenanceDue(part, overdue))
    }

    @Test
    fun `is not due exactly at the 500km boundary`() {
        val atBoundary = vehicle.copy(currentOdometer = 9500.0) // exactly 500 km remaining

        assertFalse(MaintenanceAlertPolicy.isMaintenanceDue(part, atBoundary))
    }

    @Test
    fun `threshold is evaluated in kilometers regardless of the user's display unit`() {
        // Regression test: the worker used to convert the remaining distance to the user's
        // selected display unit (e.g. miles) before comparing it to the fixed 500 threshold,
        // which made alerts fire far too early for MILES users (500 miles ~= 804 km).
        // The policy must not take a display unit into account at all.
        val vehicleWith600KmRemaining = vehicle.copy(currentOdometer = 9400.0)

        assertFalse(MaintenanceAlertPolicy.isMaintenanceDue(part, vehicleWith600KmRemaining))
    }
}
