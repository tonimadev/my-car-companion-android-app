package digital.tonima.mycarcompanion.feature.home.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import digital.tonima.mycarcompanion.core.designsystem.component.IsometricCard
import digital.tonima.mycarcompanion.core.designsystem.model.MaintenanceStatus
import digital.tonima.mycarcompanion.core.designsystem.util.formatToShortDate
import digital.tonima.mycarcompanion.feature.home.R
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    onBack: () -> Unit,
    viewModel: ChartsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.charts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        when {
            state.isLoading -> Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            !state.hasVehicle -> Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.charts_no_vehicle), modifier = Modifier.padding(16.dp))
            }
            else -> LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { UpcomingMaintenanceCard(state.upcoming) }
                item { MonthlyCostsCard(state.monthlyCosts) }
                item { ConsumptionCard(state.consumption, state.consumptionUnit) }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    IsometricCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun statusColor(status: MaintenanceStatus): Color = when (status) {
    MaintenanceStatus.OK -> MaterialTheme.colorScheme.primary
    MaintenanceStatus.WARNING -> MaterialTheme.colorScheme.tertiary
    MaintenanceStatus.CRITICAL -> MaterialTheme.colorScheme.error
}

@Composable
private fun UpcomingMaintenanceCard(upcoming: List<UpcomingMaintenance>) {
    ChartCard(stringResource(R.string.charts_upcoming_title)) {
        if (upcoming.isEmpty()) {
            Text(stringResource(R.string.charts_upcoming_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                upcoming.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(statusColor(item.status), CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.partName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                Instant.fromEpochMilliseconds(item.dueDateMillis).formatToShortDate(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = when {
                                item.daysLeft < 0 -> stringResource(R.string.charts_due_overdue, -item.daysLeft)
                                item.daysLeft == 0L -> stringResource(R.string.charts_due_today)
                                else -> stringResource(R.string.charts_due_in_days, item.daysLeft)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor(item.status)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MonthlyCostsCard(costs: List<MonthlyCost>) {
    ChartCard(stringResource(R.string.charts_costs_title)) {
        if (costs.all { it.fuel == 0.0 && it.maintenance == 0.0 }) {
            Text(stringResource(R.string.charts_costs_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            val modelProducer = remember { CartesianChartModelProducer() }
            LaunchedEffect(costs) {
                modelProducer.runTransaction {
                    columnSeries {
                        series(costs.map { it.fuel })
                        series(costs.map { it.maintenance })
                    }
                }
            }
            val monthLabels = remember(costs) {
                costs.map { it.month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
            }
            val fuelColor = MaterialTheme.colorScheme.primary
            val maintenanceColor = MaterialTheme.colorScheme.tertiary
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                            rememberLineComponent(Fill(fuelColor), 12.dp),
                            rememberLineComponent(Fill(maintenanceColor), 12.dp),
                        )
                    ),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = CartesianValueFormatter { _, x, _ -> monthLabels.getOrElse(x.toInt()) { "" } }
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendItem(fuelColor, stringResource(R.string.charts_costs_fuel))
                LegendItem(maintenanceColor, stringResource(R.string.charts_costs_maintenance))
            }
        }
    }
}

@Composable
private fun ConsumptionCard(points: List<ConsumptionPoint>, unit: digital.tonima.mycarcompanion.core.model.ConsumptionUnit) {
    ChartCard(stringResource(R.string.charts_consumption_title, unit.symbol)) {
        if (points.size < 2) {
            Text(stringResource(R.string.charts_consumption_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            val modelProducer = remember { CartesianChartModelProducer() }
            LaunchedEffect(points, unit) {
                modelProducer.runTransaction {
                    lineSeries { series(points.map { unit.fromKmPerLiter(it.kmPerLiter) }) }
                }
            }
            val dateLabels = remember(points) {
                points.map { Instant.fromEpochMilliseconds(it.dateMillis).formatToShortDate() }
            }
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = CartesianValueFormatter { _, x, _ -> dateLabels.getOrElse(x.toInt()) { "" } }
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }
    }
}
