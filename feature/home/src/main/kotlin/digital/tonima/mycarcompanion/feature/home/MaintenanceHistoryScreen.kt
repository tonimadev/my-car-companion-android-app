package digital.tonima.mycarcompanion.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mycarcompanion.core.designsystem.model.MaintenanceRecordUi
import digital.tonima.mycarcompanion.core.designsystem.util.CurrencyUtils
import digital.tonima.mycarcompanion.core.designsystem.util.formatToShortDate
import digital.tonima.mycarcompanion.core.model.DistanceUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceHistoryScreen(
    onBack: () -> Unit,
    distanceUnit: DistanceUnit,
    viewModel: MaintenanceHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingRecord by remember { mutableStateOf<MaintenanceRecordUi?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Manutenções") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Nenhuma manutenção registrada.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items, key = { it.id }) { record ->
                    MaintenanceRecordCard(
                        record = record,
                        unit = distanceUnit,
                        onEdit = { editingRecord = record },
                        onDelete = { viewModel.deleteRecord(record) }
                    )
                }
            }
        }

        editingRecord?.let { record ->
            EditMaintenanceDialog(
                record = record,
                unit = distanceUnit,
                onConfirm = { odo, cost, notes, date ->
                    viewModel.updateRecord(record.copy(odometerAtMaintenance = odo, cost = cost, notes = notes, date = date))
                    editingRecord = null
                },
                onDismiss = { editingRecord = null }
            )
        }
    }
}

@Composable
fun MaintenanceRecordCard(
    record: MaintenanceRecordUi,
    unit: DistanceUnit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.partName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${unit.fromKm(record.odometerAtMaintenance).toInt()} ${unit.name.lowercase()} | ${CurrencyUtils.formatCurrency(record.cost)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = record.date.formatToShortDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.notes.isNotBlank()) {
                    Text(
                        text = record.notes,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
