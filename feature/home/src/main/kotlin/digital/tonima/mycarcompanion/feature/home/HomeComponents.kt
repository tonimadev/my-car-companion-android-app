package digital.tonima.mycarcompanion.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlin.math.roundToInt

@Composable
fun VehicleSelector(
    vehicles: List<Vehicle>,
    selectedVehicle: Vehicle?,
    onVehicleSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (vehicles.isEmpty()) return

    val selectedIndex = vehicles.indexOfFirst { it.id == selectedVehicle?.id }.coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
        modifier = modifier
    ) {
        vehicles.forEachIndexed { index, vehicle ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onVehicleSelected(vehicle.id) },
                text = {
                    Text(
                        text = vehicle.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                icon = {
                    Icon(Icons.Rounded.DirectionsCar, contentDescription = null)
                }
            )
        }
    }
}

@Composable
fun OdometerDisplay(
    odometer: Double,
    unit: DistanceUnit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.current_odometer),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = unit.fromKm(odometer).roundToInt().toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = unit.name.lowercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.edit_odometer)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MaintenanceList(
    parts: List<Part>,
    predictions: Map<Long, kotlinx.datetime.Instant?>,
    currentOdometer: Double,
    unit: DistanceUnit,
    onPerformMaintenance: (Part) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.upcoming_maintenance),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(parts, key = { it.id }) { part ->
            MaintenanceItem(
                part = part,
                prediction = predictions[part.id],
                currentOdometer = currentOdometer,
                unit = unit,
                onPerformMaintenance = { onPerformMaintenance(part) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
fun MaintenanceItem(
    part: Part,
    prediction: kotlinx.datetime.Instant?,
    currentOdometer: Double,
    unit: DistanceUnit,
    onPerformMaintenance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remaining = (part.lastMaintenanceOdometer + part.lifeSpanMileage) - currentOdometer
    val targetProgress = (remaining / part.lifeSpanMileage).coerceIn(0.0, 1.0).toFloat()
    
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress_animation"
    )

    val color = when {
        remaining < 500 -> MaterialTheme.colorScheme.error
        remaining < 2000 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val remainingInUnit = unit.fromKm(remaining)
                    Text(
                        text = if (remaining > 0) 
                            stringResource(R.string.due_in, remainingInUnit.roundToInt(), unit.name.lowercase())
                            else stringResource(R.string.overdue_by, (-remainingInUnit).roundToInt(), unit.name.lowercase()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color
                    )
                    if (prediction != null) {
                        Text(
                            text = stringResource(R.string.predicted_date, prediction.formatToShortDate()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                IconButton(onClick = onPerformMaintenance) {
                    Icon(
                        Icons.Rounded.Build,
                        contentDescription = stringResource(R.string.mark_as_done),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 1f - animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun MaintenanceDialog(
    part: Part,
    currentOdometer: Double, // in KM
    unit: DistanceUnit,
    onConfirm: (odometer: Double, cost: Double, notes: String) -> Unit, // in KM
    onDismiss: () -> Unit
) {
    val currentOdometerInUnit = unit.fromKm(currentOdometer)
    var odometerText by rememberSaveable { mutableStateOf(currentOdometerInUnit.roundToInt().toString()) }
    var costText by rememberSaveable { mutableStateOf("") }
    var notesText by rememberSaveable { mutableStateOf("") }
    val isError = odometerText.toDoubleOrNull()?.let { it < currentOdometerInUnit } ?: true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.maintenance_title, part.name)) },
        text = {
            Column {
                Text(text = stringResource(R.string.maintenance_instruction))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = it },
                    label = { Text(stringResource(R.string.current_odometer_label, unit.name)) },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(stringResource(R.string.odometer_error_min, currentOdometerInUnit.roundToInt()))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Custo total (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Observações / Mecânica (opcional)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    odometerText.toDoubleOrNull()?.let { odo ->
                        val cost = costText.toDoubleOrNull() ?: 0.0
                        onConfirm(unit.toKm(odo), cost, notesText) 
                    } 
                },
                enabled = !isError
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

@Composable
fun UpdateOdometerDialog(
    currentOdometer: Double, // in KM
    unit: DistanceUnit,
    onConfirm: (Double) -> Unit, // in KM
    onDismiss: () -> Unit
) {
    val currentOdometerInUnit = unit.fromKm(currentOdometer)
    var odometerText by rememberSaveable { mutableStateOf(currentOdometerInUnit.roundToInt().toString()) }
    val isValid = odometerText.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.update_odometer)) },
        text = {
            Column {
                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = it },
                    label = { Text(stringResource(R.string.current_odometer_label, unit.name)) },
                    isError = !isValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    odometerText.toDoubleOrNull()?.let {
                        onConfirm(unit.toKm(it))
                    }
                },
                enabled = isValid
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
