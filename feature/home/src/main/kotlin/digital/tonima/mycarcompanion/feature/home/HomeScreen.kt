package digital.tonima.mycarcompanion.feature.home

import digital.tonima.mycarcompanion.core.designsystem.util.NumberUtils
import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import android.net.Uri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import digital.tonima.mycarcompanion.core.designsystem.MyCarCompanionTheme
import digital.tonima.mycarcompanion.core.designsystem.component.AdBannerView
import digital.tonima.mycarcompanion.core.designsystem.component.GarageBackground
import digital.tonima.mycarcompanion.core.designsystem.component.IsometricCarView
import digital.tonima.mycarcompanion.core.designsystem.component.IsometricCard
import digital.tonima.mycarcompanion.core.designsystem.model.PartUi
import digital.tonima.mycarcompanion.core.designsystem.model.VehicleUi
import digital.tonima.mycarcompanion.core.designsystem.util.CurrencyUtils
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import digital.tonima.mycarcompanion.core.designsystem.R as DesignR

@Composable
fun HomeRoute(
    onNavigateToSettings: () -> Unit,
    onNavigateToFuel: () -> Unit,
    onNavigateToMaintenanceHistory: () -> Unit,
    onNavigateToDiagnosticChat: () -> Unit,
    adUnitId: String,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle(initialValue = null)

    HomeScreen(
        uiState = uiState,
        effect = effect,
        onIntent = viewModel::onIntent,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToFuel = onNavigateToFuel,
        onNavigateToMaintenanceHistory = onNavigateToMaintenanceHistory,
        onNavigateToDiagnosticChat = onNavigateToDiagnosticChat,
        onSubscribeAiClick = viewModel::subscribeAi,
        adUnitId = adUnitId
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    effect: HomeUiEffect?,
    onIntent: (HomeUiIntent) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFuel: () -> Unit,
    onNavigateToMaintenanceHistory: () -> Unit,
    onNavigateToDiagnosticChat: () -> Unit = {},
    onSubscribeAiClick: (Activity) -> Unit = {},
    adUnitId: String
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedPartForMaintenance by remember { mutableStateOf<PartUi?>(null) }
    var showUpdateOdometerDialog by remember { mutableStateOf(false) }

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val useTwoColumns = adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    LaunchedEffect(effect) {
        if (effect != null) {
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
                HomeUiEffect.NavigateToDiagnosticChat -> {
                    onNavigateToDiagnosticChat()
                }
            }
            onIntent(HomeUiIntent.ConsumeEffect)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            shadow = Shadow(
                                color = MaterialTheme.colorScheme.primary,
                                blurRadius = 15f
                            )
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ),
                actions = {
                    IconButton(onClick = { onIntent(HomeUiIntent.ToggleFinancialData) }) {
                        Icon(
                            imageVector = if (uiState.showFinancialData) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = stringResource(R.string.toggle_financial_data)
                        )
                    }
                    IconButton(onClick = { onIntent(HomeUiIntent.NavigateToDiagnosticChat) }) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = stringResource(R.string.diagnostic_chat_title))
                    }
                    IconButton(onClick = { onIntent(HomeUiIntent.NavigateToMaintenanceHistory) }) {
                        Icon(Icons.Default.Build, contentDescription = stringResource(R.string.maintenance_history_title))
                    }
                    IconButton(onClick = { onIntent(HomeUiIntent.NavigateToFuel) }) {
                        Icon(Icons.Default.LocalGasStation, contentDescription = stringResource(R.string.fuel_log))
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
        GarageBackground(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
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
                            // Left Column: Compact overview (hero, odometer, stats)
                            Column(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 8.dp)
                            ) {
                                HomeHeroSection(
                                    parts = uiState.parts,
                                    isAiUser = uiState.isAiUser,
                                    aiInsight = uiState.aiInsight,
                                    onGenerateAiInsightClick = { onIntent(HomeUiIntent.GenerateAiInsight) },
                                    onSubscribeAiClick = onSubscribeAiClick
                                )

                                OdometerDisplay(
                                    odometer = vehicle.currentOdometer,
                                    unit = uiState.distanceUnit,
                                    onEditClick = { showUpdateOdometerDialog = true },
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                if (uiState.showFinancialData) {
                                    IsometricCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        depthColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                    ) {
                                        Column {
                                            Text(text = stringResource(R.string.total_expenses), style = MaterialTheme.typography.labelSmall)
                                            Text(
                                                text = CurrencyUtils.formatCurrency(uiState.totalMaintenanceCost + uiState.totalFuelCost),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = stringResource(
                                                    R.string.expenses_breakdown,
                                                    CurrencyUtils.formatCurrency(uiState.totalMaintenanceCost),
                                                    CurrencyUtils.formatCurrency(uiState.totalFuelCost)
                                                ),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                uiState.averageFuelConsumption?.let { avg ->
                                    IsometricCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        depthColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                    ) {
                                        Column(modifier = Modifier.clickable { onNavigateToFuel() }) {
                                            Text(text = stringResource(DesignR.string.label_consumption), style = MaterialTheme.typography.labelSmall)
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
                                    if (uiState.showFinancialData) {
                                        uiState.costPerDistance?.let { costPerDist ->
                                            IsometricCard(
                                                modifier = Modifier.weight(1f),
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                depthColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            ) {
                                                Column {
                                                    Text(text = stringResource(R.string.cost_per_distance, uiState.distanceUnit.symbol), style = MaterialTheme.typography.labelSmall)
                                                    Text(
                                                        text = CurrencyUtils.formatCurrency(costPerDist),
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    uiState.estimatedRange?.let { range ->
                                        IsometricCard(
                                            modifier = Modifier.weight(1f),
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            depthColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        ) {
                                            Column {
                                                Text(text = stringResource(R.string.estimated_range), style = MaterialTheme.typography.labelSmall)
                                                Text(
                                                    text = "${NumberUtils.formatDecimal(range, 0)} ${uiState.distanceUnit.symbol}",
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

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
                        // Single scrollable list: header content plus the maintenance
                        // list share one LazyColumn so the whole screen scrolls together
                        // and the parts list is never squeezed out of view.
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                HomeHeroSection(
                                    parts = uiState.parts,
                                    isAiUser = uiState.isAiUser,
                                    aiInsight = uiState.aiInsight,
                                    onGenerateAiInsightClick = { onIntent(HomeUiIntent.GenerateAiInsight) },
                                    onSubscribeAiClick = onSubscribeAiClick
                                )
                            }

                            item {
                                OdometerDisplay(
                                    odometer = vehicle.currentOdometer,
                                    unit = uiState.distanceUnit,
                                    onEditClick = { showUpdateOdometerDialog = true },
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            item {
                                // Resumo Financeiro e Consumo
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (uiState.showFinancialData) {
                                        IsometricCard(
                                            modifier = Modifier.weight(1f),
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            depthColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                        ) {
                                            Column {
                                                Text(text = stringResource(R.string.total_expenses), style = MaterialTheme.typography.labelSmall)
                                                Text(
                                                    text = CurrencyUtils.formatCurrency(uiState.totalMaintenanceCost + uiState.totalFuelCost),
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    text = stringResource(
                                                    R.string.expenses_breakdown,
                                                    CurrencyUtils.formatCurrency(uiState.totalMaintenanceCost),
                                                    CurrencyUtils.formatCurrency(uiState.totalFuelCost)
                                                ),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }

                                    uiState.averageFuelConsumption?.let { avg ->
                                        IsometricCard(
                                            modifier = Modifier.weight(1f),
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            depthColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                        ) {
                                            Column(modifier = Modifier.clickable { onNavigateToFuel() }) {
                                                Text(text = stringResource(DesignR.string.label_consumption), style = MaterialTheme.typography.labelSmall)
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
                                                    text = stringResource(R.string.view_history),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.showFinancialData) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        uiState.costPerDistance?.let { costPerDist ->
                                            IsometricCard(
                                                modifier = Modifier.weight(1f),
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                depthColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            ) {
                                                Column {
                                                    Text(text = stringResource(R.string.cost_per_distance, uiState.distanceUnit.symbol), style = MaterialTheme.typography.labelSmall)
                                                    Text(
                                                        text = CurrencyUtils.formatCurrency(costPerDist),
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                }
                                            }
                                        }

                                        uiState.estimatedRange?.let { range ->
                                            IsometricCard(
                                                modifier = Modifier.weight(1f),
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                depthColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            ) {
                                                Column {
                                                    Text(text = stringResource(R.string.estimated_range), style = MaterialTheme.typography.labelSmall)
                                                    Text(
                                                        text = "${NumberUtils.formatDecimal(range, 0)} ${uiState.distanceUnit.symbol}",
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                AdBannerView(
                                    isProUser = uiState.isProUser,
                                    adId = adUnitId,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            maintenanceItems(
                                parts = uiState.parts,
                                predictions = uiState.predictions,
                                isAiUser = uiState.isAiUser,
                                currentOdometer = vehicle.currentOdometer,
                                unit = uiState.distanceUnit,
                                onPerformMaintenance = { selectedPartForMaintenance = it }
                            )

                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            IsometricCarView(
                                parts = persistentListOf(),
                                modifier = Modifier.size(200.dp)
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
    val sampleParts = persistentListOf(
        PartUi(id = 1, vehicleId = 1, name = "Oil Change", lifeSpanMileage = 5000.0, lastMaintenanceOdometer = 10500.0),
        PartUi(id = 2, vehicleId = 1, name = "Tire Rotation", lifeSpanMileage = 10000.0, lastMaintenanceOdometer = 5000.0)
    )
    
    MyCarCompanionTheme {
        HomeScreen(
            uiState = HomeUiState(
                vehicles = persistentListOf(sampleVehicle),
                currentVehicle = sampleVehicle,
                parts = sampleParts,
                distanceUnit = DistanceUnit.KM
            ),
            effect = null,
            onIntent = {},
            onNavigateToSettings = {},
            onNavigateToFuel = {},
            onNavigateToMaintenanceHistory = {},
            adUnitId = ""
        )
    }
}

@Composable
private fun HomeHeroSection(
    parts: ImmutableList<PartUi>,
    isAiUser: Boolean,
    aiInsight: AiInsightUiState,
    onGenerateAiInsightClick: () -> Unit,
    onSubscribeAiClick: (Activity) -> Unit
) {
    val context = LocalContext.current
    val gasStationQuery = stringResource(R.string.maps_query_gas_station)
    val mechanicQuery = stringResource(R.string.maps_query_mechanic)

    IsometricCarView(
        parts = parts,
        modifier = Modifier.padding(top = 8.dp)
    )

    // Search shortcuts (gas stations and mechanics)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IsometricCard(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, "geo:0,0?q=${Uri.encode(gasStationQuery)}".toUri())
                    context.startActivity(intent)
                },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            depthColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.LocalGasStation, contentDescription = null)
                Text(stringResource(R.string.find_gas_stations), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }

        IsometricCard(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, "geo:0,0?q=${Uri.encode(mechanicQuery)}".toUri())
                    context.startActivity(intent)
                },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            depthColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Build, contentDescription = null)
                Text(stringResource(R.string.find_mechanics), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }
    }

    AiInsightCard(
        isAiUser = isAiUser,
        insightState = aiInsight,
        onGenerateClick = onGenerateAiInsightClick,
        onUpgradeClick = {
            (context as? Activity)?.let { onSubscribeAiClick(it) }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
