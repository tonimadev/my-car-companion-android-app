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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mycarcompanion.core.designsystem.util.LaunchedUiEffectHandler
import digital.tonima.mycarcompanion.feature.parts.PartsContent
import digital.tonima.mycarcompanion.feature.parts.PartsIntent
import digital.tonima.mycarcompanion.feature.parts.PartsViewModel
import digital.tonima.mycarcompanion.feature.vehicles.GarageContent
import digital.tonima.mycarcompanion.feature.vehicles.GarageIntent
import digital.tonima.mycarcompanion.feature.vehicles.GarageUiEffect
import digital.tonima.mycarcompanion.feature.vehicles.GarageViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GarageAdaptiveScreen(
    onBack: () -> Unit,
    adUnitId: String,
    partsAdUnitId: String
) {
    val garageViewModel: GarageViewModel = hiltViewModel()
    val garageState by garageViewModel.state.collectAsStateWithLifecycle()
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val coroutineScope = rememberCoroutineScope()

    val selectedVehicleId = navigator.currentDestination?.contentKey
    val selectedVehicle = garageState.vehicles.find { it.id == selectedVehicleId }

    val title = if (navigator.canNavigateBack() && selectedVehicle != null) {
        selectedVehicle.name
    } else {
        stringResource(R.string.garage_parts_title)
    }

    BackHandler(navigator.canNavigateBack()) {
        coroutineScope.launch {
            navigator.navigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (navigator.canNavigateBack()) {
                                coroutineScope.launch { navigator.navigateBack() }
                            } else {
                                onBack()
                            }
                        }
                    ) {
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
                LaunchedUiEffectHandler(
                    effectFlow = garageViewModel.effect,
                    onConsumeEffect = { garageViewModel.handleIntent(GarageIntent.ConsumeEffect) },
                    onEffect = { effect ->
                        when (effect) {
                            is GarageUiEffect.NavigateToParts -> {
                                coroutineScope.launch {
                                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, effect.vehicleId)
                                }
                            }
                            else -> {}
                        }
                    }
                )

                GarageContent(
                    state = garageState,
                    effectFlow = garageViewModel.effect,
                    onIntent = garageViewModel::handleIntent,
                    onOpenParts = { id ->
                        garageViewModel.onNavigateToParts(id)
                    },
                    adUnitId = adUnitId
                )
            },
            detailPane = {
                val vehicleId = navigator.currentDestination?.contentKey
                if (vehicleId != null) {
                    val partsViewModel: PartsViewModel = hiltViewModel<PartsViewModel, PartsViewModel.Factory>(
                        key = "parts_$vehicleId",
                        creationCallback = { factory -> factory.create(vehicleId) }
                    )
                    val partsState by partsViewModel.state.collectAsStateWithLifecycle()

                    LaunchedUiEffectHandler(
                        effectFlow = partsViewModel.effect,
                        onConsumeEffect = { partsViewModel.handleIntent(PartsIntent.ConsumeEffect) },
                        onEffect = { _ -> } // Currently only ShowError which is handled in PartsContent
                    )

                    PartsContent(
                        state = partsState,
                        effectFlow = partsViewModel.effect,
                        onIntent = partsViewModel::handleIntent,
                        onBack = {
                            coroutineScope.launch {
                                navigator.navigateBack()
                            }
                        },
                        adUnitId = partsAdUnitId
                    )
                } else {
                    Text(stringResource(R.string.select_vehicle_message))
                }
            }
        )
    }
}
