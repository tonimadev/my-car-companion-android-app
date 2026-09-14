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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mycarcompanion.core.designsystem.component.GarageBackground
import digital.tonima.mycarcompanion.core.designsystem.component.IsometricCard
import digital.tonima.mycarcompanion.core.designsystem.util.CurrencyUtils
import digital.tonima.mycarcompanion.core.designsystem.util.formatToShortDate
import kotlin.time.Instant

private val fuelTypeOptions = listOf("Gasolina", "Etanol", "Diesel", "GNV")

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

    LaunchedEffect(existingRecord) {
        existingRecord?.let { record ->
            litersText = record.liters.toString()
            totalCostText = record.totalCost.toString()
            mileageText = record.mileage.toInt().toString()
            fuelDate = record.date
            if (record.fuelType in fuelTypeOptions) {
                fuelType = record.fuelType
                isCustomFuelType = false
            } else {
                isCustomFuelType = true
                customFuelTypeText = record.fuelType
            }
            if (record.liters > 0) {
                pricePerLiterText = "%.2f".format(record.totalCost / record.liters).replace(",", ".")
            }
        }
    }

    LaunchedEffect(currentVehicle) {
        if (recordId == null && mileageText.isEmpty()) {
            mileageText = currentVehicle?.currentOdometer?.toInt()?.toString() ?: ""
        }
    }

    val calculateLiters = { total: String, price: String ->
        val totalVal = total.replace(",", ".").toDoubleOrNull() ?: 0.0
        val priceVal = price.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (totalVal > 0.0 && priceVal > 0.0) {
            val liters = totalVal / priceVal
            litersText = "%.2f".format(liters).replace(",", ".")
        }
    }

    val currencySymbol = remember { CurrencyUtils.getCurrencySymbol() }
    val effectiveFuelType = if (isCustomFuelType) customFuelTypeText else fuelType
    val canSave = !isSaving && litersText.isNotBlank() && totalCostText.isNotBlank() &&
        (litersText.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0.0 &&
        (totalCostText.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0.0 &&
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
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recordId == null) "Novo Abastecimento" else "Editar Abastecimento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
                        SectionLabel("Quilometragem")
                        OutlinedTextField(
                            value = mileageText,
                            onValueChange = { mileageText = it },
                            label = { Text("Quilometragem (km)") },
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
                        SectionLabel("Combustível")

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
                                    label = { Text("Outro") }
                                )
                            }
                        }

                        if (isCustomFuelType) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = customFuelTypeText,
                                onValueChange = { customFuelTypeText = it },
                                label = { Text("Qual combustível?") },
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
                            label = { Text("Valor Total ($currencySymbol)") },
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
                                label = { Text("Preço/Litro") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = litersText,
                                onValueChange = { litersText = it },
                                label = { Text("Litros") },
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
                            Text("Data: ${fuelDate.formatToShortDate()}")
                        }
                    }
                }

                Button(
                    onClick = {
                        val liters = litersText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val totalCost = totalCostText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val mileage = mileageText.toDoubleOrNull()
                        if (liters > 0.0 && totalCost > 0.0) {
                            viewModel.saveFuelRecord(
                                liters = liters,
                                totalCost = totalCost,
                                fuelType = effectiveFuelType.ifBlank { fuelTypeOptions.first() },
                                mileage = mileage,
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
                        Text(if (recordId == null) "Salvar Abastecimento" else "Atualizar Abastecimento")
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
