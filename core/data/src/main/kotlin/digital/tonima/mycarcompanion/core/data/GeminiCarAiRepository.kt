package digital.tonima.mycarcompanion.core.data

import android.util.Log
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.content
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GeminiCarAiRepository"

@Singleton
class GeminiCarAiRepository @Inject constructor(
    private val generativeModel: GenerativeModel,
) : CarAiRepository {

    override suspend fun diagnose(
        vehicleContext: String,
        history: List<ChatTurn>,
        message: String,
    ): Result<String> = runCatching {
        val chatHistory = buildList {
            add(content("user") { text("Vehicle summary:\n$vehicleContext") })
            add(content("model") { text("Understood, I am ready to help with the diagnosis.") })
            history.forEach { turn ->
                val role = if (turn.role == ChatRole.USER) "user" else "model"
                add(content(role) { text(turn.text) })
            }
        }

        val chat = generativeModel.startChat(chatHistory)
        val response = chat.sendMessage(message)
        response.text ?: throw IllegalStateException("Empty AI response")
    }.onFailure { e ->
        Log.e(TAG, "diagnose() failed", e)
    }

    override suspend fun generateMaintenanceInsight(vehicleContext: String): Result<String> = runCatching {
        val prompt = """
            Based on the vehicle summary below, write a short insight (3 to 5 sentences) about
            maintenance and fuel consumption: highlight the most urgent maintenance, give a practical
            tip and comment on the consumption trend if there is enough data. Do not repeat the
            summary, only analyze it. Write it in the "User language" stated in the summary.

            $vehicleContext
        """.trimIndent()

        val response = generativeModel.generateContent(prompt)
        response.text ?: throw IllegalStateException("Empty AI response")
    }.onFailure { e ->
        Log.e(TAG, "generateMaintenanceInsight() failed", e)
    }
}
