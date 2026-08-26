package digital.tonima.mycarcompanion.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mycarcompanion.core.designsystem.MyCarCompanionTheme
import digital.tonima.mycarcompanion.core.designsystem.component.AdBannerView
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle

@Composable
fun HomeRoute(
    onNavigateToSettings: () -> Unit,
    onNavigateToFuel: () -> Unit,
    adUnitId: String,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToFuel = onNavigateToFuel,
        adUnitId = adUnitId
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFuel: () -> Unit,
    adUnitId: String
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedPartForMaintenance by remember { mutableStateOf<Part?>(null) }
    var showUpdateOdometerDialog by remember { mutableStateOf(false) }

    // Handle events
    LaunchedEffect(uiState.events) {
        uiState.events.forEach { event ->
            when (event) {
                is HomeUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                    onIntent(HomeUiIntent.ConsumeEvent(event.id))
                }
                is HomeUiEvent.NavigateToSettings -> {
                    onNavigateToSettings()
                    onIntent(HomeUiIntent.ConsumeEvent(event.id))
                }
                is HomeUiEvent.NavigateToFuel -> {
                    onNavigateToFuel()
                    onIntent(HomeUiIntent.ConsumeEvent(event.id))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = { onIntent(HomeUiIntent.NavigateToFuel) }) {
                        Icon(Icons.Default.LocalGasStation, contentDescription = "Abastecimento")
                    }
                    IconButton(onClick = { onIntent(HomeUiIntent.NavigateToSettings) }) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                VehicleSelector(
                    vehicles = uiState.vehicles,
                    selectedVehicle = uiState.currentVehicle,
                    onVehicleSelected = { onIntent(HomeUiIntent.SelectVehicle(it)) }
                )

                uiState.currentVehicle?.let { vehicle ->
                    OdometerDisplay(
                        odometer = vehicle.currentOdometer,
                        unit = uiState.distanceUnit,
                        onEditClick = { showUpdateOdometerDialog = true },
                        modifier = Modifier.padding(16.dp)
                    )

                    // Resumo Financeiro e Consumo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Gastos Totais", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "R$ %.2f".format(uiState.totalMaintenanceCost + uiState.totalFuelCost),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Mnt: R$ %.0f | Comb: R$ %.0f".format(uiState.totalMaintenanceCost, uiState.totalFuelCost),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        uiState.averageFuelConsumption?.let { avg ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                onClick = onNavigateToFuel
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "Média Consumo", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "%.1f km/L".format(avg),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Ver histórico",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    AdBannerView(
                        isProUser = uiState.isProUser,
                        adId = adUnitId,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    MaintenanceList(
                        parts = uiState.parts,
                        predictions = uiState.predictions,
                        isAiUser = uiState.isAiUser,
                        currentOdometer = vehicle.currentOdometer,
                        unit = uiState.distanceUnit,
                        onPerformMaintenance = { selectedPartForMaintenance = it },
                        modifier = Modifier.weight(1f)
                    )
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_car_placeholder),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.no_vehicle_selected),
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.no_vehicle_selected_subtext),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = onNavigateToSettings,
                                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.add_vehicle_button),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPartForMaintenance?.let { part ->
        MaintenanceDialog(
            part = part,
            currentOdometer = uiState.currentVehicle?.currentOdometer ?: 0.0,
            unit = uiState.distanceUnit,
            onConfirm = { newOdometer, cost, notes ->
                onIntent(HomeUiIntent.PerformMaintenance(part, newOdometer, cost, notes))
                selectedPartForMaintenance = null
            },
            onDismiss = { selectedPartForMaintenance = null }
        )
    }

    if (showUpdateOdometerDialog) {
        UpdateOdometerDialog(
            currentOdometer = uiState.currentVehicle?.currentOdometer ?: 0.0,
            unit = uiState.distanceUnit,
            onConfirm = { newOdometer ->
                onIntent(HomeUiIntent.UpdateOdometer(newOdometer))
                showUpdateOdometerDialog = false
            },
            onDismiss = { showUpdateOdometerDialog = false }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomePreview() {
    val sampleVehicle = Vehicle(id = 1, name = "My Car", currentOdometer = 15000.0, isCurrent = true)
    val sampleParts = listOf(
        Part(id = 1, vehicleId = 1, name = "Oil Change", lifeSpanMileage = 5000.0, lastMaintenanceOdometer = 10500.0),
        Part(id = 2, vehicleId = 1, name = "Tire Rotation", lifeSpanMileage = 10000.0, lastMaintenanceOdometer = 5000.0)
    )
    
    MyCarCompanionTheme {
        HomeScreen(
            uiState = HomeUiState(
                vehicles = listOf(sampleVehicle),
                currentVehicle = sampleVehicle,
                parts = sampleParts,
                distanceUnit = DistanceUnit.KM
            ),
            onIntent = {},
            onNavigateToSettings = {},
            onNavigateToFuel = {},
            adUnitId = ""
        )
    }
}
