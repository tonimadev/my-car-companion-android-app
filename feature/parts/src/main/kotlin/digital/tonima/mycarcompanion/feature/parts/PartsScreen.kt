package digital.tonima.mycarcompanion.feature.parts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mycarcompanion.core.designsystem.component.AdBannerView
import digital.tonima.mycarcompanion.core.designsystem.component.ConfirmDeleteDialog
import digital.tonima.mycarcompanion.core.designsystem.component.IsometricCarView
import digital.tonima.mycarcompanion.core.designsystem.component.IsometricCard
import digital.tonima.mycarcompanion.core.designsystem.component.IsometricProgressBar
import digital.tonima.mycarcompanion.core.designsystem.model.MaintenanceStatus
import digital.tonima.mycarcompanion.core.designsystem.model.PartUi
import digital.tonima.mycarcompanion.core.designsystem.util.isometricDepth
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlin.math.roundToInt

@Composable
fun PartsScreen(
    onBack: () -> Unit,
    adUnitId: String,
    viewModel: PartsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle(initialValue = null)

    PartsContent(
        state = state,
        effect = effect,
        onIntent = viewModel::handleIntent,
        onBack = onBack,
        adUnitId = adUnitId
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartsContent(
    state: PartsState,
    effect: PartsUiEffect?,
    onIntent: (PartsIntent) -> Unit,
    onBack: () -> Unit,
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editingPartId by rememberSaveable { mutableStateOf<Long?>(null) }
    var partPendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    val editingPart = remember(editingPartId, state.parts) {
        state.parts.find { it.id == editingPartId }
    }
    val partPendingDelete = remember(partPendingDeleteId, state.parts) {
        state.parts.find { it.id == partPendingDeleteId }
    }

    LaunchedEffect(effect) {
        if (effect != null) {
            when (effect) {
                is PartsUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
            onIntent(PartsIntent.ConsumeEffect)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.vehicle?.name?.let { stringResource(R.string.parts_title_format, it) } ?: stringResource(R.string.parts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        IsometricCarView(
                            parts = state.parts,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    items(state.parts, key = { it.id }) { part ->
                        PartItem(
                            part = part,
                            unit = state.distanceUnit,
                            currentOdometer = state.vehicle?.currentOdometer ?: part.lastMaintenanceOdometer,
                            onEdit = {
                                editingPartId = part.id
                                showDialog = true
                            },
                            onDelete = { partPendingDeleteId = part.id }
                        )
                    }

                    item {
                        AdBannerView(
                            isProUser = state.isProUser,
                            adId = adUnitId,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    editingPartId = null
                    showDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .isometricDepth(depth = 4.dp, cornerRadius = 16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_part))
            }

            if (showDialog) {
                PartEditDialog(
                    part = editingPart,
                    unit = state.distanceUnit,
                    onDismiss = { showDialog = false },
                    onConfirm = { name, lifeSpan, lastMaintenance, lifeSpanMonths, lastMaintenanceDate ->
                        if (editingPart == null) {
                            onIntent(PartsIntent.AddPart(name, lifeSpan, lastMaintenance, lifeSpanMonths, lastMaintenanceDate))
                        } else {
                            onIntent(
                                PartsIntent.UpdatePart(
                                    editingPart.copy(
                                        name = name,
                                        lifeSpanMileage = lifeSpan,
                                        lastMaintenanceOdometer = lastMaintenance,
                                        lifeSpanMonths = lifeSpanMonths,
                                        lastMaintenanceDate = lastMaintenanceDate
                                    )
                                )
                            )
                        }
                        showDialog = false
                    }
                )
            }

            partPendingDelete?.let { part ->
                ConfirmDeleteDialog(
                    title = stringResource(R.string.delete_part_title),
                    message = stringResource(R.string.delete_part_message, part.name),
                    confirmText = stringResource(R.string.delete),
                    cancelText = stringResource(R.string.cancel),
                    onConfirm = {
                        onIntent(PartsIntent.DeletePart(part))
                        partPendingDeleteId = null
                    },
                    onDismiss = { partPendingDeleteId = null }
                )
            }
        }
    }
}

@Composable
fun PartItem(
    part: PartUi,
    unit: DistanceUnit,
    currentOdometer: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (part.status) {
        MaintenanceStatus.CRITICAL -> MaterialTheme.colorScheme.error
        MaintenanceStatus.WARNING -> MaterialTheme.colorScheme.tertiary
        MaintenanceStatus.OK -> MaterialTheme.colorScheme.primary
    }

    val remaining = (part.lastMaintenanceOdometer + part.lifeSpanMileage) - currentOdometer
    val wornFraction = (1f - (remaining / part.lifeSpanMileage).toFloat()).coerceIn(0f, 1f)
    val remainingInUnit = unit.fromKm(remaining)

    IsometricCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        depthColor = statusColor.copy(alpha = 0.2f),
        glowColor = if (part.status == MaintenanceStatus.CRITICAL) statusColor.copy(alpha = 0.8f) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Build,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp),
                tint = statusColor
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = part.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.lifespan_format, unit.fromKm(part.lifeSpanMileage).roundToInt(), unit.symbol),
                    style = MaterialTheme.typography.bodySmall
                )
                part.lifeSpanMonths?.let { months ->
                    Text(
                        text = pluralStringResource(R.plurals.interval_months, months, months),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = stringResource(R.string.last_maint_format, unit.fromKm(part.lastMaintenanceOdometer).roundToInt(), unit.symbol),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        IsometricProgressBar(
            progress = wornFraction,
            color = statusColor,
            trackColor = statusColor.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.wear_used, (wornFraction * 100).roundToInt()) + " · " +
                if (remaining >= 0)
                    stringResource(R.string.wear_remaining, remainingInUnit.roundToInt(), unit.symbol)
                else
                    stringResource(R.string.wear_overdue, (-remainingInUnit).roundToInt(), unit.symbol),
            style = MaterialTheme.typography.bodySmall,
            color = statusColor
        )
    }
}
