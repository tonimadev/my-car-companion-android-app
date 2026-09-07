package digital.tonima.mycarcompanion.feature.home.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.feature.home.R
import kotlinx.coroutines.launch

@Composable
fun OnboardingRoute(
    onOnboardingFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val distanceUnit by viewModel.distanceUnit.collectAsStateWithLifecycle()
    val isCompleting by viewModel.isCompleting.collectAsStateWithLifecycle()

    OnboardingScreen(
        distanceUnit = distanceUnit,
        isCompleting = isCompleting,
        onDistanceUnitChange = viewModel::setDistanceUnit,
        onCompleteOnboarding = { vehicleName, odometer ->
            viewModel.completeOnboarding(vehicleName, odometer, onOnboardingFinished)
        },
        modifier = modifier
    )
}

@Composable
internal fun OnboardingScreen(
    distanceUnit: DistanceUnit,
    isCompleting: Boolean,
    onDistanceUnitChange: (DistanceUnit) -> Unit,
    onCompleteOnboarding: (String?, Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    var vehicleName by remember { mutableStateOf("") }
    var initialOdometer by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    icon = Icons.Default.Build,
                    title = stringResource(R.string.onboarding_slide1_title),
                    description = stringResource(R.string.onboarding_slide1_desc),
                    currentPage = 0,
                    onNext = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    onSkip = { onCompleteOnboarding(null, null) }
                )
                1 -> OnboardingPage(
                    icon = Icons.Default.LocalGasStation,
                    title = stringResource(R.string.onboarding_slide2_title),
                    description = stringResource(R.string.onboarding_slide2_desc),
                    currentPage = 1,
                    onNext = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    onSkip = { onCompleteOnboarding(null, null) }
                )
                2 -> SetupPage(
                    distanceUnit = distanceUnit,
                    onDistanceUnitChange = onDistanceUnitChange,
                    vehicleName = vehicleName,
                    onVehicleNameChange = { vehicleName = it },
                    initialOdometer = initialOdometer,
                    onInitialOdometerChange = { initialOdometer = it },
                    isCompleting = isCompleting,
                    onComplete = {
                        val odo = initialOdometer.toDoubleOrNull()
                        onCompleteOnboarding(
                            vehicleName.ifBlank { null },
                            odo
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun OnboardingActions(
    currentPage: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit = {},
    isCompleting: Boolean = false,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Page Indicator
        if (!isLandscape) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                repeat(3) { iteration ->
                    val color = if (currentPage == iteration) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                    val width = if (currentPage == iteration) 24.dp else 8.dp
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPage < 2) {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.onboarding_skip))
                }

                Button(onClick = onNext) {
                    Text(stringResource(R.string.onboarding_next))
                }
            } else {
                Button(
                    onClick = onComplete,
                    enabled = !isCompleting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCompleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.onboarding_get_started))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    icon: ImageVector,
    title: String,
    description: String,
    currentPage: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(if (isLandscape) 16.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(if (isLandscape) 64.dp else 100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (isLandscape) 32.dp else 48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OnboardingActions(
            currentPage = currentPage,
            onNext = onNext,
            onSkip = onSkip
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupPage(
    distanceUnit: DistanceUnit,
    onDistanceUnitChange: (DistanceUnit) -> Unit,
    vehicleName: String,
    onVehicleNameChange: (String) -> Unit,
    initialOdometer: String,
    onInitialOdometerChange: (String) -> Unit,
    isCompleting: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(if (isLandscape) 16.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!isLandscape) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = stringResource(R.string.onboarding_slide3_title),
            style = if (isLandscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (!isLandscape) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.onboarding_slide3_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))

        // Distance Unit Selector
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = distanceUnit == DistanceUnit.KM,
                onClick = { onDistanceUnitChange(DistanceUnit.KM) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(stringResource(R.string.kilometers_km))
            }
            SegmentedButton(
                selected = distanceUnit == DistanceUnit.MILES,
                onClick = { onDistanceUnitChange(DistanceUnit.MILES) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(stringResource(R.string.miles_mi))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = vehicleName,
            onValueChange = onVehicleNameChange,
            label = { Text(stringResource(R.string.vehicle_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = initialOdometer,
            onValueChange = onInitialOdometerChange,
            label = { Text(stringResource(R.string.initial_odometer_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OnboardingActions(
            currentPage = 2,
            onNext = {},
            onSkip = {},
            onComplete = onComplete,
            isCompleting = isCompleting
        )
    }
}
