package digital.tonima.mycarcompanion.feature.tracking.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mycarcompanion.core.data.FuelRepository
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.designsystem.model.FuelRecordUi
import digital.tonima.mycarcompanion.core.designsystem.model.VehicleUi
import digital.tonima.mycarcompanion.core.designsystem.model.toUi
import digital.tonima.mycarcompanion.core.model.FuelRecord
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class FuelRecordItemUiState(
    val record: FuelRecord,
    val consumptionKmPerL: Double? = null
)

@Immutable
data class FuelHistoryUiState(
    val currentVehicle: VehicleUi? = null,
    val items: ImmutableList<FuelRecordUi> = kotlinx.collections.immutable.persistentListOf(),
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
                        record.toUi(consumption)
                    }.toImmutableList()

                    val validConsumptions = uiItems.mapNotNull { it.consumptionKmPerL }
                    val avgConsumption = if (validConsumptions.isNotEmpty()) {
                        validConsumptions.average()
                    } else null

                    val totalSpent = records.sumOf { it.totalCost }

                    FuelHistoryUiState(
                        currentVehicle = vehicle.toUi(),
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

    fun deleteRecord(record: FuelRecordUi) {
        viewModelScope.launch {
            fuelRepository.deleteFuelRecord(
                FuelRecord(
                    id = record.id,
                    vehicleId = record.vehicleId,
                    date = record.date,
                    mileage = record.mileage,
                    liters = record.liters,
                    totalCost = record.totalCost,
                    fuelType = record.fuelType
                )
            )
        }
    }
}