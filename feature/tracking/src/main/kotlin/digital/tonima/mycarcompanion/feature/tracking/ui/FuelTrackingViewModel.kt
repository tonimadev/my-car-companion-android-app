package digital.tonima.mycarcompanion.feature.tracking.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mycarcompanion.core.data.FuelRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.model.FuelRecord
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Instant
import javax.inject.Inject

@HiltViewModel
class FuelTrackingViewModel @Inject constructor(
    private val fuelRepository: FuelRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    val currentVehicle: StateFlow<Vehicle?> = vehicleRepository.getCurrentVehicle()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _existingRecord = MutableStateFlow<FuelRecord?>(null)
    val existingRecord: StateFlow<FuelRecord?> = _existingRecord

    fun loadRecord(recordId: Long) {
        viewModelScope.launch {
            fuelRepository.getFuelRecord(recordId).collect { record ->
                _existingRecord.value = record
            }
        }
    }

    fun saveFuelRecord(
        liters: Double,
        totalCost: Double,
        fuelType: String,
        mileage: Double? = null,
        date: Instant? = null,
        id: Long = 0
    ) {
        val vehicle = currentVehicle.value ?: return
        
        viewModelScope.launch {
            _isSaving.value = true
            
            val record = FuelRecord(
                id = id,
                vehicleId = vehicle.id,
                date = date ?: Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                mileage = mileage ?: vehicle.currentOdometer,
                liters = liters,
                totalCost = totalCost,
                fuelType = fuelType
            )
            
            if (id == 0L) {
                fuelRepository.insertFuelRecord(record)
                // If it's a new record and mileage is greater than current, update vehicle
                if (mileage != null && mileage > vehicle.currentOdometer) {
                    vehicleRepository.updateVehicle(vehicle.copy(currentOdometer = mileage))
                }
            } else {
                fuelRepository.updateFuelRecord(record)
            }
            _isSaving.value = false
        }
    }
}