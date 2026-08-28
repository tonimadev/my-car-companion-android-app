package digital.tonima.mycarcompanion.feature.vehicles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Card
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mycarcompanion.core.designsystem.component.AdBannerView
import digital.tonima.mycarcompanion.core.designsystem.model.VehicleUi
import digital.tonima.mycarcompanion.core.designsystem.util.LaunchedUiEffectHandler
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

@Composable
fun GarageScreen(
    onNavigateToParts: (Long) -> Unit,
    adUnitId: String,
    viewModel: GarageViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    GarageContent(
        state = state,
        effectFlow = viewModel.effect,
        onIntent = viewModel::handleIntent,
        onOpenParts = onNavigateToParts,
        adUnitId = adUnitId
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageContent(
    state: GarageState,
    effectFlow: Flow<GarageUiEffect?>,
    onIntent: (GarageIntent) -> Unit,
    onOpenParts: (Long) -> Unit,
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editingVehicleId by rememberSaveable { mutableStateOf<Long?>(null) }
    
    val editingVehicle = remember(editingVehicleId, state.vehicles) {
        state.vehicles.find { it.id == editingVehicleId }
    }

    LaunchedUiEffectHandler(
        effectFlow = effectFlow,
        onConsumeEffect = { onIntent(GarageIntent.ConsumeEffect) },
        onEffect = { effect ->
            when (effect) {
                is GarageUiEffect.NavigateToParts -> {
                    onOpenParts(effect.vehicleId)
                }
                is GarageUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.garage_title)) })
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
                    items(state.vehicles, key = { it.id }) { vehicle ->
                        VehicleItem(
                            vehicle = vehicle,
                            unit = state.distanceUnit,
                            onEdit = {
                                editingVehicleId = vehicle.id
                                showDialog = true
                            },
                            onDelete = { onIntent(GarageIntent.DeleteVehicle(vehicle)) },
                            onSetCurrent = { onIntent(GarageIntent.SetCurrentVehicle(vehicle.id)) },
                            onOpenParts = { onOpenParts(vehicle.id) }
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
                    editingVehicleId = null
                    showDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_vehicle))
            }

            if (showDialog) {
                VehicleEditDialog(
                    vehicle = editingVehicle,
                    unit = state.distanceUnit,
                    onDismiss = { showDialog = false },
                    onConfirm = { name, odometer ->
                        if (editingVehicle == null) {
                            onIntent(GarageIntent.AddVehicle(name, odometer))
                        } else {
                            onIntent(
                                GarageIntent.UpdateVehicle(
                                    editingVehicle.copy(name = name, currentOdometer = odometer)
                                )
                            )
                        }
                        showDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun VehicleItem(
    vehicle: VehicleUi,
    unit: DistanceUnit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetCurrent: () -> Unit,
    onOpenParts: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onOpenParts() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = vehicle.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${unit.fromKm(vehicle.currentOdometer).roundToInt()} ${unit.name.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onSetCurrent) {
                Icon(
                    imageVector = if (vehicle.isCurrent) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = stringResource(R.string.set_current),
                    tint = if (vehicle.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}
