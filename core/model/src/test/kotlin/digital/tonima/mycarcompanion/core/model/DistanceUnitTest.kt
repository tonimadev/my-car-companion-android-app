package digital.tonima.mycarcompanion.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceUnitTest {

    @Test
    fun `KM conversions are identity`() {
        assertEquals(123.4, DistanceUnit.KM.toKm(123.4), 0.0)
        assertEquals(123.4, DistanceUnit.KM.fromKm(123.4), 0.0)
    }

    @Test
    fun `MILES toKm converts miles to kilometers`() {
        assertEquals(160.934, DistanceUnit.MILES.toKm(100.0), 0.001)
    }

    @Test
    fun `MILES fromKm converts kilometers to miles`() {
        assertEquals(62.1371, DistanceUnit.MILES.fromKm(100.0), 0.0001)
    }

    @Test
    fun `MILES round trip preserves value`() {
        val km = 4321.0
        assertEquals(km, DistanceUnit.MILES.toKm(DistanceUnit.MILES.fromKm(km)), 1e-9)
    }

    @Test
    fun `symbols are international abbreviations`() {
        assertEquals("km", DistanceUnit.KM.symbol)
        assertEquals("mi", DistanceUnit.MILES.symbol)
    }

    @Test
    fun `default unit follows region`() {
        assertEquals(DistanceUnit.MILES, DistanceUnit.defaultForRegion("US"))
        assertEquals(DistanceUnit.MILES, DistanceUnit.defaultForRegion("gb"))
        assertEquals(DistanceUnit.KM, DistanceUnit.defaultForRegion("BR"))
        assertEquals(DistanceUnit.KM, DistanceUnit.defaultForRegion("DE"))
        assertEquals(DistanceUnit.KM, DistanceUnit.defaultForRegion(""))
    }
}
