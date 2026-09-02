package digital.tonima.mycarcompanion.feature.tracking.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mycarcompanion.core.designsystem.util.CurrencyUtils
import digital.tonima.mycarcompanion.core.designsystem.util.formatToShortDate
import kotlin.time.Instant

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
    var fuelType by remember { mutableStateOf("Gasolina") }
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
            fuelType = record.fuelType
            fuelDate = record.date
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
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            currentVehicle?.let { vehicle ->
                Text(
                    text = "Veículo: ${vehicle.name}",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            OutlinedTextField(
                value = mileageText,
                onValueChange = { mileageText = it },
                label = { Text("Quilometragem (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

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

            OutlinedTextField(
                value = pricePerLiterText,
                onValueChange = {
                    pricePerLiterText = it
                    calculateLiters(totalCostText, it)
                },
                label = { Text("Preço por Litro ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = litersText,
                onValueChange = { litersText = it },
                label = { Text("Litros abastecidos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = fuelType,
                onValueChange = { fuelType = it },
                label = { Text("Tipo de Combustível") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Data: ${fuelDate.formatToShortDate()}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val liters = litersText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val totalCost = totalCostText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val mileage = mileageText.toDoubleOrNull()
                    if (liters > 0.0 && totalCost > 0.0) {
                        viewModel.saveFuelRecord(
                            liters = liters,
                            totalCost = totalCost,
                            fuelType = fuelType,
                            mileage = mileage,
                            date = fuelDate,
                            id = recordId ?: 0L
                        )
                        onNavigateUp()
                    }
                },
                enabled = !isSaving && litersText.isNotBlank() && totalCostText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp))
                } else {
                    Text(if (recordId == null) "Salvar Abastecimento" else "Atualizar Abastecimento")
                }
            }
        }
    }
}
