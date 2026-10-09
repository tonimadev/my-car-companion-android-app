package digital.tonima.mycarcompanion.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import digital.tonima.mycarcompanion.core.data.StatePrice
import digital.tonima.mycarcompanion.core.designsystem.component.IsometricCard
import digital.tonima.mycarcompanion.core.designsystem.util.CurrencyUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Average gasoline and diesel prices for the user's state (or Brazil), with the date they were collected. */
@Composable
fun FuelPriceCard(fuelPrice: FuelPriceUi, modifier: Modifier = Modifier) {
    val collectedOn = remember(fuelPrice.collectedAt) { formatCollectionDate(fuelPrice.collectedAt) }
    val reference = fuelPrice.gasoline ?: fuelPrice.diesel

    IsometricCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.fuel_price_title),
                style = MaterialTheme.typography.titleSmall
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                fuelPrice.gasoline?.let { PriceColumn(stringResource(R.string.fuel_price_gasoline), it) }
                fuelPrice.diesel?.let { PriceColumn(stringResource(R.string.fuel_price_diesel), it) }
            }
            if (reference != null) {
                Text(
                    text = if (reference.isNationalAverage) {
                        stringResource(R.string.fuel_price_national_source, collectedOn)
                    } else {
                        stringResource(R.string.fuel_price_state_source, reference.state.uppercase(), collectedOn)
                    },
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun PriceColumn(label: String, price: StatePrice) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            text = CurrencyUtils.formatCurrency(price.value),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** "2026-10-09 16:34:49" -> the date in the user's short format; falls back to the raw text. */
internal fun formatCollectionDate(collectedAt: String): String = runCatching {
    LocalDate.parse(collectedAt.take(10)).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
}.getOrDefault(collectedAt)
