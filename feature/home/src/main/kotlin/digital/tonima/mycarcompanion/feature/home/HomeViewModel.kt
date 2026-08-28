package digital.tonima.mycarcompanion.feature.home

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mycarcompanion.core.data.FuelRepository
import digital.tonima.mycarcompanion.core.data.MaintenanceRepository
import digital.tonima.mycarcompanion.core.data.OdometerRepository
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.PredictionEngine
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.designsystem.model.PartUi
import digital.tonima.mycarcompanion.core.designsystem.model.VehicleUi
import digital.tonima.mycarcompanion.core.designsystem.model.toPartUiModels
import digital.tonima.mycarcompanion.core.designsystem.model.toUi
import digital.tonima.mycarcompanion.core.designsystem.model.toUiModels
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.MaintenanceRecord
import digital.tonima.mycarcompanion.core.model.OdometerRecord
import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Instant

@Immutable
data class HomeUiState(
    val vehicles: ImmutableList<VehicleUi> = persistentListOf(),
    val currentVehicle: VehicleUi? = null,
    val parts: ImmutableList<PartUi> = persistentListOf(),
    val predictions: Map<Long, Instant?> = emptyMap(),
    val totalMaintenanceCost: Double = 0.0,
    val totalFuelCost: Double = 0.0,
    val averageFuelConsumption: Double? = null,
    val fuelConsumptionTrend: FuelTrend = FuelTrend.STABLE,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val isProUser: Boolean = false,
    val isAiUser: Boolean = false,
    val isLoading: Boolean = false,
    val effect: HomeUiEffect? = null
)

enum class FuelTrend {
    IMPROVING, WORSENING, STABLE
}

sealed interface HomeUiIntent {
    data class SelectVehicle(val vehicleId: Long) : HomeUiIntent
    data class PerformMaintenance(
        val part: PartUi,
        val newOdometer: Double,
        val cost: Double = 0.0,
        val notes: String = ""
    ) : HomeUiIntent
    data class UpdateOdometer(val newMileage: Double) : HomeUiIntent
    data object NavigateToSettings : HomeUiIntent
    data object NavigateToFuel : HomeUiIntent
    data object ConsumeEffect : HomeUiIntent
}

