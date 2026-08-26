package digital.tonima.mycarcompanion.feature.home.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    val distanceUnit: StateFlow<DistanceUnit> = userPreferencesRepository.distanceUnit
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DistanceUnit.KM
        )

    private val _isCompleting = MutableStateFlow(false)
    val isCompleting: StateFlow<Boolean> = _isCompleting

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch {
            userPreferencesRepository.setDistanceUnit(unit)
        }
    }

    fun completeOnboarding(
        vehicleName: String? = null,
        initialOdometer: Double? = null,
        onFinished: () -> Unit
    ) {
        viewModelScope.launch {
            _isCompleting.value = true

            // Se o usuário preencheu os dados do primeiro veículo, cadastra agora
            if (!vehicleName.isNullOrBlank()) {
                val odometerKm = initialOdometer ?: 0.0
                val unit = distanceUnit.value
                val odometerInKm = unit.toKm(odometerKm)

                val vehicle = Vehicle(
                    name = vehicleName.trim(),
                    currentOdometer = odometerInKm,
                    isCurrent = true
                )
                val newVehicleId = vehicleRepository.insertVehicle(vehicle)
                vehicleRepository.setCurrentVehicle(newVehicleId)
            }

            userPreferencesRepository.setOnboardingCompleted(true)
            _isCompleting.value = false
            onFinished()
        }
    }
}
