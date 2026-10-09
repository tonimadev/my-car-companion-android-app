package digital.tonima.mycarcompanion.feature.vehicles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mycarcompanion.core.data.FipeItem
import digital.tonima.mycarcompanion.core.data.VehicleSpecs
import digital.tonima.mycarcompanion.core.designsystem.model.VehicleUi
import digital.tonima.mycarcompanion.core.designsystem.util.NumberUtils
import digital.tonima.mycarcompanion.core.model.ConsumptionUnit
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlin.math.roundToInt

@Composable
fun VehicleEditDialog(
    vehicle: VehicleUi? = null,
    unit: DistanceUnit,
    onDismiss: () -> Unit,
    onConfirm: (name: String, odometer: Double, tankCapacity: Double?, specs: VehicleSpecs?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(vehicle?.name ?: "") }
    val initialOdometer = vehicle?.currentOdometer?.let { unit.fromKm(it).roundToInt().toString() } ?: ""
    var odometerStr by rememberSaveable { mutableStateOf(initialOdometer) }
    var tankCapacityStr by rememberSaveable { mutableStateOf(vehicle?.tankCapacity?.let { NumberUtils.formatDecimalInput(it) } ?: "") }
    var appliedSpecs by remember { mutableStateOf<VehicleSpecs?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (vehicle == null) stringResource(R.string.add_vehicle) else stringResource(R.string.edit_vehicle)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (vehicle == null) {
                    VehicleLookupSection(
                        onDescriptionChosen = { name = it },
                        onSpecsSuggested = { specs ->
                            appliedSpecs = specs
                            specs.tankLiters?.let { tankCapacityStr = NumberUtils.formatDecimalInput(it) }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.vehicle_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = odometerStr,
                    onValueChange = { odometerStr = it },
                    label = { Text(stringResource(R.string.vehicle_odometer_label, unit.name)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = tankCapacityStr,
                    onValueChange = { tankCapacityStr = it },
                    label = { Text(stringResource(R.string.tank_capacity_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (vehicle == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.default_parts_added_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val odometerInUnit = NumberUtils.parseDecimal(odometerStr) ?: 0.0
                    val tankCap = NumberUtils.parseDecimal(tankCapacityStr)
                    onConfirm(name, unit.toKm(odometerInUnit), tankCap, appliedSpecs)
                },
                enabled = name.isNotBlank() && NumberUtils.parseDecimal(odometerStr) != null
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Brand, model and year pickers backed by the FIPE table. Choosing a year names the vehicle, and for
 * AI Assistant subscribers the specs button fills in tank size, consumption and service intervals.
 */
@Composable
private fun VehicleLookupSection(
    onDescriptionChosen: (String) -> Unit,
    onSpecsSuggested: (VehicleSpecs) -> Unit,
    viewModel: VehicleLookupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.reset() }
    LaunchedEffect(state.description) { state.description?.let(onDescriptionChosen) }
    LaunchedEffect(state.specs) { (state.specs as? SpecsState.Success)?.let { onSpecsSuggested(it.specs) } }

    Column {
        Text(stringResource(R.string.lookup_title), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        if (state.lookupFailed) {
            Text(
                text = stringResource(R.string.lookup_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            if (state.brands.isEmpty()) {
                TextButton(onClick = viewModel::loadBrands) { Text(stringResource(R.string.lookup_retry)) }
            }
            return@Column
        }

        SearchableDropdown(
            label = stringResource(R.string.lookup_brand_label),
            items = state.brands,
            selected = state.brand,
            enabled = !state.isLoadingBrands,
            onSelected = viewModel::selectBrand,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SearchableDropdown(
            label = stringResource(R.string.lookup_model_label),
            items = state.models,
            selected = state.model,
            enabled = state.brand != null && !state.isLoadingModels,
            onSelected = viewModel::selectModel,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SearchableDropdown(
            label = stringResource(R.string.lookup_year_label),
            items = state.years,
            selected = state.year,
            enabled = state.model != null && !state.isLoadingYears,
            onSelected = viewModel::selectYear,
        )

        if (state.description != null) {
            Spacer(modifier = Modifier.height(8.dp))
            SpecsSuggestion(state, onSuggest = viewModel::suggestSpecs)
        }
    }
}

@Composable
private fun SpecsSuggestion(state: VehicleLookupState, onSuggest: () -> Unit) {
    when {
        !state.isAiUser -> Text(
            text = stringResource(R.string.lookup_suggest_ai_only),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        state.specs is SpecsState.Loading -> Text(
            text = stringResource(R.string.lookup_suggest_loading),
            style = MaterialTheme.typography.bodySmall
        )
        else -> OutlinedButton(onClick = onSuggest) { Text(stringResource(R.string.lookup_suggest_button)) }
    }

    when (val specs = state.specs) {
        is SpecsState.Success -> {
            val consumption = specs.specs.consumptionKmPerLiter
            Text(
                text = if (consumption != null) {
                    stringResource(
                        R.string.lookup_suggest_result,
                        ConsumptionUnit.KM_L.format(consumption),
                        specs.specs.intervals.size
                    )
                } else {
                    stringResource(R.string.lookup_suggest_result_no_consumption, specs.specs.intervals.size)
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.lookup_suggest_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        SpecsState.Error -> Text(
            text = stringResource(R.string.lookup_suggest_error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        else -> Unit
    }
}

private const val MAX_VISIBLE_OPTIONS = 40

/**
 * True when every word of [query] appears in [name], in any order. FIPE names carry extra words
 * ("PRISMA Sed. LT 1.4 8V FlexPower 4p"), so a plain substring match would miss "prisma lt 1.4".
 */
internal fun matchesQuery(name: String, query: String): Boolean =
    query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.all { name.contains(it, ignoreCase = true) }

/** A dropdown whose text field filters the options, since FIPE lists hundreds of models per brand. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchableDropdown(
    label: String,
    items: List<FipeItem>,
    selected: FipeItem?,
    enabled: Boolean,
    onSelected: (FipeItem) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(selected) { mutableStateOf(selected?.name ?: "") }
    val options = remember(items, query, selected) {
        val typed = query.takeIf { selected == null || it != selected.name }.orEmpty()
        items.filter { matchesQuery(it.name, typed) }.take(MAX_VISIBLE_OPTIONS)
    }

    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { expanded = it }) {
        TextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled)
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.lookup_no_results)) },
                    onClick = { expanded = false },
                    enabled = false
                )
            }
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
