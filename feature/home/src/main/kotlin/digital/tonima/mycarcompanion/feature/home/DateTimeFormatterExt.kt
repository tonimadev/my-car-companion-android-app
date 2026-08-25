package digital.tonima.mycarcompanion.feature.home

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter

fun Instant.formatToShortDate(): String {
    val localDateTime = this.toLocalDateTime(TimeZone.currentSystemDefault())
    val javaLocalDateTime = java.time.LocalDateTime.of(
        localDateTime.year,
        localDateTime.monthNumber,
        localDateTime.dayOfMonth,
        localDateTime.hour,
        localDateTime.minute,
        localDateTime.second,
        localDateTime.nanosecond
    )
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    return javaLocalDateTime.format(formatter)
}
