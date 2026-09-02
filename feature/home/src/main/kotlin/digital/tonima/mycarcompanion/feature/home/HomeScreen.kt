package digital.tonima.mycarcompanion.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mycarcompanion.core.designsystem.MyCarCompanionTheme
import digital.tonima.mycarcompanion.core.designsystem.component.AdBannerView
import digital.tonima.mycarcompanion.core.designsystem.model.PartUi
import digital.tonima.mycarcompanion.core.designsystem.model.VehicleUi
import digital.tonima.mycarcompanion.core.designsystem.util.CurrencyUtils
import digital.tonima.mycarcompanion.core.designsystem.util.LaunchedUiEffectHandler
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

@Composable
fun HomeRoute(
    onNavigateToSettings: () -> Unit,
    onNavigateToFuel: () -> Unit,
    onNavigateToMaintenanceHistory: () -> Unit,
    adUnitId: String,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        effectFlow = viewModel.effect,
        onIntent = viewModel::onIntent,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToFuel = onNavigateToFuel,
        onNavigateToMaintenanceHistory = onNavigateToMaintenanceHistory,
        adUnitId = adUnitId
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    effectFlow: Flow<HomeUiEffect?>,
    onIntent: (HomeUiIntent) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFuel: () -> Unit,
    onNavigateToMaintenanceHistory: () -> Unit,
    adUnitId: String
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedPartForMaintenance by remember { mutableStateOf<PartUi?>(null) }
    var showUpdateOdometerDialog by remember { mutableStateOf(false) }

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val useTwoColumns = adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    LaunchedUiEffectHandler(
        effectFlow = effectFlow,
        onConsumeEffect = { onIntent(HomeUiIntent.ConsumeEffect) },
        onEffect = { effect ->
            when (effect) {
                is HomeUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                HomeUiEffect.NavigateToSettings -> {
                    onNavigateToSettings()
                }
                HomeUiEffect.NavigateToFuel -> {
                    onNavigateToFuel()
                }
                HomeUiEffect.NavigateToMaintenanceHistory -> {
                    onNavigateToMaintenanceHistory()
                }
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = { onIntent(HomeUiIntent.NavigateToMaintenanceHistory) }) {
                        Icon(Icons.Default.Build, contentDescription = "Manutenção")
                    }
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
                    if (useTwoColumns) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Left Column: Stats & Odometer
                            Column(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp)
                            ) {
                                OdometerDisplay(
                                    odometer = vehicle.currentOdometer,
                                    unit = uiState.distanceUnit,
                                    onEditClick = { showUpdateOdometerDialog = true },
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = "Gastos Totais", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = CurrencyUtils.formatCurrency(uiState.totalMaintenanceCost + uiState.totalFuelCost),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Mnt: ${CurrencyUtils.formatCurrency(uiState.totalMaintenanceCost)} | Comb: ${CurrencyUtils.formatCurrency(uiState.totalFuelCost)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                uiState.averageFuelConsumption?.let { avg ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        onClick = onNavigateToFuel
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(text = "Consumo", style = MaterialTheme.typography.labelSmall)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = uiState.consumptionUnit.format(avg),
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                val (icon, color) = when (uiState.fuelConsumptionTrend) {
                                                    FuelTrend.IMPROVING -> Icons.AutoMirrored.Rounded.TrendingUp to Color(0xFF4CAF50)
                                                    FuelTrend.WORSENING -> Icons.AutoMirrored.Rounded.TrendingDown to MaterialTheme.colorScheme.error
                                                    FuelTrend.STABLE -> Icons.AutoMirrored.Rounded.TrendingFlat to MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                                }
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = color,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    uiState.costPerDistance?.let { costPerDist ->
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(text = "Custo/${uiState.distanceUnit.name.lowercase()}", style = MaterialTheme.typography.labelSmall)
                                                Text(
                                                    text = CurrencyUtils.formatCurrency(costPerDist),
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                            }
                                        }
                                    }

                                    uiState.estimatedRange?.let { range ->
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(text = "Autonomia", style = MaterialTheme.typography.labelSmall)
                                                Text(
                                                    text = "${range.roundToInt()} ${uiState.distanceUnit.name.lowercase()}",
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                AdBannerView(
                                    isProUser = uiState.isProUser,
                                    adId = adUnitId,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            // Right Column: Maintenance List
                            MaintenanceList(
                                parts = uiState.parts,
                                predictions = uiState.predictions,
                                isAiUser = uiState.isAiUser,
                                currentOdometer = vehicle.currentOdometer,
                                unit = uiState.distanceUnit,
                                onPerformMaintenance = { selectedPartForMaintenance = it },
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        // Original Column Layout
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
                                        text = CurrencyUtils.formatCurrency(uiState.totalMaintenanceCost + uiState.totalFuelCost),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Mnt: ${CurrencyUtils.formatCurrency(uiState.totalMaintenanceCost)} | Comb: ${CurrencyUtils.formatCurrency(uiState.totalFuelCost)}",
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
                                        Text(text = "Consumo", style = MaterialTheme.typography.labelSmall)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = uiState.consumptionUnit.format(avg),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            val (icon, color) = when (uiState.fuelConsumptionTrend) {
                                                FuelTrend.IMPROVING -> Icons.AutoMirrored.Rounded.TrendingUp to Color(0xFF4CAF50)
                                                FuelTrend.WORSENING -> Icons.AutoMirrored.Rounded.TrendingDown to MaterialTheme.colorScheme.error
                                                FuelTrend.STABLE -> Icons.AutoMirrored.Rounded.TrendingFlat to MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                            }
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Text(
                                            text = "Ver histórico",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.costPerDistance?.let { costPerDist ->
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "Custo/${uiState.distanceUnit.name.lowercase()}", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = CurrencyUtils.formatCurrency(costPerDist),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }
                            }

                            uiState.estimatedRange?.let { range ->
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "Autonomia", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = "${range.roundToInt()} ${uiState.distanceUnit.name.lowercase()}",
                                            style = MaterialTheme.typography.titleSmall
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
                    }
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
            onConfirm = { newOdometer, cost, notes, date ->
                onIntent(HomeUiIntent.PerformMaintenance(part, newOdometer, cost, notes, date))
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
    val sampleVehicle = VehicleUi(id = 1, name = "My Car", currentOdometer = 15000.0, isCurrent = true)
    val sampleParts = kotlinx.collections.immutable.persistentListOf(
        PartUi(id = 1, vehicleId = 1, name = "Oil Change", lifeSpanMileage = 5000.0, lastMaintenanceOdometer = 10500.0),
        PartUi(id = 2, vehicleId = 1, name = "Tire Rotation", lifeSpanMileage = 10000.0, lastMaintenanceOdometer = 5000.0)
    )
    
    MyCarCompanionTheme {
        HomeScreen(
            uiState = HomeUiState(
                vehicles = kotlinx.collections.immutable.persistentListOf(sampleVehicle),
                currentVehicle = sampleVehicle,
                parts = sampleParts,
                distanceUnit = DistanceUnit.KM
            ),
            effectFlow = kotlinx.coroutines.flow.flowOf(null),
            onIntent = {},
            onNavigateToSettings = {},
            onNavigateToFuel = {},
            onNavigateToMaintenanceHistory = {},
            adUnitId = ""
        )
    }
}
