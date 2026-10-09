package digital.tonima.mycarcompanion.core.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import digital.tonima.mycarcompanion.core.data.R

/** A default part that the AI can suggest a service interval for. [key] is the identifier used in the AI prompt. */
enum class ServicePart(val key: String, val nameResId: Int) {
    ENGINE_OIL("engine_oil", R.string.part_engine_oil),
    OIL_FILTER("oil_filter", R.string.part_oil_filter),
    AIR_FILTER("air_filter", R.string.part_air_filter),
    FUEL_FILTER("fuel_filter", R.string.part_fuel_filter),
    CABIN_FILTER("cabin_filter", R.string.part_cabin_filter),
    SPARK_PLUGS("spark_plugs", R.string.part_spark_plugs),
    TIMING_BELT("timing_belt", R.string.part_timing_belt),
    BRAKE_PADS("brake_pads", R.string.part_brake_pads),
    BRAKE_FLUID("brake_fluid", R.string.part_brake_fluid),
    COOLANT("coolant", R.string.part_coolant),
}

data class ServiceInterval(val part: ServicePart, val km: Double, val months: Int?)

/** Typical figures for a vehicle model. These are estimates and are meant to be reviewed by the user. */
data class VehicleSpecs(
    val tankLiters: Double?,
    val consumptionKmPerLiter: Double?,
    val intervals: List<ServiceInterval>,
)

interface VehicleSpecsRepository {
    /** Asks the AI for typical specs of the vehicle described by [description] (e.g. "Chevrolet Prisma LT 1.4 2019"). */
    suspend fun suggest(description: String): Result<VehicleSpecs>
}

object VehicleSpecsParser {

    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val adapter = Moshi.Builder().build().adapter<Map<String, Any?>>(mapType)

    private val TANK_RANGE = 20.0..120.0
    private val CONSUMPTION_RANGE = 3.0..30.0
    private val KM_RANGE = 1_000.0..200_000.0
    private val MONTHS_RANGE = 1..120

    /**
     * Parses the AI answer, tolerating Markdown code fences. Values outside plausible ranges are dropped
     * instead of trusted. Returns null when nothing usable is left.
     */
    fun parse(text: String): VehicleSpecs? = runCatching {
        val json = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val root = adapter.fromJson(json) ?: return null

        val intervals = (root["services"] as? List<*>).orEmpty().mapNotNull { item ->
            val entry = item as? Map<*, *> ?: return@mapNotNull null
            val part = ServicePart.entries.firstOrNull { it.key == entry["part"] } ?: return@mapNotNull null
            val km = (entry["km"] as? Number)?.toDouble()?.takeIf { it in KM_RANGE } ?: return@mapNotNull null
            val months = (entry["months"] as? Number)?.toInt()?.takeIf { it in MONTHS_RANGE }
            ServiceInterval(part, km, months)
        }.distinctBy { it.part }

        val specs = VehicleSpecs(
            tankLiters = (root["tankLiters"] as? Number)?.toDouble()?.takeIf { it in TANK_RANGE },
            consumptionKmPerLiter = (root["consumptionKmPerLiter"] as? Number)?.toDouble()?.takeIf { it in CONSUMPTION_RANGE },
            intervals = intervals,
        )
        specs.takeIf { it.tankLiters != null || it.consumptionKmPerLiter != null || it.intervals.isNotEmpty() }
    }.getOrNull()
}
