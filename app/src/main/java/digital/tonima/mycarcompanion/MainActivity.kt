package digital.tonima.mycarcompanion

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.designsystem.MyCarCompanionTheme
import digital.tonima.mycarcompanion.feature.home.HomeRoute
import digital.tonima.mycarcompanion.feature.home.onboarding.OnboardingRoute
import digital.tonima.mycarcompanion.feature.parts.PartsScreen
import digital.tonima.mycarcompanion.feature.parts.PartsViewModel
import digital.tonima.mycarcompanion.feature.tracking.ui.AddFuelRecordScreen
import digital.tonima.mycarcompanion.feature.tracking.ui.FuelHistoryScreen
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
sealed interface Route : NavKey {
    @Serializable data object Onboarding : Route
    @Serializable data object Home : Route
    @Serializable data object Garage : Route
    @Serializable data object Settings : Route
    @Serializable data class Parts(val vehicleId: Long) : Route
    @Serializable data object FuelHistory : Route
    @Serializable data object MaintenanceHistory : Route
    @Serializable data class AddFuel(val recordId: Long? = null) : Route
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyCarCompanionTheme {
                NotificationPermissionEffect()
                
                val isOnboardingCompleted by userPreferencesRepository.isOnboardingCompleted
                    .collectAsStateWithLifecycle(initialValue = null)
                val distanceUnit by userPreferencesRepository.distanceUnit
                    .collectAsStateWithLifecycle(initialValue = digital.tonima.mycarcompanion.core.model.DistanceUnit.KM)

                isOnboardingCompleted?.let { completed ->
                    AppNavigation(
                        isOnboardingCompleted = completed,
                        distanceUnit = distanceUnit,
                        onFinish = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        LaunchedEffect(Unit) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
    }
}

@Composable
fun AppNavigation(
    isOnboardingCompleted: Boolean,
    distanceUnit: digital.tonima.mycarcompanion.core.model.DistanceUnit,
    onFinish: () -> Unit
) {
    val startRoute = if (isOnboardingCompleted) Route.Home else Route.Onboarding
    val backStack = remember { mutableStateListOf(startRoute) }
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    DisposableEffect(context) {
        val listener = Consumer<Intent> { intent ->
            if (isOnboardingCompleted) {
                intent.getStringExtra("shortcut_route")?.let { route ->
                    when (route) {
                        "add_fuel" -> if (backStack.last() !is Route.AddFuel) backStack.add(Route.AddFuel())
                        "garage" -> if (backStack.last() != Route.Garage) backStack.add(Route.Garage)
                    }
                    intent.removeExtra("shortcut_route")
                }
            }
        }
        activity?.addOnNewIntentListener(listener)
        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }

    // Handle initial shortcut
    LaunchedEffect(Unit) {
        if (isOnboardingCompleted) {
            activity?.intent?.getStringExtra("shortcut_route")?.let { route ->
                when (route) {
                    "add_fuel" -> if (backStack.last() !is Route.AddFuel) backStack.add(Route.AddFuel())
                    "garage" -> if (backStack.last() != Route.Garage) backStack.add(Route.Garage)
                }
                activity.intent.removeExtra("shortcut_route")
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            } else {
                onFinish()
            }
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        )
    ) { key ->
        NavEntry(key) {
            when (key) {
                Route.Onboarding -> OnboardingRoute(
                    onOnboardingFinished = {
                        backStack.clear()
                        backStack.add(Route.Home)
                    }
                )
                Route.Home -> HomeRoute(
                    onNavigateToSettings = { backStack.add(Route.Settings) },
                    onNavigateToFuel = { backStack.add(Route.FuelHistory) },
                    onNavigateToMaintenanceHistory = { backStack.add(Route.MaintenanceHistory) },
                    adUnitId = BuildConfig.ADMOB_BANNER_HOME_ID
                )
                Route.Garage -> GarageAdaptiveScreen(
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        } else {
                            onFinish()
                        }
                    },
                    adUnitId = BuildConfig.ADMOB_BANNER_GARAGE_ID,
                    partsAdUnitId = BuildConfig.ADMOB_BANNER_PARTS_ID
                )
                Route.Settings -> digital.tonima.mycarcompanion.feature.home.SettingsScreen(
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        } else {
                            onFinish()
                        }
                    },
                    onNavigateToGarage = { backStack.add(Route.Garage) }
                )
                is Route.Parts -> PartsScreen(
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        } else {
                            onFinish()
                        }
                    },
                    adUnitId = BuildConfig.ADMOB_BANNER_PARTS_ID,
                    viewModel = hiltViewModel<PartsViewModel, PartsViewModel.Factory>(
                        creationCallback = { factory -> factory.create(key.vehicleId) }
                    )
                )
                Route.FuelHistory -> FuelHistoryScreen(
                    onNavigateUp = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        } else {
                            onFinish()
                        }
                    },
                    onNavigateToAddFuel = { recordId -> backStack.add(Route.AddFuel(recordId)) },
                    adUnitId = BuildConfig.ADMOB_BANNER_FUEL_ID
                )
                Route.MaintenanceHistory -> digital.tonima.mycarcompanion.feature.home.MaintenanceHistoryScreen(
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        } else {
                            onFinish()
                        }
                    },
                    distanceUnit = distanceUnit
                )
                is Route.AddFuel -> AddFuelRecordScreen(
                    recordId = key.recordId,
                    onNavigateUp = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        } else {
                            onFinish()
                        }
                    }
                )
            }
        }
    }
}