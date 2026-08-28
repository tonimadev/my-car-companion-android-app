package digital.tonima.mycarcompanion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mycarcompanion.feature.parts.PartsContent
import digital.tonima.mycarcompanion.feature.parts.PartsViewModel
import digital.tonima.mycarcompanion.feature.vehicles.GarageContent
import digital.tonima.mycarcompanion.feature.vehicles.GarageViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GarageAdaptiveScreen(
    onBack: () -> Unit,
    adUnitId: String,
    partsAdUnitId: String
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    var selectedVehicleId by rememberSaveable { mutableStateOf<Long?>(null) }
    val coroutineScope = rememberCoroutineScope()

    BackHandler(navigator.canNavigateBack()) {
        coroutineScope.launch {
            navigator.navigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.garage_parts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        ListDetailPaneScaffold(
            modifier = Modifier.padding(padding),
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            listPane = {
                val garageViewModel: GarageViewModel = hiltViewModel()
                val garageState by garageViewModel.state.collectAsStateWithLifecycle()
                GarageContent(
                    state = garageState,
                    onIntent = garageViewModel::handleIntent,
                    onOpenParts = { id ->
                        selectedVehicleId = id
                        coroutineScope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                        }
                    },
                    adUnitId = adUnitId
                )
            },
            detailPane = {
                val vehicleId = navigator.currentDestination?.contentKey ?: selectedVehicleId
                if (vehicleId != null) {
                    val partsViewModel: PartsViewModel = hiltViewModel<PartsViewModel, PartsViewModel.Factory>(
                        key = "parts_$vehicleId",
                        creationCallback = { factory -> factory.create(vehicleId) }
                    )
                    val partsState by partsViewModel.state.collectAsStateWithLifecycle()
                    PartsContent(
                        state = partsState,
                        onIntent = partsViewModel::handleIntent,
                        adUnitId = partsAdUnitId
                    )
                } else {
                    Text(stringResource(R.string.select_vehicle_message))
                }
            }
        )
    }
}
