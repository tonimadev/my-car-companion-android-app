package digital.tonima.mycarcompanion.feature.home.assistant

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mycarcompanion.core.data.AiException
import digital.tonima.mycarcompanion.core.data.CarAiRepository
import digital.tonima.mycarcompanion.core.data.ChatRole
import digital.tonima.mycarcompanion.core.data.ChatTurn
import digital.tonima.mycarcompanion.core.data.FuelRepository
import digital.tonima.mycarcompanion.core.data.OdometerRepository
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.PredictionEngine
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleAiContext
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.feature.home.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    val isContextReady: Boolean = false,
    val error: String? = null
)

sealed interface DiagnosticChatIntent {
    data class SendMessage(val text: String) : DiagnosticChatIntent
    data object DismissError : DiagnosticChatIntent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiagnosticChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vehicleRepository: VehicleRepository,
    private val partRepository: PartRepository,
    private val odometerRepository: OdometerRepository,
    private val fuelRepository: FuelRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val proUserProvider: ProUserProvider,
    private val carAiRepository: CarAiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticChatUiState())
    val uiState: StateFlow<DiagnosticChatUiState> = _uiState.asStateFlow()

    @Volatile
    private var vehicleContext: String = ""
    private val history = mutableListOf<ChatTurn>()
    private var nextMessageId = 0L

    init {
        viewModelScope.launch {
            proUserProvider.isAiUser.collect { isAi -> _uiState.update { it.copy(isAiUser = isAi) } }
        }

        // The summary follows the current vehicle, its parts, fuel history and unit preferences,
        // so the model never answers with a stale or empty context.
        viewModelScope.launch {
            combine(
                vehicleRepository.getCurrentVehicle().flatMapLatest { vehicle ->
                    if (vehicle == null) {
                        flowOf(null)
                    } else {
                        combine(
                            partRepository.getPartsForVehicle(vehicle.id),
                            odometerRepository.getOdometerRecordsForVehicle(vehicle.id),
                            fuelRepository.getFuelRecordsForVehicle(vehicle.id),
                        ) { parts, odometerRecords, fuels -> VehicleData(vehicle, parts, odometerRecords, fuels) }
                    }
                },
                userPreferencesRepository.distanceUnit,
                userPreferencesRepository.consumptionUnit,
            ) { data, distanceUnit, consumptionUnit ->
                data?.let {
                    val consumptions = it.fuels.mapNotNull { record -> fuelRepository.calculateFuelConsumption(record) }
                    VehicleAiContext.build(
                        vehicle = it.vehicle,
                        parts = it.parts,
                        predictions = it.parts.associate { part ->
                            part.id to PredictionEngine.estimateNextMaintenanceDate(part, it.odometerRecords)
                                ?.toEpochMilliseconds()
                        },
                        distanceUnit = distanceUnit,
                        consumptionUnit = consumptionUnit,
                        averageFuelConsumption = consumptions.takeIf { c -> c.isNotEmpty() }?.average(),
                    )
                }
            }.collect { built ->
                vehicleContext = built.orEmpty()
                _uiState.update { it.copy(isContextReady = built != null) }
            }
        }
    }

    private data class VehicleData(
        val vehicle: digital.tonima.mycarcompanion.core.model.Vehicle,
        val parts: List<digital.tonima.mycarcompanion.core.model.Part>,
        val odometerRecords: List<digital.tonima.mycarcompanion.core.model.OdometerRecord>,
        val fuels: List<digital.tonima.mycarcompanion.core.model.FuelRecord>,
    )

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
        val state = _uiState.value
        if (text.isBlank() || state.isSending || !state.isContextReady) return

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
                .onFailure { e ->
                    val message = when ((e as? AiException)?.kind) {
                        AiException.Kind.TIMEOUT -> R.string.diagnostic_chat_error_timeout
                        AiException.Kind.NETWORK -> R.string.diagnostic_chat_error_network
                        AiException.Kind.EMPTY_RESPONSE -> R.string.diagnostic_chat_error_empty
                        else -> R.string.diagnostic_chat_error
                    }
                    _uiState.update { it.copy(isSending = false, error = context.getString(message)) }
                }
        }
    }
}
