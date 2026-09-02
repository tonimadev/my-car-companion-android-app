package digital.tonima.mycarcompanion.core.designsystem.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyUtils {
    fun getCurrencySymbol(locale: Locale = Locale.getDefault()): String {
        return try {
            val numberFormat = NumberFormat.getCurrencyInstance(locale)
            val currency = numberFormat.currency
            currency?.symbol ?: Currency.getInstance(locale).symbol
        } catch (_: Exception) {
            "$"
        }
    }

    fun formatCurrency(amount: Double, locale: Locale = Locale.getDefault()): String {
        return try {
            val numberFormat = NumberFormat.getCurrencyInstance(locale)
            numberFormat.format(amount)
        } catch (_: Exception) {
            "%.2f".format(amount)
        }
    }
}
