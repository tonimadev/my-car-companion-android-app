package digital.tonima.mycarcompanion.feature.tracking.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mycarcompanion.core.designsystem.component.AdBannerView
import digital.tonima.mycarcompanion.core.designsystem.component.ConfirmDeleteDialog
import digital.tonima.mycarcompanion.core.designsystem.component.GarageBackground
import digital.tonima.mycarcompanion.core.designsystem.component.IsometricCard
import digital.tonima.mycarcompanion.core.designsystem.model.FuelRecordUi
import digital.tonima.mycarcompanion.core.designsystem.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelHistoryScreen(
    onNavigateUp: () -> Unit,
    onNavigateToAddFuel: (Long?) -> Unit,
    adUnitId: String,
    modifier: Modifier = Modifier,
    viewModel: FuelHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val useTwoColumns = adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    var recordPendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val recordPendingDelete = remember(recordPendingDeleteId, uiState.items) {
        uiState.items.find { it.id == recordPendingDeleteId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Controle de Abastecimento",
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToAddFuel(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Abastecer") }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        GarageBackground(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.currentVehicle == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhum veículo selecionado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                if (useTwoColumns) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Column: Summaries
                        Column(
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(16.dp)
                        ) {
                            FuelSummaryCard(
                                label = "Consumo Médio",
                                value = uiState.averageConsumption?.let { "%.1f km/L".format(it) } ?: "-- km/L",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                depthColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            FuelSummaryCard(
                                label = "Gasto Total",
                                value = CurrencyUtils.formatCurrency(uiState.totalSpent),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                depthColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                            )
                        }

                        // Right Column: History List
                        Column(
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = "Histórico de Abastecimentos",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            FuelHistoryList(
                                items = uiState.items,
                                onEdit = onNavigateToAddFuel,
                                onRequestDelete = { recordPendingDeleteId = it },
                                isProUser = uiState.isProUser,
                                adUnitId = adUnitId
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Summary Cards
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FuelSummaryCard(
                                label = "Consumo Médio",
                                value = uiState.averageConsumption?.let { "%.1f km/L".format(it) } ?: "-- km/L",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                depthColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                modifier = Modifier.weight(1f)
                            )

                            FuelSummaryCard(
                                label = "Gasto Total",
                                value = CurrencyUtils.formatCurrency(uiState.totalSpent),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                depthColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Histórico de Abastecimentos",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        FuelHistoryList(
                            items = uiState.items,
                            onEdit = onNavigateToAddFuel,
                            onRequestDelete = { recordPendingDeleteId = it },
                            isProUser = uiState.isProUser,
                            adUnitId = adUnitId,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    recordPendingDelete?.let { record ->
        ConfirmDeleteDialog(
            title = "Excluir abastecimento?",
            message = "Tem certeza de que deseja excluir este registro de ${"%.2f".format(record.liters)} litros? Esta ação não pode ser desfeita.",
            confirmText = "Excluir",
            cancelText = "Cancelar",
            onConfirm = {
                viewModel.deleteRecord(record)
                recordPendingDeleteId = null
            },
            onDismiss = { recordPendingDeleteId = null }
        )
    }
}

@Composable
private fun FuelSummaryCard(
    label: String,
    value: String,
    containerColor: Color,
    depthColor: Color,
    modifier: Modifier = Modifier
) {
    IsometricCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = containerColor,
        depthColor = depthColor
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuelHistoryList(
    items: List<FuelRecordUi>,
    onEdit: (Long) -> Unit,
    onRequestDelete: (Long) -> Unit,
    isProUser: Boolean,
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.LocalGasStation,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nenhum abastecimento registrado.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Toque em \"Abastecer\" para adicionar o primeiro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val dismissState = rememberSwipeToDismissBoxState()

                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        onRequestDelete(item.id)
                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                    }
                }

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val backgroundColor = when (dismissState.dismissDirection) {
                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
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
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError
                                    )
                                }
                            }
                        }
                    },
                    enableDismissFromStartToEnd = false
                ) {
                    FuelRecordCard(
                        item = item,
                        onEdit = { onEdit(item.id) },
                        onDelete = { onRequestDelete(item.id) }
                    )
                }
            }

            item {
                AdBannerView(
                    isProUser = isProUser,
                    adId = adUnitId,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun FuelRecordCard(
    item: FuelRecordUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(item.date.toEpochMilliseconds()))

    IsometricCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        containerColor = MaterialTheme.colorScheme.surface,
        depthColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocalGasStation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.fuelType} - ${CurrencyUtils.formatCurrency(item.totalCost)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "%.2f Litros | Km: %.0f".format(item.liters, item.mileage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                item.consumptionKmPerL?.let { consumption ->
                    Spacer(modifier = Modifier.height(4.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Média: %.1f km/L".format(consumption)) }
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar abastecimento",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir abastecimento",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
