package digital.tonima.mycarcompanion.core.data

enum class ChatRole { USER, MODEL }

data class ChatTurn(val role: ChatRole, val text: String)

interface CarAiRepository {

    /**
     * Sends [message] to the AI diagnostic assistant, given the running [history] of the
     * conversation and a [vehicleContext] summary (vehicle, parts, upcoming maintenance).
     */
    suspend fun diagnose(vehicleContext: String, history: List<ChatTurn>, message: String): Result<String>

    /**
     * Generates a short natural-language summary of upcoming maintenance and fuel trends
     * for [vehicleContext].
     */
    suspend fun generateMaintenanceInsight(vehicleContext: String): Result<String>
}
