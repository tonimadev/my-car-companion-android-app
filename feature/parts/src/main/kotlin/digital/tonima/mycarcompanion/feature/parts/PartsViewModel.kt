package digital.tonima.mycarcompanion.feature.parts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PartsState(
    val vehicle: Vehicle? = null,
    val parts: List<Part> = emptyList(),
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val isLoading: Boolean = true
)

sealed interface PartsIntent {
    data class AddPart(val name: String, val lifeSpan: Double, val lastMaintenance: Double) : PartsIntent
    data class UpdatePart(val part: Part) : PartsIntent
    data class DeletePart(val part: Part) : PartsIntent
}

sealed interface PartsEffect {
    data class ShowError(val message: String) : PartsEffect
}

@HiltViewModel(assistedFactory = PartsViewModel.Factory::class)
class PartsViewModel @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    private val partRepository: PartRepository,
    private val vehicleRepository: VehicleRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @Assisted private val vehicleId: Long
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(vehicleId: Long): PartsViewModel
    }

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    private val _effects = Channel<PartsEffect>()
    val effects = _effects.receiveAsFlow()

    val state: StateFlow<PartsState> = combine(
        _vehicle,
        partRepository.getPartsForVehicle(vehicleId),
        userPreferencesRepository.distanceUnit
    ) { vehicle, parts, unit ->
        PartsState(
            vehicle = vehicle,
            parts = parts,
            distanceUnit = unit,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PartsState(isLoading = true)
    )

    init {
        loadVehicle()
    }

    private fun loadVehicle() {
        viewModelScope.launch {
            _vehicle.value = vehicleRepository.getVehicle(vehicleId)
        }
    }

    fun handleIntent(intent: PartsIntent) {
        when (intent) {
            is PartsIntent.AddPart -> addPart(intent.name, intent.lifeSpan, intent.lastMaintenance)
            is PartsIntent.UpdatePart -> updatePart(intent.part)
            is PartsIntent.DeletePart -> deletePart(intent.part)
        }
    }

    private fun addPart(name: String, lifeSpan: Double, lastMaintenance: Double) {
        viewModelScope.launch {
            try {
                partRepository.insertPart(
                    Part(
                        vehicleId = vehicleId,
                        name = name,
                        lifeSpanMileage = lifeSpan,
                        lastMaintenanceOdometer = lastMaintenance
                    )
                )
            } catch (e: Exception) {
                _effects.send(PartsEffect.ShowError(e.message ?: context.getString(R.string.error_add_part)))
            }
        }
    }

    private fun updatePart(part: Part) {
        viewModelScope.launch {
            try {
                partRepository.updatePart(part)
            } catch (e: Exception) {
                _effects.send(PartsEffect.ShowError(e.message ?: context.getString(R.string.error_update_part)))
            }
        }
    }

    private fun deletePart(part: Part) {
        viewModelScope.launch {
            try {
                partRepository.deletePart(part)
            } catch (e: Exception) {
                _effects.send(PartsEffect.ShowError(e.message ?: context.getString(R.string.error_delete_part)))
            }
        }
    }
}
