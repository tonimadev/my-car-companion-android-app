package digital.tonima.mycarcompanion.core.designsystem.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class CurrencyUtilsTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `currency symbol follows locale`() {
        assertEquals("$", CurrencyUtils.getCurrencySymbol(Locale.US))
        assertEquals("R$", CurrencyUtils.getCurrencySymbol(Locale.forLanguageTag("pt-BR")))
        assertEquals("€", CurrencyUtils.getCurrencySymbol(Locale.GERMANY))
    }

    @Test
    fun `currency symbol does not depend on default locale`() {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"))
        assertEquals("$", CurrencyUtils.getCurrencySymbol(Locale.US))
    }

    @Test
    fun `formats amount as currency for locale`() {
        assertEquals("$1,234.50", CurrencyUtils.formatCurrency(1234.5, Locale.US))
    }
}
