package digital.tonima.mycarcompanion.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mycarcompanion.core.data.MaintenanceRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.designsystem.model.MaintenanceRecordUi
import digital.tonima.mycarcompanion.core.designsystem.model.toMaintenanceUiModels
import digital.tonima.mycarcompanion.core.model.MaintenanceRecord
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaintenanceHistoryState(
    val items: ImmutableList<MaintenanceRecordUi> = persistentListOf(),
    val isLoading: Boolean = true
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class MaintenanceHistoryViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MaintenanceHistoryState())
    val state: StateFlow<MaintenanceHistoryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            vehicleRepository.getCurrentVehicle().flatMapLatest { vehicle ->
                if (vehicle != null) {
                    maintenanceRepository.getMaintenanceRecordsWithPartForVehicle(vehicle.id)
                } else {
                    flowOf(emptyList())
                }
            }.map { it.toMaintenanceUiModels() }
                .collectLatest { items ->
                    _state.update { it.copy(items = items, isLoading = false) }
                }
        }
    }

    fun updateRecord(record: MaintenanceRecordUi) {
        viewModelScope.launch {
            maintenanceRepository.updateMaintenanceRecord(
                MaintenanceRecord(
                    id = record.id,
                    partId = record.partId,
                    date = record.date,
                    odometerAtMaintenance = record.odometerAtMaintenance,
                    cost = record.cost,
                    notes = record.notes
                )
            )
        }
    }

    fun deleteRecord(record: MaintenanceRecordUi) {
        viewModelScope.launch {
            maintenanceRepository.deleteMaintenanceRecord(
                MaintenanceRecord(
                    id = record.id,
                    partId = record.partId,
                    date = record.date,
                    odometerAtMaintenance = record.odometerAtMaintenance,
                    cost = record.cost,
                    notes = record.notes
                )
            )
        }
    }
}
