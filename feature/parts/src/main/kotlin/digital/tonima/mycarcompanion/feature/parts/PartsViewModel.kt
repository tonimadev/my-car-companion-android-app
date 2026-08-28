package digital.tonima.mycarcompanion.feature.parts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.designsystem.model.PartUi
import digital.tonima.mycarcompanion.core.designsystem.model.VehicleUi
import digital.tonima.mycarcompanion.core.designsystem.model.toPartUiModels
import digital.tonima.mycarcompanion.core.designsystem.model.toUi
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

data class PartsState(
    val vehicle: VehicleUi? = null,
    val parts: ImmutableList<PartUi> = persistentListOf(),
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val isProUser: Boolean = false,
    val isLoading: Boolean = true,
    val effect: PartsUiEffect? = null
)

sealed interface PartsUiEffect {
    data class ShowError(val message: String) : PartsUiEffect
}

sealed interface PartsIntent {
    data class AddPart(
        val name: String,
        val lifeSpan: Double,
        val lastMaintenance: Double,
        val lifeSpanMonths: Int? = null,
        val lastMaintenanceDate: Instant? = null
    ) : PartsIntent
    data class UpdatePart(val part: PartUi) : PartsIntent
    data class DeletePart(val part: PartUi) : PartsIntent
    data object ConsumeEffect : PartsIntent
}

@HiltViewModel(assistedFactory = PartsViewModel.Factory::class)
class PartsViewModel @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    private val partRepository: PartRepository,
    private val vehicleRepository: VehicleRepository,
    userPreferencesRepository: UserPreferencesRepository,
    proUserProvider: ProUserProvider,
    @Assisted private val vehicleId: Long
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(vehicleId: Long): PartsViewModel
    }

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    private val _uiState = MutableStateFlow(PartsState(isLoading = true))
    val state: StateFlow<PartsState> = _uiState.asStateFlow()
    val effect = state.map { it.effect }

    init {
        loadVehicle()

        viewModelScope.launch {
            combine(
                _vehicle,
                partRepository.getPartsForVehicle(vehicleId),
                userPreferencesRepository.distanceUnit,
                proUserProvider.isProUser
            ) { vehicle, parts, unit, isPro ->
                _uiState.update {
                    it.copy(
                        vehicle = vehicle?.toUi(),
                        parts = parts.toPartUiModels(),
                        distanceUnit = unit,
                        isProUser = isPro,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    private fun loadVehicle() {
        viewModelScope.launch {
            _vehicle.value = vehicleRepository.getVehicle(vehicleId)
        }
    }

    fun handleIntent(intent: PartsIntent) {
        when (intent) {
            is PartsIntent.AddPart -> addPart(
                intent.name,
                intent.lifeSpan,
                intent.lastMaintenance,
                intent.lifeSpanMonths,
                intent.lastMaintenanceDate
            )
            is PartsIntent.UpdatePart -> updatePart(intent.part)
            is PartsIntent.DeletePart -> deletePart(intent.part)
            PartsIntent.ConsumeEffect -> consumeEffect()
        }
    }

    private fun addPart(
        name: String,
        lifeSpan: Double,
        lastMaintenance: Double,
        lifeSpanMonths: Int?,
        lastMaintenanceDate: Instant?
    ) {
        viewModelScope.launch {
            try {
                partRepository.insertPart(
                    Part(
                        vehicleId = vehicleId,
                        name = name,
                        lifeSpanMileage = lifeSpan,
                        lastMaintenanceOdometer = lastMaintenance,
                        lifeSpanMonths = lifeSpanMonths,
                        lastMaintenanceDate = lastMaintenanceDate
                    )
                )
            } catch (e: Exception) {
                triggerEffect(PartsUiEffect.ShowError(e.message ?: context.getString(R.string.error_add_part)))
            }
        }
    }

    private fun updatePart(part: PartUi) {
        viewModelScope.launch {
            try {
                partRepository.updatePart(
                    Part(
                        id = part.id,
                        vehicleId = part.vehicleId,
                        name = part.name,
                        lifeSpanMileage = part.lifeSpanMileage,
                        lastMaintenanceOdometer = part.lastMaintenanceOdometer,
                        lifeSpanMonths = part.lifeSpanMonths,
                        lastMaintenanceDate = part.lastMaintenanceDate
                    )
                )
            } catch (e: Exception) {
                triggerEffect(PartsUiEffect.ShowError(e.message ?: context.getString(R.string.error_update_part)))
            }
        }
    }

    private fun deletePart(part: PartUi) {
        viewModelScope.launch {
            try {
                partRepository.deletePart(
                    Part(
                        id = part.id,
                        vehicleId = part.vehicleId,
                        name = part.name,
                        lifeSpanMileage = part.lifeSpanMileage,
                        lastMaintenanceOdometer = part.lastMaintenanceOdometer,
                        lifeSpanMonths = part.lifeSpanMonths,
                        lastMaintenanceDate = part.lastMaintenanceDate
                    )
                )
            } catch (e: Exception) {
                triggerEffect(PartsUiEffect.ShowError(e.message ?: context.getString(R.string.error_delete_part)))
            }
        }
    }

    private fun triggerEffect(effect: PartsUiEffect) {
        _uiState.update { it.copy(effect = effect) }
    }

    private fun consumeEffect() {
        _uiState.update { it.copy(effect = null) }
    }
}
