package digital.tonima.mycarcompanion

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.designsystem.MyCarCompanionTheme
import digital.tonima.mycarcompanion.feature.home.HomeRoute
import digital.tonima.mycarcompanion.feature.home.onboarding.OnboardingRoute
import digital.tonima.mycarcompanion.feature.parts.PartsScreen
import digital.tonima.mycarcompanion.feature.parts.PartsViewModel
import digital.tonima.mycarcompanion.feature.tracking.ui.AddFuelRecordScreen
import digital.tonima.mycarcompanion.feature.tracking.ui.FuelHistoryScreen
import dagger.hilt.android.AndroidEntryPoint
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
    @Serializable data object AddFuel : Route
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

                isOnboardingCompleted?.let { completed ->
                    AppNavigation(
                        isOnboardingCompleted = completed,
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
    onFinish: () -> Unit
) {
    val startRoute = if (isOnboardingCompleted) Route.Home else Route.Onboarding
    val backStack = remember { mutableStateListOf<Route>(startRoute) }
    
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
                    onNavigateToFuel = { backStack.add(Route.FuelHistory) }
                )
                Route.Garage -> GarageAdaptiveScreen(
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        } else {
                            onFinish()
                        }
                    }
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
                    onNavigateToAddFuel = { backStack.add(Route.AddFuel) }
                )
                Route.AddFuel -> AddFuelRecordScreen(
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