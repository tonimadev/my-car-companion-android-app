package digital.tonima.mycarcompanion.feature.home.assistant

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mycarcompanion.core.data.CarAiRepository
import digital.tonima.mycarcompanion.core.data.ChatRole
import digital.tonima.mycarcompanion.core.data.ChatTurn
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleAiContext
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.feature.home.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ChatMessageUi(val id: Long, val role: ChatRole, val text: String)

@Immutable
data class DiagnosticChatUiState(
    val isAiUser: Boolean = false,
    val messages: ImmutableList<ChatMessageUi> = persistentListOf(),
    val isSending: Boolean = false,
    val error: String? = null
)

sealed interface DiagnosticChatIntent {
    data class SendMessage(val text: String) : DiagnosticChatIntent
    data object DismissError : DiagnosticChatIntent
}

@HiltViewModel
class DiagnosticChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vehicleRepository: VehicleRepository,
    private val partRepository: PartRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val proUserProvider: ProUserProvider,
    private val carAiRepository: CarAiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticChatUiState())
    val uiState: StateFlow<DiagnosticChatUiState> = _uiState.asStateFlow()

    private var vehicleContext: String = ""
    private val history = mutableListOf<ChatTurn>()
    private var nextMessageId = 0L

    init {
        viewModelScope.launch {
            combine(
                proUserProvider.isAiUser,
                userPreferencesRepository.distanceUnit,
                userPreferencesRepository.consumptionUnit
            ) { isAi, distanceUnit, consumptionUnit -> Triple(isAi, distanceUnit, consumptionUnit) }
                .collect { (isAi, distanceUnit, consumptionUnit) ->
                    _uiState.update { it.copy(isAiUser = isAi) }
                    if (isAi) {
                        val vehicle = vehicleRepository.getCurrentVehicle().first()
                        if (vehicle != null) {
                            val parts = partRepository.getPartsForVehicle(vehicle.id).first()
                            vehicleContext = VehicleAiContext.build(
                                vehicle = vehicle,
                                parts = parts,
                                predictions = emptyMap(),
                                distanceUnit = distanceUnit,
                                consumptionUnit = consumptionUnit
                            )
                        }
                    }
                }
        }
    }

    fun onIntent(intent: DiagnosticChatIntent) {
        when (intent) {
            is DiagnosticChatIntent.SendMessage -> sendMessage(intent.text)
            DiagnosticChatIntent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    fun subscribeAi(activity: Activity) {
        proUserProvider.launchSubscribeAi(activity)
    }

    private fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isSending) return

        val userMessage = ChatMessageUi(id = nextMessageId++, role = ChatRole.USER, text = text)
        _uiState.update {
            it.copy(messages = (it.messages + userMessage).toImmutableList(), isSending = true)
        }

        viewModelScope.launch {
            carAiRepository.diagnose(vehicleContext, history.toList(), text)
                .onSuccess { reply ->
                    history.add(ChatTurn(ChatRole.USER, text))
                    history.add(ChatTurn(ChatRole.MODEL, reply))
                    val aiMessage = ChatMessageUi(id = nextMessageId++, role = ChatRole.MODEL, text = reply)
                    _uiState.update {
                        it.copy(messages = (it.messages + aiMessage).toImmutableList(), isSending = false)
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            error = context.getString(R.string.diagnostic_chat_error)
                        )
                    }
                }
        }
    }
}
