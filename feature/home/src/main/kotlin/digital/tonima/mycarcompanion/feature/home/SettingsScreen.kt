package digital.tonima.mycarcompanion.feature.home

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.model.ConsumptionUnit
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val proUserProvider: ProUserProvider,
) : ViewModel() {
    val distanceUnit: StateFlow<DistanceUnit> = userPreferencesRepository.distanceUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DistanceUnit.KM)

    val consumptionUnit: StateFlow<ConsumptionUnit> = userPreferencesRepository.consumptionUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConsumptionUnit.KM_L)

    val isProUser: StateFlow<Boolean> = proUserProvider.isProUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAiUser: StateFlow<Boolean> = proUserProvider.isAiUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch {
            userPreferencesRepository.setDistanceUnit(unit)
        }
    }

    fun setConsumptionUnit(unit: ConsumptionUnit) {
        viewModelScope.launch {
            userPreferencesRepository.setConsumptionUnit(unit)
        }
    }

    fun refreshPurchases() {
        proUserProvider.refresh()
    }

    fun purchasePro(activity: Activity) {
        proUserProvider.launchPurchasePro(activity)
    }

    fun subscribeAi(activity: Activity) {
        proUserProvider.launchSubscribeAi(activity)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToGarage: () -> Unit,
    onPurchaseRequest: () -> Unit = {},
    onSubscriptionRequest: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val distanceUnit by viewModel.distanceUnit.collectAsStateWithLifecycle()
    val consumptionUnit by viewModel.consumptionUnit.collectAsStateWithLifecycle()
    val isProUser by viewModel.isProUser.collectAsStateWithLifecycle()
    val isAiUser by viewModel.isAiUser.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.settings_plans_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (!isAiUser) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_subscribe_ai)) },
                    supportingContent = { Text(stringResource(R.string.settings_subscribe_ai_description)) },
                    leadingContent = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.selectable(
                        selected = false,
                        onClick = {
                            val activity = context as? Activity
                            activity?.let { viewModel.subscribeAi(it) } ?: onSubscriptionRequest()
                        }
                    )
                )
            } else {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_ai_active)) },
                    supportingContent = { Text(stringResource(R.string.settings_ai_active_description)) },
                    leadingContent = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                )
            }

            if (!isProUser) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_remove_ads)) },
                    supportingContent = { Text(stringResource(R.string.settings_remove_ads_description)) },
                    leadingContent = { Icon(Icons.Rounded.Block, contentDescription = null) },
                    modifier = Modifier.selectable(
                        selected = false,
                        onClick = {
                            val activity = context as? Activity
                            activity?.let { viewModel.purchasePro(it) } ?: onPurchaseRequest()
                        }
                    )
                )
            } else if (!isAiUser) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_ads_removed)) },
                    supportingContent = { Text(stringResource(R.string.settings_ads_removed_description)) },
                    leadingContent = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_restore_purchases)) },
                supportingContent = { Text(stringResource(R.string.settings_restore_purchases_description)) },
                leadingContent = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                modifier = Modifier.selectable(selected = false, onClick = { viewModel.refreshPurchases() })
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.preferences),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.manage_garage)) },
                supportingContent = { Text(stringResource(R.string.manage_garage_description)) },
                leadingContent = { Icon(Icons.Rounded.DirectionsCar, contentDescription = null) },
                modifier = Modifier.selectable(selected = false, onClick = onNavigateToGarage)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.distance_unit),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            
            UnitOption(
                text = stringResource(R.string.kilometers_km),
                selected = distanceUnit == DistanceUnit.KM,
                onClick = { viewModel.setDistanceUnit(DistanceUnit.KM) }
            )
            
            UnitOption(
                text = stringResource(R.string.miles_mi),
                selected = distanceUnit == DistanceUnit.MILES,
                onClick = { viewModel.setDistanceUnit(DistanceUnit.MILES) }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.consumption_unit),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            ConsumptionUnit.entries.forEach { unit ->
                UnitOption(
                    text = stringResource(
                        when (unit) {
                            ConsumptionUnit.KM_L -> R.string.consumption_km_l
                            ConsumptionUnit.L_100KM -> R.string.consumption_l_100km
                            ConsumptionUnit.MPG -> R.string.consumption_mpg
                        }
                    ),
                    selected = consumptionUnit == unit,
                    onClick = { viewModel.setConsumptionUnit(unit) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun UnitOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // handled by row selectable
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

