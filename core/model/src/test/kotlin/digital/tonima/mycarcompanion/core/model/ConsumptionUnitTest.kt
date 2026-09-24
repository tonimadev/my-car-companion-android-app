package digital.tonima.mycarcompanion.core.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class ConsumptionUnitTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `KM_L formats value as is`() {
        assertEquals("12.5 km/L", ConsumptionUnit.KM_L.format(12.5))
    }

    @Test
    fun `L_100KM converts km per liter to liters per 100 km`() {
        assertEquals("8.0 L/100km", ConsumptionUnit.L_100KM.format(12.5))
    }

    @Test
    fun `L_100KM returns zero for non positive consumption`() {
        assertEquals("0.0 L/100km", ConsumptionUnit.L_100KM.format(0.0))
        assertEquals("0.0 L/100km", ConsumptionUnit.L_100KM.format(-3.0))
    }

    @Test
    fun `MPG converts km per liter to miles per gallon`() {
        assertEquals("23.5 MPG", ConsumptionUnit.MPG.format(10.0))
    }
}
