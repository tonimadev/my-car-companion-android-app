package digital.tonima.mycarcompanion.core.designsystem.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Instant

/** Formats the date in the short style of [locale] (e.g. 11/14/23 in the US, 14/11/2023 in Brazil). */
fun Instant.formatToShortDate(locale: Locale = Locale.getDefault()): String =
    DateFormat.getDateInstance(DateFormat.SHORT, locale).format(Date(toEpochMilliseconds()))

/** Formats date and time in the short style of [locale]. */
fun Instant.formatToShortDateTime(locale: Locale = Locale.getDefault()): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(Date(toEpochMilliseconds()))
