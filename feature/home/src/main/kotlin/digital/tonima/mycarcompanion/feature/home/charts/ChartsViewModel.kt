package digital.tonima.mycarcompanion.feature.home.charts

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mycarcompanion.core.data.FuelRepository
import digital.tonima.mycarcompanion.core.data.MaintenanceRepository
import digital.tonima.mycarcompanion.core.data.OdometerRepository
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.PredictionEngine
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.model.ConsumptionUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Clock

@Immutable
data class ChartsUiState(
    val isLoading: Boolean = true,
    val hasVehicle: Boolean = false,
    val upcoming: ImmutableList<UpcomingMaintenance> = persistentListOf(),
    val monthlyCosts: ImmutableList<MonthlyCost> = persistentListOf(),
    val consumption: ImmutableList<ConsumptionPoint> = persistentListOf(),
    val consumptionUnit: ConsumptionUnit = ConsumptionUnit.KM_L,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChartsViewModel @Inject constructor(
    vehicleRepository: VehicleRepository,
    partRepository: PartRepository,
    odometerRepository: OdometerRepository,
    fuelRepository: FuelRepository,
    maintenanceRepository: MaintenanceRepository,
    userPreferencesRepository: UserPreferencesRepository,
    proUserProvider: ProUserProvider,
) : ViewModel() {

    val uiState: StateFlow<ChartsUiState> = combine(
        vehicleRepository.getCurrentVehicle().flatMapLatest { vehicle ->
            if (vehicle == null) {
                flowOf(null)
            } else {
                combine(
                    partRepository.getPartsForVehicle(vehicle.id),
                    odometerRepository.getOdometerRecordsForVehicle(vehicle.id),
                    fuelRepository.getFuelRecordsForVehicle(vehicle.id),
                    maintenanceRepository.getMaintenanceRecordsForVehicle(vehicle.id),
                ) { parts, odometer, fuels, maintenance -> VehicleData(parts, odometer, fuels, maintenance) }
            }
        },
        userPreferencesRepository.consumptionUnit,
        proUserProvider.isAiUser,
    ) { data, consumptionUnit, isAi ->
        if (data == null) {
            ChartsUiState(isLoading = false, consumptionUnit = consumptionUnit)
        } else {
            val now = Clock.System.now()
            val predictions = data.parts.associate { part ->
                part.id to PredictionEngine.estimateNextMaintenanceDate(part, data.odometer, includeMileageProjection = isAi)
                    ?.toEpochMilliseconds()
            }
            ChartsUiState(
                isLoading = false,
                hasVehicle = true,
                upcoming = upcomingMaintenance(data.parts, predictions, now).toImmutableList(),
                monthlyCosts = monthlyCosts(data.fuels, data.maintenance, now).toImmutableList(),
                consumption = consumptionSeries(data.fuels).toImmutableList(),
                consumptionUnit = consumptionUnit,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChartsUiState())

    private data class VehicleData(
        val parts: List<digital.tonima.mycarcompanion.core.model.Part>,
        val odometer: List<digital.tonima.mycarcompanion.core.model.OdometerRecord>,
        val fuels: List<digital.tonima.mycarcompanion.core.model.FuelRecord>,
        val maintenance: List<digital.tonima.mycarcompanion.core.model.MaintenanceRecord>,
    )
}
