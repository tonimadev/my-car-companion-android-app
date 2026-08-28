package digital.tonima.mycarcompanion.feature.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GarageState(
    val vehicles: List<Vehicle> = emptyList(),
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val isProUser: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed interface GarageIntent {
    data class AddVehicle(val name: String, val currentOdometer: Double) : GarageIntent
    data class UpdateVehicle(val vehicle: Vehicle) : GarageIntent
    data class DeleteVehicle(val vehicle: Vehicle) : GarageIntent
    data class SetCurrentVehicle(val id: Long) : GarageIntent
}

sealed interface GarageEffect {
    data class NavigateToParts(val vehicleId: Long) : GarageEffect
    data class ShowError(val message: String) : GarageEffect
}

@HiltViewModel
class GarageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vehicleRepository: VehicleRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val proUserProvider: ProUserProvider
) : ViewModel() {

    private val _effects = Channel<GarageEffect>()
    val effects = _effects.receiveAsFlow()

    val state: StateFlow<GarageState> = combine(
        vehicleRepository.getVehicles(),
        userPreferencesRepository.distanceUnit,
        proUserProvider.isProUser
    ) { vehicles, unit, isPro ->
        GarageState(
            vehicles = vehicles,
            distanceUnit = unit,
            isProUser = isPro,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GarageState(isLoading = true)
    )

    fun handleIntent(intent: GarageIntent) {
        when (intent) {
            is GarageIntent.AddVehicle -> addVehicle(intent.name, intent.currentOdometer)
            is GarageIntent.UpdateVehicle -> updateVehicle(intent.vehicle)
            is GarageIntent.DeleteVehicle -> deleteVehicle(intent.vehicle)
            is GarageIntent.SetCurrentVehicle -> setCurrentVehicle(intent.id)
        }
    }

    private fun addVehicle(name: String, odometer: Double) {
        viewModelScope.launch {
            try {
                vehicleRepository.insertVehicle(Vehicle(name = name, currentOdometer = odometer))
            } catch (e: Exception) {
                _effects.send(GarageEffect.ShowError(e.message ?: context.getString(R.string.error_add_vehicle)))
            }
        }
    }

    private fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            try {
                vehicleRepository.updateVehicle(vehicle)
            } catch (e: Exception) {
                _effects.send(GarageEffect.ShowError(e.message ?: context.getString(R.string.error_update_vehicle)))
            }
        }
    }

    private fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            try {
                vehicleRepository.deleteVehicle(vehicle)
            } catch (e: Exception) {
                _effects.send(GarageEffect.ShowError(e.message ?: context.getString(R.string.error_delete_vehicle)))
            }
        }
    }

    private fun setCurrentVehicle(id: Long) {
        viewModelScope.launch {
            try {
                vehicleRepository.setCurrentVehicle(id)
            } catch (e: Exception) {
                _effects.send(GarageEffect.ShowError(e.message ?: context.getString(R.string.error_set_current_vehicle)))
            }
        }
    }

    fun onNavigateToParts(vehicleId: Long) {
        viewModelScope.launch {
            _effects.send(GarageEffect.NavigateToParts(vehicleId))
        }
    }
}
