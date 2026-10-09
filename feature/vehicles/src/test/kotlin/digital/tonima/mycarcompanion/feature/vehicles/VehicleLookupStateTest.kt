package digital.tonima.mycarcompanion.feature.vehicles

import digital.tonima.mycarcompanion.core.data.FipeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleLookupStateTest {

    private val brand = FipeItem("23", "GM - Chevrolet")
    private val model = FipeItem("4501", "PRISMA Sed. LT 1.4 8V FlexPower 4p")
    private val year = FipeItem("2019-1", "2019 Flex")

    @Test
    fun `description joins brand without its group prefix, model and year`() {
        val state = VehicleLookupState(brand = brand, model = model, year = year)

        assertEquals("Chevrolet PRISMA Sed. LT 1.4 8V FlexPower 4p 2019", state.description)
    }

    @Test
    fun `description is null until brand model and year are all chosen`() {
        assertNull(VehicleLookupState(brand = brand, model = model).description)
        assertNull(VehicleLookupState(brand = brand).description)
        assertNull(VehicleLookupState().description)
    }
}
