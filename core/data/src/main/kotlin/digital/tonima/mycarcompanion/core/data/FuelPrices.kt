package digital.tonima.mycarcompanion.core.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/** The fuel price source only covers Brazil, so the feature is limited to that region. */
fun isFuelPriceRegion(locale: java.util.Locale = java.util.Locale.getDefault()): Boolean =
    locale.country.equals("BR", ignoreCase = true)

enum class FuelKind(internal val apiKey: String) {
    GASOLINE("gasolina"),
    DIESEL("diesel"),
}

/** Average price per liter for a [FuelKind], either for a [state] (UF) or the national average. */
data class StatePrice(val value: Double, val state: String, val isNationalAverage: Boolean)

/**
 * Fuel prices published by combustivelapi.com.br, in BRL per liter, keyed by lower-case UF.
 * The key "br" is the national average. Only gasoline and diesel are available, and not every state.
 */
data class FuelPriceSnapshot(
    val collectedAt: String,
    val prices: Map<FuelKind, Map<String, Double>>,
) {
    /** The price for [state], falling back to the national average when that state has no data. */
    fun priceFor(kind: FuelKind, state: String?): StatePrice? {
        val byState = prices[kind] ?: return null
        val key = state?.lowercase()
        if (key != null && key != NATIONAL) {
            byState[key]?.let { return StatePrice(it, key, isNationalAverage = false) }
        }
        return byState[NATIONAL]?.let { StatePrice(it, NATIONAL, isNationalAverage = true) }
    }

    companion object {
        const val NATIONAL = "br"
    }
}

object FuelPriceParser {

    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val adapter = Moshi.Builder().build().adapter<Map<String, Any?>>(mapType)

    /** Parses the API response, or returns null when it is malformed or reports an error. */
    fun parse(json: String): FuelPriceSnapshot? = runCatching {
        val root = adapter.fromJson(json) ?: return null
        if (root["error"] == true) return null
        val precos = root["precos"] as? Map<*, *> ?: return null

        val prices = FuelKind.entries.mapNotNull { kind ->
            val byState = precos[kind.apiKey] as? Map<*, *> ?: return@mapNotNull null
            val parsed = byState.entries.mapNotNull { (state, value) ->
                parsePrice(value as? String)?.let { (state as String).lowercase() to it }
            }.toMap()
            if (parsed.isEmpty()) null else kind to parsed
        }.toMap()

        if (prices.isEmpty()) null else FuelPriceSnapshot(root["data_coleta"] as? String ?: "", prices)
    }.getOrNull()

    /** "6,55" -> 6.55, rejecting values that are not plausible prices per liter. */
    private fun parsePrice(raw: String?): Double? =
        raw?.trim()?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it > 0.0 && it < 50.0 }
}
