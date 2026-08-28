package digital.tonima.mycarcompanion.feature.vehicles

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.designsystem.model.VehicleUi
import digital.tonima.mycarcompanion.core.designsystem.model.toUiModels
import digital.tonima.mycarcompanion.core.model.DistanceUnit
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
import javax.inject.Inject

data class GarageState(
    val vehicles: ImmutableList<VehicleUi> = persistentListOf(),
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val isProUser: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val effect: GarageUiEffect? = null
)

sealed interface GarageUiEffect {
    data class NavigateToParts(val vehicleId: Long) : GarageUiEffect
    data class ShowError(val message: String) : GarageUiEffect
}

sealed interface GarageIntent {
    data class AddVehicle(val name: String, val currentOdometer: Double) : GarageIntent
    data class UpdateVehicle(val vehicle: VehicleUi) : GarageIntent
    data class DeleteVehicle(val vehicle: VehicleUi) : GarageIntent
    data class SetCurrentVehicle(val id: Long) : GarageIntent
    data object ConsumeEffect : GarageIntent
}

@HiltViewModel
class GarageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vehicleRepository: VehicleRepository,
    userPreferencesRepository: UserPreferencesRepository,
    proUserProvider: ProUserProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(GarageState(isLoading = true))
    val state: StateFlow<GarageState> = _uiState.asStateFlow()
    val effect = state.map { it.effect }

    init {
        viewModelScope.launch {
            combine(
                vehicleRepository.getVehicles(),
                userPreferencesRepository.distanceUnit,
                proUserProvider.isProUser
            ) { vehicles, unit, isPro ->
                _uiState.update {
                    it.copy(
                        vehicles = vehicles.toUiModels(),
                        distanceUnit = unit,
                        isProUser = isPro,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    fun handleIntent(intent: GarageIntent) {
        when (intent) {
            is GarageIntent.AddVehicle -> addVehicle(intent.name, intent.currentOdometer)
            is GarageIntent.UpdateVehicle -> updateVehicle(intent.vehicle)
            is GarageIntent.DeleteVehicle -> deleteVehicle(intent.vehicle)
            is GarageIntent.SetCurrentVehicle -> setCurrentVehicle(intent.id)
            GarageIntent.ConsumeEffect -> consumeEffect()
        }
    }

    private fun addVehicle(name: String, odometer: Double) {
        viewModelScope.launch {
            try {
                vehicleRepository.insertVehicle(Vehicle(name = name, currentOdometer = odometer))
            } catch (e: Exception) {
                triggerEffect(GarageUiEffect.ShowError(e.message ?: context.getString(R.string.error_add_vehicle)))
            }
        }
    }

    private fun updateVehicle(vehicle: VehicleUi) {
        viewModelScope.launch {
            try {
                vehicleRepository.updateVehicle(
                    Vehicle(
                        id = vehicle.id,
                        name = vehicle.name,
                        currentOdometer = vehicle.currentOdometer,
                        isCurrent = vehicle.isCurrent
                    )
                )
            } catch (e: Exception) {
                triggerEffect(GarageUiEffect.ShowError(e.message ?: context.getString(R.string.error_update_vehicle)))
            }
        }
    }

    private fun deleteVehicle(vehicle: VehicleUi) {
        viewModelScope.launch {
            try {
                vehicleRepository.deleteVehicle(
                    Vehicle(
                        id = vehicle.id,
                        name = vehicle.name,
                        currentOdometer = vehicle.currentOdometer,
                        isCurrent = vehicle.isCurrent
                    )
                )
            } catch (e: Exception) {
                triggerEffect(GarageUiEffect.ShowError(e.message ?: context.getString(R.string.error_delete_vehicle)))
            }
        }
    }

    private fun setCurrentVehicle(id: Long) {
        viewModelScope.launch {
            try {
                vehicleRepository.setCurrentVehicle(id)
            } catch (e: Exception) {
                triggerEffect(GarageUiEffect.ShowError(e.message ?: context.getString(R.string.error_set_current_vehicle)))
            }
        }
    }

    fun onNavigateToParts(vehicleId: Long) {
        triggerEffect(GarageUiEffect.NavigateToParts(vehicleId))
    }

    private fun triggerEffect(effect: GarageUiEffect) {
        _uiState.update { it.copy(effect = effect) }
    }

    private fun consumeEffect() {
        _uiState.update { it.copy(effect = null) }
    }
}
