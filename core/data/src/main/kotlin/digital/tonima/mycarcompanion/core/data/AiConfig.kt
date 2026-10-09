package digital.tonima.mycarcompanion.core.data

object AiConfig {
    const val GEMINI_MODEL = "gemini-3.5-flash-lite"
    const val REQUEST_TIMEOUT_MS = 60_000L
    const val MAX_HISTORY_TURNS = 20
    const val MAX_MESSAGE_LENGTH = 2_000

    private val INSIGHT_INSTRUCTION = """
        Based on the vehicle summary below, write a short insight (3 to 5 sentences) about
        maintenance and fuel consumption: highlight the most urgent maintenance, give a practical
        tip and comment on the consumption trend if there is enough data. Do not repeat the
        summary, only analyze it. Write it in the "User language" stated in the summary.
    """.trimIndent()

    fun insightPrompt(vehicleContext: String): String = "$INSIGHT_INSTRUCTION\n\n$vehicleContext"

    private val VEHICLE_SPECS_INSTRUCTION = """
        Give typical specifications for the vehicle below, as sold in the Brazilian market.
        Reply with ONLY a JSON object: no Markdown, no code fence, no explanation, in exactly this shape:
        {"tankLiters": number or null,
         "consumptionKmPerLiter": number or null,
         "services": [{"part": "<key>", "km": number, "months": number or null}]}
        Rules:
        - tankLiters is the fuel tank capacity in liters.
        - consumptionKmPerLiter is the typical mixed city/highway fuel economy in km/L (use gasoline for flex cars).
        - services lists the manufacturer's recommended replacement interval for each part you know,
          where "part" is one of: engine_oil, oil_filter, air_filter, fuel_filter, cabin_filter,
          spark_plugs, timing_belt, brake_pads, brake_fluid, coolant. "km" is the distance interval and
          "months" the time interval when the manufacturer defines one, otherwise null.
        - Use null (or omit the service) when you are not reasonably sure. Never guess wildly.
    """.trimIndent()

    fun vehicleSpecsPrompt(description: String): String = "$VEHICLE_SPECS_INSTRUCTION\n\nVehicle: $description"

    val SYSTEM_INSTRUCTION = """
        You are an automotive diagnostic and maintenance assistant built into the My Car Companion app.
        You receive a summary of the user's vehicle (details, parts and maintenance/fuel history)
        and must answer in a direct, objective and friendly way.

        Language and units:
        - Always reply in the user's language: the language of their latest message, or, when that
          is unclear, the "User language" stated in the vehicle summary.
        - Express distances and fuel economy in the "User units" stated in the vehicle summary.

        When diagnosing a symptom reported by the user:
        - List 2 to 4 possible causes, ordered from most to least likely.
        - Give an urgency level (low, medium, high) for each cause when it makes sense.
        - Suggest a practical next step (e.g. check something specific, or see a mechanic).
        - Always make clear that this is a preliminary suggestion based on the information provided
          and does NOT replace the evaluation of a qualified mechanic, especially for safety
          symptoms (brakes, steering, suspension).
        - Only answer about the vehicle, maintenance, fuel and related automotive topics.
          If asked about anything else, kindly say you can only help with the car.
        - Be brief: a few short paragraphs or a list, no filler.
    """.trimIndent()
}
