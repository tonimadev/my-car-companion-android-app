package digital.tonima.mycarcompanion.feature.vehicles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import digital.tonima.mycarcompanion.core.designsystem.model.VehicleUi
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlin.math.roundToInt

@Composable
fun VehicleEditDialog(
    vehicle: VehicleUi? = null,
    unit: DistanceUnit,
    onDismiss: () -> Unit,
    onConfirm: (name: String, odometer: Double, tankCapacity: Double?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(vehicle?.name ?: "") }
    val initialOdometer = vehicle?.currentOdometer?.let { unit.fromKm(it).roundToInt().toString() } ?: ""
    var odometerStr by rememberSaveable { mutableStateOf(initialOdometer) }
    var tankCapacityStr by rememberSaveable { mutableStateOf(vehicle?.tankCapacity?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (vehicle == null) stringResource(R.string.add_vehicle) else stringResource(R.string.edit_vehicle)) },
        text = {
            Column {
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
                    label = { Text("Capacidade do Tanque (Litros)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (vehicle == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.default_parts_added_note),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val odometerInUnit = odometerStr.toDoubleOrNull() ?: 0.0
                    val tankCap = tankCapacityStr.replace(",", ".").toDoubleOrNull()
                    onConfirm(name, unit.toKm(odometerInUnit), tankCap)
                },
                enabled = name.isNotBlank() && odometerStr.toDoubleOrNull() != null
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
