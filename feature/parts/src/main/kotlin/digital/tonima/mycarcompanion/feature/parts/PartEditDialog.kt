package digital.tonima.mycarcompanion.feature.parts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import digital.tonima.mycarcompanion.core.designsystem.model.PartUi
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.designsystem.util.formatToShortDate
import kotlin.math.roundToInt
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartEditDialog(
    part: PartUi? = null,
    unit: DistanceUnit,
    onDismiss: () -> Unit,
    onConfirm: (name: String, lifeSpan: Double, lastMaintenance: Double, lifeSpanMonths: Int?, lastMaintenanceDate: Instant?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(part?.name ?: "") }
    var lifeSpanStr by rememberSaveable { 
        mutableStateOf(part?.lifeSpanMileage?.let { unit.fromKm(it).roundToInt().toString() } ?: "") 
    }
    var lastMaintenanceStr by rememberSaveable { 
        mutableStateOf(part?.lastMaintenanceOdometer?.let { unit.fromKm(it).roundToInt().toString() } ?: "") 
    }
    var lifeSpanMonthsStr by rememberSaveable {
        mutableStateOf(part?.lifeSpanMonths?.toString() ?: "")
    }
    var lastMaintenanceDate by remember {
        mutableStateOf(part?.lastMaintenanceDate ?: Instant.fromEpochMilliseconds(System.currentTimeMillis()))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = lastMaintenanceDate.toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        lastMaintenanceDate = Instant.fromEpochMilliseconds(it)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (part == null) stringResource(R.string.add_part) else stringResource(R.string.edit_part)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (part == null) {
                    Text(text = "Sugestões:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                name = "Troca de Óleo"
                                lifeSpanStr = unit.fromKm(10000.0).roundToInt().toString()
                                lifeSpanMonthsStr = "6"
                            },
                            label = { Text("Óleo") }
                        )
                        AssistChip(
                            onClick = {
                                name = "Filtro de Ar"
                                lifeSpanStr = unit.fromKm(15000.0).roundToInt().toString()
                                lifeSpanMonthsStr = "12"
                            },
                            label = { Text("Filtro Ar") }
                        )
                        AssistChip(
                            onClick = {
                                name = "Pneus"
                                lifeSpanStr = unit.fromKm(40000.0).roundToInt().toString()
                                lifeSpanMonthsStr = "24"
                            },
                            label = { Text("Pneus") }
                        )
                    }
                }

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.part_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = lifeSpanStr,
                    onValueChange = { lifeSpanStr = it },
                    label = { Text(stringResource(R.string.part_lifespan_label, unit.name)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = lastMaintenanceStr,
                    onValueChange = { lastMaintenanceStr = it },
                    label = { Text(stringResource(R.string.part_last_maintenance_label, unit.name)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = lifeSpanMonthsStr,
                    onValueChange = { lifeSpanMonthsStr = it },
                    label = { Text("Validade em meses (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Data da última manutenção: ${lastMaintenanceDate.formatToShortDate()}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lifeSpanInUnit = lifeSpanStr.toDoubleOrNull() ?: 0.0
                    val lastMaintenanceInUnit = lastMaintenanceStr.toDoubleOrNull() ?: 0.0
                    val lifeSpanMonths = lifeSpanMonthsStr.toIntOrNull()
                    onConfirm(
                        name,
                        unit.toKm(lifeSpanInUnit),
                        unit.toKm(lastMaintenanceInUnit),
                        lifeSpanMonths,
                        lastMaintenanceDate
                    )
                },
                enabled = name.isNotBlank() && 
                        lifeSpanStr.toDoubleOrNull() != null && 
                        lastMaintenanceStr.toDoubleOrNull() != null
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
