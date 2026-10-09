package digital.tonima.mycarcompanion.core.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleSpecsParserTest {

    @Test
    fun `parses a plain JSON answer`() {
        val specs = VehicleSpecsParser.parse(
            """
            {"tankLiters": 54, "consumptionKmPerLiter": 12.5,
             "services": [
               {"part": "engine_oil", "km": 10000, "months": 12},
               {"part": "timing_belt", "km": 60000, "months": null}
             ]}
            """
        )!!

        assertEquals(54.0, specs.tankLiters!!, 0.0)
        assertEquals(12.5, specs.consumptionKmPerLiter!!, 0.0)
        assertEquals(
            listOf(
                ServiceInterval(ServicePart.ENGINE_OIL, 10_000.0, 12),
                ServiceInterval(ServicePart.TIMING_BELT, 60_000.0, null)
            ),
            specs.intervals
        )
    }

    @Test
    fun `tolerates markdown code fences`() {
        val specs = VehicleSpecsParser.parse("```json\n{\"tankLiters\": 45}\n```")

        assertEquals(45.0, specs!!.tankLiters!!, 0.0)
    }

    @Test
    fun `drops implausible values and unknown parts`() {
        val specs = VehicleSpecsParser.parse(
            """
            {"tankLiters": 5000, "consumptionKmPerLiter": 0.1,
             "services": [
               {"part": "engine_oil", "km": 5, "months": 12},
               {"part": "warp_drive", "km": 10000, "months": 12},
               {"part": "coolant", "km": 50000, "months": 999},
               {"part": "coolant", "km": 40000, "months": 24}
             ]}
            """
        )!!

        assertNull(specs.tankLiters)
        assertNull(specs.consumptionKmPerLiter)
        // The out-of-range months is dropped but the km interval is kept; the duplicate part is ignored.
        assertEquals(listOf(ServiceInterval(ServicePart.COOLANT, 50_000.0, null)), specs.intervals)
    }

    @Test
    fun `returns null when nothing usable is left or the text is not JSON`() {
        assertNull(VehicleSpecsParser.parse("I cannot help with that"))
        assertNull(VehicleSpecsParser.parse("""{"tankLiters": null, "consumptionKmPerLiter": null, "services": []}"""))
    }
}

class OnlineFipeRepositoryTest {

    private class FakeApi(val responses: Map<String, String>) : FipeApi {
        val calls = mutableListOf<String>()
        override suspend fun get(path: String): String {
            calls += path
            return responses[path] ?: throw java.io.IOException("offline")
        }
    }

    private val api = FakeApi(
        mapOf(
            "marcas" to """[{"codigo":"23","nome":"Chevrolet"},{"codigo":"59","nome":"VW - VolksWagen"}]""",
            "marcas/23/modelos" to """{"modelos":[{"codigo":4501,"nome":"PRISMA Sed. LT 1.4 8V FlexPower 4p"}],"anos":[]}""",
            "marcas/23/modelos/4501/anos" to """[{"codigo":"2019-1","nome":"2019 Gasolina"},{"codigo":"32000-1","nome":"32000 Gasolina"}]"""
        )
    )
    private val repository = OnlineFipeRepository(api)

    @Test
    fun `loads brands models and years`() = runTest {
        assertEquals(listOf("Chevrolet", "VW - VolksWagen"), repository.brands().getOrThrow().map { it.name })

        val model = repository.models("23").getOrThrow().single()
        assertEquals(FipeItem("4501", "PRISMA Sed. LT 1.4 8V FlexPower 4p"), model)

        val years = repository.years("23", model.code).getOrThrow()
        assertEquals(listOf(2019, null), years.map { it.modelYear() })
    }

    @Test
    fun `results are cached for the process lifetime`() = runTest {
        repository.brands()
        repository.brands()

        assertEquals(1, api.calls.count { it == "marcas" })
    }

    @Test
    fun `network errors and unexpected payloads become failures`() = runTest {
        assertTrue(repository.models("999").isFailure)

        val garbage = OnlineFipeRepository(FakeApi(mapOf("marcas" to "<html>")))
        assertTrue(garbage.brands().isFailure)
    }
}
