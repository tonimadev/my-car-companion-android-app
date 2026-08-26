package digital.tonima.mycarcompanion.feature.home

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.GpsFixed
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mycarcompanion.core.data.ProUserProvider
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.feature.tracking.service.MileageTrackingService
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

    val isProUser: StateFlow<Boolean> = proUserProvider.isProUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAiUser: StateFlow<Boolean> = proUserProvider.isAiUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch {
            userPreferencesRepository.setDistanceUnit(unit)
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
    val isProUser by viewModel.isProUser.collectAsStateWithLifecycle()
    val isAiUser by viewModel.isAiUser.collectAsStateWithLifecycle()
    var isTrackingEnabled by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            isTrackingEnabled = true
            MileageTrackingService.start(context)
        } else {
            isTrackingEnabled = false
        }
    }

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
                text = "Planos e Assinatura",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (!isAiUser) {
                ListItem(
                    headlineContent = { Text("Assinar Assistente IA") },
                    supportingContent = { Text("Diagnósticos com IA, predições avançadas e remoção total de anúncios.") },
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
                    headlineContent = { Text("Plano Pro IA Ativo") },
                    supportingContent = { Text("Sua assinatura do Assistente IA está ativa.") },
                    leadingContent = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                )
            }

            if (!isProUser) {
                ListItem(
                    headlineContent = { Text("Remover Anúncios") },
                    supportingContent = { Text("Compra única para desfrutar do aplicativo sem anúncios.") },
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
                    headlineContent = { Text("Remoção de Anúncios Ativa") },
                    supportingContent = { Text("Você possui a versão sem anúncios.") },
                    leadingContent = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                )
            }

            ListItem(
                headlineContent = { Text("Restaurar Compras") },
                supportingContent = { Text("Sincronizar compras e assinaturas com sua conta da Play Store.") },
                leadingContent = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                modifier = Modifier.selectable(selected = false, onClick = { viewModel.refreshPurchases() })
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Rastreamento e GPS",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { Text("Rastreamento Automático por GPS") },
                supportingContent = { 
                    Text("Atualiza o hodômetro em tempo real durante suas viagens usando GPS em primeiro plano.") 
                },
                leadingContent = { Icon(Icons.Rounded.GpsFixed, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = isTrackingEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                val permissionsToRequest = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                
                                val hasFineLocation = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasFineLocation) {
                                    isTrackingEnabled = true
                                    MileageTrackingService.start(context)
                                } else {
                                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                                }
                            } else {
                                isTrackingEnabled = false
                                MileageTrackingService.stop(context)
                            }
                        }
                    )
                }
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

