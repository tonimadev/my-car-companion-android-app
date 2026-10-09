package digital.tonima.mycarcompanion.core.data

import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.content
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiCarAiRepository @Inject constructor(
    private val generativeModel: GenerativeModel,
) : CarAiRepository, VehicleSpecsRepository {

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

    override suspend fun suggest(description: String): Result<VehicleSpecs> {
        val answer = aiCall("suggest()") {
            generativeModel.generateContent(AiConfig.vehicleSpecsPrompt(description.take(AiConfig.MAX_MESSAGE_LENGTH))).text
        }
        return answer.mapCatching { text ->
            VehicleSpecsParser.parse(text) ?: throw AiException(AiException.Kind.EMPTY_RESPONSE)
        }
    }
}
