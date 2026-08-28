package digital.tonima.mycarcompanion.feature.parts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import digital.tonima.mycarcompanion.core.designsystem.model.PartUi
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlin.math.roundToInt
import kotlin.time.Instant

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (part == null) stringResource(R.string.add_part) else stringResource(R.string.edit_part)) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.part_name_label)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = lifeSpanStr,
                    onValueChange = { lifeSpanStr = it },
                    label = { Text(stringResource(R.string.part_lifespan_label, unit.name)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = lastMaintenanceStr,
                    onValueChange = { lastMaintenanceStr = it },
                    label = { Text(stringResource(R.string.part_last_maintenance_label, unit.name)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = lifeSpanMonthsStr,
                    onValueChange = { lifeSpanMonthsStr = it },
                    label = { Text("Validade em meses (ex: 6 ou 12)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lifeSpanInUnit = lifeSpanStr.toDoubleOrNull() ?: 0.0
                    val lastMaintenanceInUnit = lastMaintenanceStr.toDoubleOrNull() ?: 0.0
                    val lifeSpanMonths = lifeSpanMonthsStr.toIntOrNull()
                    val lastDate = part?.lastMaintenanceDate ?: Instant.fromEpochMilliseconds(System.currentTimeMillis())
                    onConfirm(
                        name,
                        unit.toKm(lifeSpanInUnit),
                        unit.toKm(lastMaintenanceInUnit),
                        lifeSpanMonths,
                        if (lifeSpanMonths != null) lastDate else null
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
