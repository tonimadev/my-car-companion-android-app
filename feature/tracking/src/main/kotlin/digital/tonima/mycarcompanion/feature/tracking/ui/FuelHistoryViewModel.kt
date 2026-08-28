package digital.tonima.mycarcompanion.feature.tracking.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mycarcompanion.core.data.FuelRepository
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.model.FuelRecord
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FuelRecordItemUiState(
    val record: FuelRecord,
    val consumptionKmPerL: Double? = null
)

data class FuelHistoryUiState(
    val currentVehicle: Vehicle? = null,
    val items: List<FuelRecordItemUiState> = emptyList(),
    val averageConsumption: Double? = null,
    val totalSpent: Double = 0.0,
    val isProUser: Boolean = false,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FuelHistoryViewModel @Inject constructor(
    private val fuelRepository: FuelRepository,
    private val vehicleRepository: VehicleRepository,
    private val proUserProvider: ProUserProvider
) : ViewModel() {

    val uiState: StateFlow<FuelHistoryUiState> = kotlinx.coroutines.flow.combine(
        vehicleRepository.getCurrentVehicle(),
        proUserProvider.isProUser
    ) { vehicle, isPro -> vehicle to isPro }
        .flatMapLatest { (vehicle, isPro) ->
            if (vehicle != null) {
                fuelRepository.getFuelRecordsForVehicle(vehicle.id).map { records ->
                    val uiItems = records.map { record ->
                        val consumption = fuelRepository.calculateFuelConsumption(record)
                        FuelRecordItemUiState(record = record, consumptionKmPerL = consumption)
                    }

                    val validConsumptions = uiItems.mapNotNull { it.consumptionKmPerL }
                    val avgConsumption = if (validConsumptions.isNotEmpty()) {
                        validConsumptions.average()
                    } else null

                    val totalSpent = records.sumOf { it.totalCost }

                    FuelHistoryUiState(
                        currentVehicle = vehicle,
                        items = uiItems,
                        averageConsumption = avgConsumption,
                        totalSpent = totalSpent,
                        isProUser = isPro,
                        isLoading = false
                    )
                }
            } else {
                flowOf(FuelHistoryUiState(isProUser = isPro, isLoading = false))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FuelHistoryUiState(isLoading = true)
        )

    fun deleteRecord(record: FuelRecord) {
        viewModelScope.launch {
            fuelRepository.deleteFuelRecord(record)
        }
    }
}