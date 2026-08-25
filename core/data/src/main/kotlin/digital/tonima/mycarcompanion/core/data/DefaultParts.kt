package digital.tonima.mycarcompanion.core.data

data class DefaultPart(
    val nameResId: Int,
    val lifeSpanKm: Double
)

val DEFAULT_PARTS = listOf(
    DefaultPart(R.string.part_engine_oil, 10000.0),
    DefaultPart(R.string.part_oil_filter, 10000.0),
    DefaultPart(R.string.part_air_filter, 20000.0),
    DefaultPart(R.string.part_fuel_filter, 20000.0),
    DefaultPart(R.string.part_cabin_filter, 15000.0),
    DefaultPart(R.string.part_spark_plugs, 50000.0),
    DefaultPart(R.string.part_timing_belt, 60000.0),
    DefaultPart(R.string.part_brake_pads, 30000.0),
    DefaultPart(R.string.part_brake_fluid, 40000.0),
    DefaultPart(R.string.part_coolant, 50000.0)
)
