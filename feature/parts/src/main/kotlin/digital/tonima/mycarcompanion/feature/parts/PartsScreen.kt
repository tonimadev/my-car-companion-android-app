package digital.tonima.mycarcompanion.feature.parts

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.runtime.LaunchedEffect
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
import digital.tonima.mycarcompanion.core.designsystem.model.PartUi
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartsScreen(
    onBack: () -> Unit,
    adUnitId: String,
    viewModel: PartsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PartsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
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
    ) { padding ->
        PartsContent(
            state = state,
            onIntent = viewModel::handleIntent,
            adUnitId = adUnitId,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun PartsContent(
    state: PartsState,
    onIntent: (PartsIntent) -> Unit,
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editingPartId by rememberSaveable { mutableStateOf<Long?>(null) }
    
    val editingPart = remember(editingPartId, state.parts) {
        state.parts.find { it.id == editingPartId }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(state.parts, key = { it.id }) { part ->
                    PartItem(
                        part = part,
                        unit = state.distanceUnit,
                        onEdit = {
                            editingPartId = part.id
                            showDialog = true
                        },
                        onDelete = { onIntent(PartsIntent.DeletePart(part)) }
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
    }
}

@Composable
fun PartItem(
    part: PartUi,
    unit: DistanceUnit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Build,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = part.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.lifespan_format, unit.fromKm(part.lifeSpanMileage).roundToInt(), unit.name.lowercase()),
                    style = MaterialTheme.typography.bodySmall
                )
                part.lifeSpanMonths?.let { months ->
                    Text(
                        text = "Intervalo: $months meses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = stringResource(R.string.last_maint_format, unit.fromKm(part.lastMaintenanceOdometer).roundToInt(), unit.name.lowercase()),
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
    }
}
