package digital.tonima.mycarcompanion.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.data.OdometerRepository
import digital.tonima.mycarcompanion.core.data.PredictionEngine
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.OdometerRecord
import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val currentVehicle: Vehicle? = null,
    val parts: List<Part> = emptyList(),
    val predictions: Map<Long, kotlinx.datetime.Instant?> = emptyMap(),
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val isLoading: Boolean = false,
    val events: List<HomeUiEvent> = emptyList()
)

sealed interface HomeUiIntent {
    data class SelectVehicle(val vehicleId: Long) : HomeUiIntent
    data class PerformMaintenance(val part: Part, val newOdometer: Double) : HomeUiIntent
    data class UpdateOdometer(val newMileage: Double) : HomeUiIntent
    data object NavigateToSettings : HomeUiIntent
    data class ConsumeEvent(val eventId: String) : HomeUiIntent
}

sealed interface HomeUiEvent {
    val id: String
    data class ShowError(override val id: String, val message: String) : HomeUiEvent
    data class NavigateToSettings(override val id: String) : HomeUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vehicleRepository: VehicleRepository,
    private val partRepository: PartRepository,
    private val odometerRepository: OdometerRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _events = MutableStateFlow<List<HomeUiEvent>>(emptyList())

    init {
        viewModelScope.launch {
            combine(
                vehicleRepository.getVehicles(),
                vehicleRepository.getCurrentVehicle()
            ) { vehicles, current ->
                if (current == null && vehicles.isNotEmpty()) {
                    val defaultVehicle = vehicles.find { it.isCurrent } ?: vehicles.first()
                    vehicleRepository.setCurrentVehicle(defaultVehicle.id)
                }
            }.collect {}
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        vehicleRepository.getVehicles(),
        vehicleRepository.getCurrentVehicle().flatMapLatest { vehicle ->
            if (vehicle != null) {
                combine(
                    partRepository.getPartsForVehicle(vehicle.id),
                    odometerRepository.getOdometerRecordsForVehicle(vehicle.id)
                ) { parts, records -> parts to records }
            } else {
                flowOf(emptyList<Part>() to emptyList<OdometerRecord>())
            }
        },
        vehicleRepository.getCurrentVehicle(),
        userPreferencesRepository.distanceUnit,
        _events
    ) { vehicles, (parts, odometerRecords), currentVehicle, distanceUnit, events ->
        val sortedParts = parts.sortedBy { (it.lastMaintenanceOdometer + it.lifeSpanMileage) - (currentVehicle?.currentOdometer ?: 0.0) }
        val predictions = sortedParts.associate { part ->
            part.id to PredictionEngine.estimateNextMaintenanceDate(part, odometerRecords)
        }
        
        HomeUiState(
            vehicles = vehicles,
            currentVehicle = currentVehicle,
            parts = sortedParts,
            predictions = predictions,
            distanceUnit = distanceUnit,
            events = events,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun onIntent(intent: HomeUiIntent) {
        when (intent) {
            is HomeUiIntent.SelectVehicle -> selectVehicle(intent.vehicleId)
            is HomeUiIntent.PerformMaintenance -> performMaintenance(intent.part, intent.newOdometer)
            is HomeUiIntent.UpdateOdometer -> updateOdometer(intent.newMileage)
            HomeUiIntent.NavigateToSettings -> addEvent(HomeUiEvent.NavigateToSettings(UUID.randomUUID().toString()))
            is HomeUiIntent.ConsumeEvent -> consumeEvent(intent.eventId)
        }
    }

    private fun selectVehicle(vehicleId: Long) {
        viewModelScope.launch {
            vehicleRepository.setCurrentVehicle(vehicleId)
        }
    }

    private fun performMaintenance(part: Part, newOdometer: Double) {
        viewModelScope.launch {
            try {
                // Update vehicle odometer
                val currentVehicle = uiState.value.currentVehicle
                if (currentVehicle != null) {
                    val increment = newOdometer - currentVehicle.currentOdometer
                    if (increment >= 0) {
                        vehicleRepository.updateActiveVehicleOdometer(increment)
                        // Update part last maintenance odometer
                        partRepository.updatePart(part.copy(lastMaintenanceOdometer = newOdometer))
                    } else {
                        addEvent(HomeUiEvent.ShowError(UUID.randomUUID().toString(), context.getString(R.string.error_odometer_greater)))
                    }
                }
            } catch (e: Exception) {
                addEvent(HomeUiEvent.ShowError(UUID.randomUUID().toString(), e.message ?: context.getString(R.string.error_unknown)))
            }
        }
    }

    private fun updateOdometer(newMileage: Double) {
        viewModelScope.launch {
            try {
                val currentVehicle = uiState.value.currentVehicle
                if (currentVehicle != null) {
                    vehicleRepository.updateVehicle(currentVehicle.copy(currentOdometer = newMileage))
                }
            } catch (e: Exception) {
                addEvent(HomeUiEvent.ShowError(UUID.randomUUID().toString(), e.message ?: context.getString(R.string.error_unknown)))
            }
        }
    }

    private fun addEvent(event: HomeUiEvent) {
        _events.update { it + event }
    }

    private fun consumeEvent(eventId: String) {
        _events.update { it.filterNot { event -> event.id == eventId } }
    }
}
