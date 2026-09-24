package digital.tonima.mycarcompanion.feature.tracking.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mycarcompanion.core.designsystem.component.GarageBackground
import digital.tonima.mycarcompanion.core.designsystem.component.IsometricCard
import digital.tonima.mycarcompanion.core.designsystem.util.CurrencyUtils
import digital.tonima.mycarcompanion.core.designsystem.util.NumberUtils
import digital.tonima.mycarcompanion.core.designsystem.util.formatToShortDate
import digital.tonima.mycarcompanion.feature.tracking.R
import kotlin.math.roundToLong
import kotlin.time.Instant
import digital.tonima.mycarcompanion.core.data.R as DataR
import digital.tonima.mycarcompanion.core.designsystem.R as DesignR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFuelRecordScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    recordId: Long? = null,
    viewModel: FuelTrackingViewModel = hiltViewModel()
) {
    val currentVehicle by viewModel.currentVehicle.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val existingRecord by viewModel.existingRecord.collectAsStateWithLifecycle()
    val distanceUnit by viewModel.distanceUnit.collectAsStateWithLifecycle()

    val fuelTypeOptions = listOf(
        stringResource(DataR.string.fuel_type_gasoline),
        stringResource(DataR.string.fuel_type_ethanol),
        stringResource(DataR.string.fuel_type_diesel),
        stringResource(DataR.string.fuel_type_cng)
    )

    var litersText by remember { mutableStateOf("") }
    var totalCostText by remember { mutableStateOf("") }
    var pricePerLiterText by remember { mutableStateOf("") }
    var mileageText by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf(fuelTypeOptions.first()) }
    var isCustomFuelType by remember { mutableStateOf(false) }
    var customFuelTypeText by remember { mutableStateOf("") }
    var fuelDate by remember {
        mutableStateOf(Instant.fromEpochMilliseconds(System.currentTimeMillis()))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(recordId) {
        if (recordId != null) {
            viewModel.loadRecord(recordId)
        }
    }

    LaunchedEffect(existingRecord, distanceUnit) {
        existingRecord?.let { record ->
            litersText = NumberUtils.formatDecimalInput(record.liters)
            totalCostText = NumberUtils.formatDecimalInput(record.totalCost)
            mileageText = distanceUnit.fromKm(record.mileage).roundToLong().toString()
            fuelDate = record.date
            if (record.fuelType in fuelTypeOptions) {
                fuelType = record.fuelType
                isCustomFuelType = false
            } else {
                isCustomFuelType = true
                customFuelTypeText = record.fuelType
            }
            if (record.liters > 0) {
                pricePerLiterText = NumberUtils.formatDecimalInput(record.totalCost / record.liters)
            }
        }
    }

    // Prefill with the vehicle odometer in the user's unit, unless the user already typed something.
    var prefilledMileageText by remember { mutableStateOf("") }
    LaunchedEffect(currentVehicle, distanceUnit) {
        if (recordId == null && mileageText == prefilledMileageText) {
            prefilledMileageText = currentVehicle?.let { distanceUnit.fromKm(it.currentOdometer).roundToLong().toString() } ?: ""
            mileageText = prefilledMileageText
        }
    }

    val calculateLiters = { total: String, price: String ->
        val totalVal = NumberUtils.parseDecimal(total) ?: 0.0
        val priceVal = NumberUtils.parseDecimal(price) ?: 0.0
        if (totalVal > 0.0 && priceVal > 0.0) {
            litersText = NumberUtils.formatDecimalInput(totalVal / priceVal)
        }
    }

    val currencySymbol = remember { CurrencyUtils.getCurrencySymbol() }
    val effectiveFuelType = if (isCustomFuelType) customFuelTypeText else fuelType
    val canSave = !isSaving && litersText.isNotBlank() && totalCostText.isNotBlank() &&
        (NumberUtils.parseDecimal(litersText) ?: 0.0) > 0.0 &&
        (NumberUtils.parseDecimal(totalCostText) ?: 0.0) > 0.0 &&
        effectiveFuelType.isNotBlank()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fuelDate.toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        fuelDate = Instant.fromEpochMilliseconds(it)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(DesignR.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(DesignR.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (recordId == null) R.string.fuel_new_title else R.string.fuel_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(DesignR.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        GarageBackground(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                currentVehicle?.let { vehicle ->
                    IsometricCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        depthColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = vehicle.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                IsometricCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    depthColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                ) {
                    Column {
                        SectionLabel(stringResource(R.string.fuel_section_mileage))
                        OutlinedTextField(
                            value = mileageText,
                            onValueChange = { mileageText = it },
                            label = { Text(stringResource(R.string.fuel_mileage_label, distanceUnit.symbol)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                IsometricCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    depthColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Column {
                        SectionLabel(stringResource(R.string.fuel_section_fuel))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(fuelTypeOptions) { option ->
                                FilterChip(
                                    selected = !isCustomFuelType && fuelType == option,
                                    onClick = {
                                        isCustomFuelType = false
                                        fuelType = option
                                    },
                                    label = { Text(option) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = isCustomFuelType,
                                    onClick = { isCustomFuelType = true },
                                    label = { Text(stringResource(R.string.fuel_type_other)) }
                                )
                            }
                        }

                        if (isCustomFuelType) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = customFuelTypeText,
                                onValueChange = { customFuelTypeText = it },
                                label = { Text(stringResource(R.string.fuel_type_custom_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = totalCostText,
                            onValueChange = {
                                totalCostText = it
                                calculateLiters(it, pricePerLiterText)
                            },
                            label = { Text(stringResource(R.string.fuel_total_cost_label, currencySymbol)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pricePerLiterText,
                                onValueChange = {
                                    pricePerLiterText = it
                                    calculateLiters(totalCostText, it)
                                },
                                label = { Text(stringResource(R.string.fuel_price_per_liter_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = litersText,
                                onValueChange = { litersText = it },
                                label = { Text(stringResource(DesignR.string.unit_liters)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(DesignR.string.label_date, fuelDate.formatToShortDate()))
                        }
                    }
                }

                Button(
                    onClick = {
                        val liters = NumberUtils.parseDecimal(litersText) ?: 0.0
                        val totalCost = NumberUtils.parseDecimal(totalCostText) ?: 0.0
                        val mileageKm = NumberUtils.parseDecimal(mileageText)?.let { distanceUnit.toKm(it) }
                        if (liters > 0.0 && totalCost > 0.0) {
                            viewModel.saveFuelRecord(
                                liters = liters,
                                totalCost = totalCost,
                                fuelType = effectiveFuelType.ifBlank { fuelTypeOptions.first() },
                                mileageKm = mileageKm,
                                date = fuelDate,
                                id = recordId ?: 0L
                            )
                            onNavigateUp()
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    } else {
                        Icon(Icons.Rounded.LocalGasStation, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(if (recordId == null) R.string.fuel_save else R.string.fuel_update))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}
