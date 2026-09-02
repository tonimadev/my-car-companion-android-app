package digital.tonima.mycarcompanion.core.designsystem.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Instant

fun Instant.formatToShortDate(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(this.toEpochMilliseconds()))
}
