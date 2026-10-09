package digital.tonima.mycarcompanion.feature.vehicles

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mycarcompanion.core.data.FipeItem
import digital.tonima.mycarcompanion.core.data.FipeRepository
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.VehicleSpecs
import digital.tonima.mycarcompanion.core.data.VehicleSpecsRepository
import digital.tonima.mycarcompanion.core.data.modelYear
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
sealed interface SpecsState {
    data object Idle : SpecsState
    data object Loading : SpecsState
    data class Success(val specs: VehicleSpecs) : SpecsState
    data object Error : SpecsState
}

@Immutable
data class VehicleLookupState(
    val brands: ImmutableList<FipeItem> = persistentListOf(),
    val models: ImmutableList<FipeItem> = persistentListOf(),
    val years: ImmutableList<FipeItem> = persistentListOf(),
    val brand: FipeItem? = null,
    val model: FipeItem? = null,
    val year: FipeItem? = null,
    val isLoadingBrands: Boolean = true,
    val isLoadingModels: Boolean = false,
    val isLoadingYears: Boolean = false,
    val lookupFailed: Boolean = false,
    val isAiUser: Boolean = false,
    val specs: SpecsState = SpecsState.Idle,
) {
    /** "Chevrolet PRISMA Sed. LT 1.4 8V FlexPower 4p 2019", or null until brand, model and year are chosen. */
    val description: String?
        get() {
            val yearNumber = year?.modelYear()?.toString()
            if (brand == null || model == null || year == null) return null
            // FIPE brands can carry a group prefix ("GM - Chevrolet", "VW - VolksWagen"); the brand is what follows it.
            return listOfNotNull(brand.name.substringAfterLast(" - "), model.name, yearNumber).joinToString(" ")
        }
}

/**
 * Drives the "find your car" section of the add-vehicle dialog: FIPE brand/model/year selection and the
 * AI suggestion of tank size, typical consumption and service intervals for the chosen model.
 */
@HiltViewModel
class VehicleLookupViewModel @Inject constructor(
    private val fipeRepository: FipeRepository,
    private val vehicleSpecsRepository: VehicleSpecsRepository,
    proUserProvider: ProUserProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(VehicleLookupState())
    val state: StateFlow<VehicleLookupState> = _state.asStateFlow()

    private var modelsJob: Job? = null
    private var yearsJob: Job? = null

    init {
        viewModelScope.launch {
            proUserProvider.isAiUser.collect { isAi -> _state.update { it.copy(isAiUser = isAi) } }
        }
        loadBrands()
    }

    fun loadBrands() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingBrands = true, lookupFailed = false) }
            fipeRepository.brands()
                .onSuccess { brands ->
                    _state.update { it.copy(brands = brands.toImmutableList(), isLoadingBrands = false) }
                }
                .onFailure { _state.update { it.copy(isLoadingBrands = false, lookupFailed = true) } }
        }
    }

    /** Clears the previous selection (the dialog is reopened for each new vehicle) but keeps loaded brands. */
    fun reset() {
        modelsJob?.cancel()
        yearsJob?.cancel()
        _state.update {
            it.copy(
                brand = null, model = null, year = null,
                models = persistentListOf(), years = persistentListOf(),
                isLoadingModels = false, isLoadingYears = false, specs = SpecsState.Idle,
            )
        }
    }

    fun selectBrand(brand: FipeItem) {
        modelsJob?.cancel()
        yearsJob?.cancel()
        _state.update {
            it.copy(
                brand = brand, model = null, year = null,
                models = persistentListOf(), years = persistentListOf(),
                isLoadingModels = true, isLoadingYears = false, lookupFailed = false,
                specs = SpecsState.Idle,
            )
        }
        modelsJob = viewModelScope.launch {
            fipeRepository.models(brand.code)
                .onSuccess { models ->
                    _state.update { it.copy(models = models.toImmutableList(), isLoadingModels = false) }
                }
                .onFailure { _state.update { it.copy(isLoadingModels = false, lookupFailed = true) } }
        }
    }

    fun selectModel(model: FipeItem) {
        val brand = _state.value.brand ?: return
        yearsJob?.cancel()
        _state.update {
            it.copy(
                model = model, year = null, years = persistentListOf(),
                isLoadingYears = true, lookupFailed = false, specs = SpecsState.Idle,
            )
        }
        yearsJob = viewModelScope.launch {
            fipeRepository.years(brand.code, model.code)
                // "32000-1" is the zero-km entry, not a model year, so it cannot name a vehicle.
                .onSuccess { years ->
                    _state.update {
                        it.copy(years = years.filter { y -> y.modelYear() != null }.toImmutableList(), isLoadingYears = false)
                    }
                }
                .onFailure { _state.update { it.copy(isLoadingYears = false, lookupFailed = true) } }
        }
    }

    fun selectYear(year: FipeItem) {
        _state.update { it.copy(year = year, specs = SpecsState.Idle) }
    }

    fun suggestSpecs() {
        val current = _state.value
        val description = current.description ?: return
        if (!current.isAiUser || current.specs is SpecsState.Loading) return

        viewModelScope.launch {
            _state.update { it.copy(specs = SpecsState.Loading) }
            vehicleSpecsRepository.suggest(description)
                .onSuccess { specs -> _state.update { it.copy(specs = SpecsState.Success(specs)) } }
                .onFailure { _state.update { it.copy(specs = SpecsState.Error) } }
        }
    }
}
