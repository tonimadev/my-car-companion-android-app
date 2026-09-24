package digital.tonima.mycarcompanion.core.designsystem.util

import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

object NumberUtils {

    /** Formats [value] for display, with grouping and exactly [fractionDigits] decimals, using [locale]. */
    fun formatDecimal(value: Double, fractionDigits: Int, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }.format(value)

    /** Formats [value] to prefill a text field: no grouping, up to [maxFractionDigits] decimals, using [locale]. */
    fun formatDecimalInput(value: Double, maxFractionDigits: Int = 2, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = false
            minimumFractionDigits = 0
            maximumFractionDigits = maxFractionDigits
        }.format(value)

    /**
     * Parses a number typed by the user. The locale decimal separator is honored, and when it is
     * absent a single `.` or `,` is also accepted as decimal separator, since keyboards do not always
     * offer the locale one.
     */
    fun parseDecimal(text: String, locale: Locale = Locale.getDefault()): Double? {
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val decimal = symbols.decimalSeparator
        val grouping = symbols.groupingSeparator
        var normalized = text.trim().filterNot { it.isWhitespace() || it == ' ' || it == ' ' }
        if (normalized.isEmpty()) return null

        normalized = if (normalized.contains(decimal)) {
            normalized.replace(grouping.toString(), "").replace(decimal, '.')
        } else {
            val other = if (decimal == ',') '.' else ','
            if (normalized.count { it == other } == 1) {
                normalized.replace(other, '.')
            } else {
                normalized.replace(other.toString(), "")
            }
        }
        return normalized.toDoubleOrNull()
    }
}
