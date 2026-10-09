package digital.tonima.mycarcompanion.core.data

import android.util.Log
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException
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
    ): Result<String> = aiCall("diagnose()") {
        val chatHistory = buildList {
            add(content("user") { text("Vehicle summary:\n$vehicleContext") })
            add(content("model") { text("Understood, I am ready to help with the diagnosis.") })
            // The history must alternate user/model, so a trimmed window has to start with a user turn.
            history.takeLast(AiConfig.MAX_HISTORY_TURNS)
                .dropWhile { it.role != ChatRole.USER }
                .forEach { turn ->
                    val role = if (turn.role == ChatRole.USER) "user" else "model"
                    add(content(role) { text(turn.text) })
                }
        }

        val chat = generativeModel.startChat(chatHistory)
        chat.sendMessage(message.take(AiConfig.MAX_MESSAGE_LENGTH)).text
    }

    override suspend fun generateMaintenanceInsight(vehicleContext: String): Result<String> =
        aiCall("generateMaintenanceInsight()") {
            generativeModel.generateContent(AiConfig.insightPrompt(vehicleContext)).text
        }

    /**
     * Runs [block] with a timeout and classifies failures as [AiException]. Real coroutine
     * cancellation is rethrown instead of being swallowed into a [Result].
     */
    private suspend fun aiCall(name: String, block: suspend () -> String?): Result<String> = try {
        val text = withTimeout(AiConfig.REQUEST_TIMEOUT_MS) { block() }
        if (text.isNullOrBlank()) {
            Result.failure(AiException(AiException.Kind.EMPTY_RESPONSE))
        } else {
            Result.success(text)
        }
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "$name timed out", e)
        Result.failure(AiException(AiException.Kind.TIMEOUT, e))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "$name failed", e)
        val kind = if (e is IOException) AiException.Kind.NETWORK else AiException.Kind.UNKNOWN
        Result.failure(AiException(kind, e))
    }
}
