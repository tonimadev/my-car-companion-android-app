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
            add(content("user") { text("Contexto do veículo:\n$vehicleContext") })
            add(content("model") { text("Entendido, estou pronto para ajudar com o diagnóstico.") })
            history.forEach { turn ->
                val role = if (turn.role == ChatRole.USER) "user" else "model"
                add(content(role) { text(turn.text) })
            }
        }

        val chat = generativeModel.startChat(chatHistory)
        val response = chat.sendMessage(message)
        response.text ?: throw IllegalStateException("Resposta vazia da IA")
    }.onFailure { e ->
        Log.e(TAG, "diagnose() failed", e)
    }

    override suspend fun generateMaintenanceInsight(vehicleContext: String): Result<String> = runCatching {
        val prompt = """
            Com base no resumo do veículo abaixo, escreva um insight curto (3 a 5 frases) sobre
            manutenção e consumo: destaque a manutenção mais urgente, dê uma dica prática e comente
            a tendência de consumo se houver dados suficientes. Não repita o resumo, apenas analise-o.

            $vehicleContext
        """.trimIndent()

        val response = generativeModel.generateContent(prompt)
        response.text ?: throw IllegalStateException("Resposta vazia da IA")
    }.onFailure { e ->
        Log.e(TAG, "generateMaintenanceInsight() failed", e)
    }
}
