package digital.tonima.mycarcompanion.core.data

enum class ChatRole { USER, MODEL }

data class ChatTurn(val role: ChatRole, val text: String)

/** Failure of an AI request, classified so the UI can show a meaningful message. */
class AiException(val kind: Kind, cause: Throwable? = null) : Exception(kind.name, cause) {
    enum class Kind { EMPTY_RESPONSE, TIMEOUT, NETWORK, UNKNOWN }
}

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
