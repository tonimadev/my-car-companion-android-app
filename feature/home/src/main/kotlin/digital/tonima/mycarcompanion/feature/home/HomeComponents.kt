package digital.tonima.mycarcompanion.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DiscFull
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import digital.tonima.mycarcompanion.core.designsystem.model.PartUi
import digital.tonima.mycarcompanion.core.designsystem.model.VehicleUi
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

@Composable
fun VehicleSelector(
    vehicles: ImmutableList<VehicleUi>,
    selectedVehicle: VehicleUi?,
    onVehicleSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (vehicles.isEmpty()) return

    val selectedIndex = vehicles.indexOfFirst { it.id == selectedVehicle?.id }.coerceAtLeast(0)
    val haptic = LocalHapticFeedback.current

    SecondaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp,
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(selectedTabIndex = selectedIndex)
            )
        },
        divider = {},
        tabs = {
            vehicles.forEachIndexed { index, vehicle ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onVehicleSelected(vehicle.id) 
                    },
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
    )
}

@Composable
fun OdometerDisplay(
    odometer: Double,
    unit: DistanceUnit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
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
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEditClick()
                },
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
    parts: ImmutableList<PartUi>,
    predictions: Map<Long, kotlinx.datetime.Instant?>,
    isAiUser: Boolean,
    currentOdometer: Double,
    unit: DistanceUnit,
    onPerformMaintenance: (PartUi) -> Unit,
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
            val dismissState = rememberSwipeToDismissBoxState()

            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
                    onPerformMaintenance(part)
                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                }
            }

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val backgroundColor = when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                        else -> Color.Transparent
                    }
                    Surface(
                        color = backgroundColor,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                },
                enableDismissFromEndToStart = false
            ) {
                MaintenanceItem(
                    part = part,
                    prediction = if (isAiUser) predictions[part.id] else null,
                    currentOdometer = currentOdometer,
                    unit = unit,
                    onPerformMaintenance = { onPerformMaintenance(part) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
fun MaintenanceItem(
    part: PartUi,
    prediction: kotlinx.datetime.Instant?,
    currentOdometer: Double,
    unit: DistanceUnit,
    onPerformMaintenance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val remaining = (part.lastMaintenanceOdometer + part.lifeSpanMileage) - currentOdometer

    val partIcon = when {
        part.name.contains("óleo", ignoreCase = true) || part.name.contains("oil", ignoreCase = true) -> Icons.Rounded.WaterDrop
        part.name.contains("pneu", ignoreCase = true) || part.name.contains("tire", ignoreCase = true) -> Icons.Rounded.Settings
        part.name.contains("freio", ignoreCase = true) || part.name.contains("brake", ignoreCase = true) -> Icons.Rounded.DiscFull
        part.name.contains("bateria", ignoreCase = true) || part.name.contains("battery", ignoreCase = true) -> Icons.Rounded.BatteryFull
        part.name.contains("filtro", ignoreCase = true) || part.name.contains("filter", ignoreCase = true) -> Icons.Rounded.FilterAlt
        else -> Icons.Rounded.Build
    }

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
                Icon(
                    imageVector = partIcon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 12.dp)
                )
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
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPerformMaintenance()
                }) {
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
    part: PartUi,
    currentOdometer: Double, // in KM
    unit: DistanceUnit,
    onConfirm: (odometer: Double, cost: Double, notes: String) -> Unit, // in KM
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
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
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
    val haptic = LocalHapticFeedback.current
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
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
