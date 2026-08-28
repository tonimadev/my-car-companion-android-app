package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.model.OdometerRecord
import digital.tonima.mycarcompanion.core.model.Part
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.days

class PredictionEngineTest {

    @Test
    fun `estimateNextMaintenanceDate returns null when no records and no months`() {
        val part = Part(
            id = 1,
            vehicleId = 1,
            name = "Test Part",
            lifeSpanMileage = 5000.0,
            lastMaintenanceOdometer = 0.0,
            lifeSpanMonths = null,
            lastMaintenanceDate = null
        )
        val result = PredictionEngine.estimateNextMaintenanceDate(part, emptyList())
        assertNull(result)
    }

    @Test
    fun `estimateNextMaintenanceDate uses time-based estimate when it is sooner`() {
        val lastMaintDate = Instant.fromEpochMilliseconds(1000000000000L) // Some fixed date
        val part = Part(
            id = 1,
            vehicleId = 1,
            name = "Test Part",
            lifeSpanMileage = 100000.0, // Very large mileage
            lastMaintenanceOdometer = 0.0,
            lifeSpanMonths = 12,
            lastMaintenanceDate = lastMaintDate
        )
        
        // Engine logic: val daysToAdd = (lifeSpanMonths * 30.4375).toLong()
        val daysToAdd = (12 * 30.4375).toLong()
        val expectedTime = lastMaintDate.toEpochMilliseconds() + (daysToAdd * 24 * 60 * 60 * 1000)
        
        val result = PredictionEngine.estimateNextMaintenanceDate(part, emptyList())
        
        assertNotNull(result)
        assertEquals(expectedTime, result?.toEpochMilliseconds())
    }

    @Test
    fun `estimateNextMaintenanceDate uses mileage-based estimate when it is sooner`() {
        val lastMaintDate = Instant.fromEpochMilliseconds(1000000000000L)
        val part = Part(
            id = 1,
            vehicleId = 1,
            name = "Test Part",
            lifeSpanMileage = 1000.0, // Short mileage
            lastMaintenanceOdometer = 0.0,
            lifeSpanMonths = 120, // Very long time
            lastMaintenanceDate = lastMaintDate
        )
        
        // 2 records to allow linear regression
        // Day 0: 0 km
        // Day 10: 500 km -> rate = 50 km/day (50/ (24*60*60*1000) km/ms)
        // Target 1000 km -> should be Day 20
        val records = listOf(
            OdometerRecord(1, 1, lastMaintDate, 0.0),
            OdometerRecord(2, 1, lastMaintDate + 10.days, 500.0)
        )
        
        val result = PredictionEngine.estimateNextMaintenanceDate(part, records)
        
        assertNotNull(result)
        val expectedTime = (lastMaintDate + 20.days).toEpochMilliseconds()
        // Use delta for floating point precision in regression
        assertEquals(expectedTime.toDouble(), result?.toEpochMilliseconds()?.toDouble() ?: 0.0, 1000.0)
    }
}
