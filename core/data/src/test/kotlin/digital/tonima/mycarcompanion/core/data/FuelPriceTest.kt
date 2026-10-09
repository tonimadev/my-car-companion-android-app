package digital.tonima.mycarcompanion.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private const val SAMPLE = """
{
  "error": false,
  "message": "ok",
  "data_coleta": "2026-10-09 16:34:49",
  "moeda": "BRL",
  "precos": {
    "gasolina": { "br": "6,55", "sp": "6,36", "rs": "6,24" },
    "diesel": { "br": "7,10", "sp": "7,29" }
  }
}
"""

class FuelPriceParserTest {

    @Test
    fun `parses comma decimal prices per state and fuel`() {
        val snapshot = FuelPriceParser.parse(SAMPLE)!!

        assertEquals("2026-10-09 16:34:49", snapshot.collectedAt)
        assertEquals(6.36, snapshot.priceFor(FuelKind.GASOLINE, "sp")!!.value, 0.0)
        assertEquals(7.29, snapshot.priceFor(FuelKind.DIESEL, "SP")!!.value, 0.0)
    }

    @Test
    fun `falls back to the national average when the state has no data`() {
        val snapshot = FuelPriceParser.parse(SAMPLE)!!

        val price = snapshot.priceFor(FuelKind.DIESEL, "ba")!!

        assertEquals(7.10, price.value, 0.0)
        assertTrue(price.isNationalAverage)
        assertTrue(snapshot.priceFor(FuelKind.GASOLINE, null)!!.isNationalAverage)
        assertFalse(snapshot.priceFor(FuelKind.GASOLINE, "rs")!!.isNationalAverage)
    }

    @Test
    fun `returns null for errors and malformed payloads`() {
        assertNull(FuelPriceParser.parse("not json"))
        assertNull(FuelPriceParser.parse("""{"error": true, "precos": {"gasolina": {"br": "6,55"}}}"""))
        assertNull(FuelPriceParser.parse("""{"error": false}"""))
        assertNull(FuelPriceParser.parse("""{"error": false, "precos": {"gasolina": {"br": "abc"}}}"""))
    }

    @Test
    fun `ignores implausible prices`() {
        val snapshot = FuelPriceParser.parse(
            """{"precos": {"gasolina": {"br": "6,55", "sp": "0,00", "rj": "999,00"}}}"""
        )!!

        assertNull(snapshot.prices.getValue(FuelKind.GASOLINE)["sp"])
        assertNull(snapshot.prices.getValue(FuelKind.GASOLINE)["rj"])
    }
}

class FuelPriceRegionTest {
    private val newYork = java.util.TimeZone.getTimeZone("America/New_York")
    private val saoPaulo = java.util.TimeZone.getTimeZone("America/Sao_Paulo")

    @Test
    fun `enabled by Brazilian region or Brazilian time zone`() {
        assertTrue(isFuelPriceRegion(java.util.Locale.forLanguageTag("pt-BR"), newYork))
        assertTrue(isFuelPriceRegion(java.util.Locale.US, saoPaulo))
        assertFalse(isFuelPriceRegion(java.util.Locale.US, newYork))
        assertFalse(isFuelPriceRegion(java.util.Locale.forLanguageTag("pt-PT"), java.util.TimeZone.getTimeZone("Europe/Lisbon")))
    }
}

class OnlineFuelPriceRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class FakeApi(var response: () -> String) : FuelPriceApi {
        var calls = 0
        override suspend fun fetch(): String {
            calls++
            return response()
        }
    }

    private fun TestScope.repository(api: FuelPriceApi) = OnlineFuelPriceRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            tmp.newFile("prices.preferences_pb").also { it.delete() }
        },
        api
    )

    @Test
    fun `refresh stores the snapshot`() = runTest {
        val repository = repository(FakeApi { SAMPLE })

        assertNull(repository.snapshot.first())
        repository.refresh(force = false, nowMillis = 1_000L)

        assertNotNull(repository.snapshot.first())
    }

    @Test
    fun `recent cache is not refetched unless forced`() = runTest {
        val api = FakeApi { SAMPLE }
        val repository = repository(api)

        repository.refresh(force = false, nowMillis = 0L)
        repository.refresh(force = false, nowMillis = 60 * 60 * 1000L)
        assertEquals(1, api.calls)

        repository.refresh(force = false, nowMillis = 3 * 60 * 60 * 1000L)
        assertEquals(2, api.calls)

        repository.refresh(force = true, nowMillis = 3 * 60 * 60 * 1000L + 1)
        assertEquals(3, api.calls)
    }

    @Test
    fun `network failure or bad payload keeps the previous cache`() = runTest {
        val api = FakeApi { SAMPLE }
        val repository = repository(api)
        repository.refresh(force = false, nowMillis = 0L)

        api.response = { throw java.io.IOException("offline") }
        repository.refresh(force = true, nowMillis = 10L)
        api.response = { "garbage" }
        repository.refresh(force = true, nowMillis = 20L)

        assertEquals(6.36, repository.snapshot.first()!!.priceFor(FuelKind.GASOLINE, "sp")!!.value, 0.0)
    }
}
