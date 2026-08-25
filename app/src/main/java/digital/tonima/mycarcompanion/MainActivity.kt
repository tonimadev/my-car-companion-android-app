package digital.tonima.mycarcompanion

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import digital.tonima.mycarcompanion.core.designsystem.MyCarCompanionTheme
import digital.tonima.mycarcompanion.feature.home.HomeRoute
import digital.tonima.mycarcompanion.feature.parts.PartsScreen
import digital.tonima.mycarcompanion.feature.parts.PartsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable data object Home : Route
    @Serializable data object Garage : Route
    @Serializable data object Settings : Route
    @Serializable data class Parts(val vehicleId: Long) : Route
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyCarCompanionTheme {
                NotificationPermissionEffect()
                AppNavigation(onFinish = { finish() })
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
fun AppNavigation(onFinish: () -> Unit) {
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    
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
                Route.Home -> HomeRoute(
                    onNavigateToSettings = { backStack.add(Route.Settings) }
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
            }
        }
    }
}