sealed interface HomeUiEffect {
    data class ShowError(val message: String) : HomeUiEffect
    data object NavigateToSettings : HomeUiEffect
    data object NavigateToFuel : HomeUiEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vehicleRepository: VehicleRepository,
    private val partRepository: PartRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val odometerRepository: OdometerRepository,
    private val fuelRepository: FuelRepository,
    userPreferencesRepository: UserPreferencesRepository,
    proUserProvider: ProUserProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))

    init {
        viewModelScope.launch {
            combine(
                vehicleRepository.getVehicles(),
                vehicleRepository.getCurrentVehicle()
            ) { vehicles, current ->
                if (current == null && vehicles.isNotEmpty()) {
                    val defaultVehicle = vehicles.find { it.isCurrent } ?: vehicles.first()
                    vehicleRepository.setCurrentVehicle(defaultVehicle.id)
                }
            }.collect {}
        }

        viewModelScope.launch {
            combine(
                combine(
                    vehicleRepository.getVehicles(),
                    vehicleRepository.getCurrentVehicle()
                ) { vehicles, current -> vehicles to current },
                vehicleRepository.getCurrentVehicle().flatMapLatest { vehicle ->
                    if (vehicle != null) {
                        combine(
                            partRepository.getPartsForVehicle(vehicle.id),
                            odometerRepository.getOdometerRecordsForVehicle(vehicle.id),
                            fuelRepository.getFuelRecordsForVehicle(vehicle.id),
                            maintenanceRepository.getTotalMaintenanceCostForVehicle(vehicle.id),
                            fuelRepository.getTotalFuelCostForVehicle(vehicle.id),
                            transform = { parts, records, fuels, maintCost, fuelCost ->
                                UiData(parts, records, fuels, maintCost ?: 0.0, fuelCost ?: 0.0)
                            }
                        )
                    } else {
                        flowOf(UiData())
                    }
                },
                userPreferencesRepository.distanceUnit,
                combine(proUserProvider.isProUser, proUserProvider.isAiUser) { pro, ai -> pro to ai }
            ) { (vehicles, currentVehicle), uiData, distanceUnit, (isPro, isAi) ->
                val sortedParts = uiData.parts.sortedBy { (it.lastMaintenanceOdometer + it.lifeSpanMileage) - (currentVehicle?.currentOdometer ?: 0.0) }
                val predictions = sortedParts.associate { part ->
                    part.id to PredictionEngine.estimateNextMaintenanceDate(part, uiData.records)
                }

                // Calculate average consumption
                val consumptions = uiData.fuels.mapNotNull { fuelRepository.calculateFuelConsumption(it) }
                val avgConsumption = if (consumptions.isNotEmpty()) consumptions.average() else null

                val trend = if (consumptions.size >= 2) {
                    val last = consumptions.first()
                    val previous = consumptions.drop(1).first()
                    when {
                        last > previous + 0.1 -> FuelTrend.IMPROVING
                        last < previous - 0.1 -> FuelTrend.WORSENING
                        else -> FuelTrend.STABLE
                    }
                } else FuelTrend.STABLE

                _uiState.update {
                    it.copy(
                        vehicles = vehicles.toUiModels(),
                        currentVehicle = currentVehicle?.toUi(),
                        parts = sortedParts.toPartUiModels(),
                        predictions = predictions,
                        totalMaintenanceCost = uiData.totalMaintCost,
                        totalFuelCost = uiData.totalFuelCost,
                        averageFuelConsumption = avgConsumption,
                        fuelConsumptionTrend = trend,
                        distanceUnit = distanceUnit,
                        isProUser = isPro,
                        isAiUser = isAi,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }

    private data class UiData(
        val parts: List<Part> = emptyList(),
        val records: List<OdometerRecord> = emptyList(),
        val fuels: List<digital.tonima.mycarcompanion.core.model.FuelRecord> = emptyList(),
        val totalMaintCost: Double = 0.0,
        val totalFuelCost: Double = 0.0
    )

    fun onIntent(intent: HomeUiIntent) {
        when (intent) {
            is HomeUiIntent.SelectVehicle -> selectVehicle(intent.vehicleId)
            is HomeUiIntent.PerformMaintenance -> performMaintenance(intent.part, intent.newOdometer, intent.cost, intent.notes)
            is HomeUiIntent.UpdateOdometer -> updateOdometer(intent.newMileage)
            HomeUiIntent.NavigateToSettings -> triggerEffect(HomeUiEffect.NavigateToSettings)
            HomeUiIntent.NavigateToFuel -> triggerEffect(HomeUiEffect.NavigateToFuel)
            HomeUiIntent.ConsumeEffect -> consumeEffect()
        }
    }

    private fun selectVehicle(vehicleId: Long) {
        viewModelScope.launch {
            vehicleRepository.setCurrentVehicle(vehicleId)
        }
    }

    private fun performMaintenance(part: PartUi, newOdometer: Double, cost: Double, notes: String) {
        viewModelScope.launch {
            try {
                val currentVehicle = uiState.value.currentVehicle
                if (currentVehicle != null) {
                    val increment = newOdometer - currentVehicle.currentOdometer
                    if (increment >= 0) {
                        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                        vehicleRepository.updateActiveVehicleOdometer(increment)
                        partRepository.updatePart(
                            Part(
                                id = part.id,
                                vehicleId = part.vehicleId,
                                name = part.name,
                                lifeSpanMileage = part.lifeSpanMileage,
                                lastMaintenanceOdometer = newOdometer,
                                lifeSpanMonths = part.lifeSpanMonths,
                                lastMaintenanceDate = now
                            )
                        )
                        maintenanceRepository.insertMaintenanceRecord(
                            MaintenanceRecord(
                                partId = part.id,
                                date = now,
                                odometerAtMaintenance = newOdometer,
                                cost = cost,
                                notes = notes
                            )
                        )
                    } else {
                        triggerEffect(HomeUiEffect.ShowError(context.getString(R.string.error_odometer_greater)))
                    }
                }
            } catch (e: Exception) {
                triggerEffect(HomeUiEffect.ShowError(e.message ?: context.getString(R.string.error_unknown)))
            }
        }
    }

    private fun updateOdometer(newMileage: Double) {
        viewModelScope.launch {
            try {
                val currentVehicle = uiState.value.currentVehicle
                if (currentVehicle != null) {
                    vehicleRepository.updateVehicle(
                        Vehicle(
                            id = currentVehicle.id,
                            name = currentVehicle.name,
                            currentOdometer = newMileage,
                            isCurrent = currentVehicle.isCurrent
                        )
                    )
                }
            } catch (e: Exception) {
                triggerEffect(HomeUiEffect.ShowError(e.message ?: context.getString(R.string.error_unknown)))
            }
        }
    }

    private fun triggerEffect(effect: HomeUiEffect) {
        _uiState.update { it.copy(effect = effect) }
    }

    private fun consumeEffect() {
        _uiState.update { it.copy(effect = null) }
    }
}
