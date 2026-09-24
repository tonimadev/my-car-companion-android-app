package digital.tonima.mycarcompanion.core.designsystem.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class NumberUtilsTest {

    private val ptBR = Locale.forLanguageTag("pt-BR")
    private val us = Locale.US
    private val de = Locale.GERMANY

    @Test
    fun `formatDecimal uses locale separators and fixed decimals`() {
        assertEquals("1.234,50", NumberUtils.formatDecimal(1234.5, 2, ptBR))
        assertEquals("1,234.50", NumberUtils.formatDecimal(1234.5, 2, us))
        assertEquals("12,3", NumberUtils.formatDecimal(12.34, 1, de))
    }

    @Test
    fun `formatDecimalInput has no grouping and trims trailing zeros`() {
        assertEquals("1234,5", NumberUtils.formatDecimalInput(1234.5, locale = ptBR))
        assertEquals("1234.5", NumberUtils.formatDecimalInput(1234.5, locale = us))
        assertEquals("40", NumberUtils.formatDecimalInput(40.0, locale = us))
        assertEquals("5,79", NumberUtils.formatDecimalInput(5.7859, locale = ptBR))
    }

    @Test
    fun `parseDecimal honors locale decimal and grouping separators`() {
        assertEquals(1234.5, NumberUtils.parseDecimal("1.234,5", ptBR)!!, 0.0)
        assertEquals(1234.5, NumberUtils.parseDecimal("1,234.5", us)!!, 0.0)
        assertEquals(5.79, NumberUtils.parseDecimal("5,79", de)!!, 0.0)
    }

    @Test
    fun `parseDecimal accepts the other separator as decimal when typed once`() {
        assertEquals(5.79, NumberUtils.parseDecimal("5.79", ptBR)!!, 0.0)
        assertEquals(5.79, NumberUtils.parseDecimal("5,79", us)!!, 0.0)
    }

    @Test
    fun `parseDecimal treats repeated other separator as grouping`() {
        assertEquals(1234567.0, NumberUtils.parseDecimal("1.234.567", ptBR)!!, 0.0)
    }

    @Test
    fun `parseDecimal handles whitespace, integers and invalid input`() {
        assertEquals(40.0, NumberUtils.parseDecimal(" 40 ", us)!!, 0.0)
        assertNull(NumberUtils.parseDecimal("", us))
        assertNull(NumberUtils.parseDecimal("abc", us))
    }
}
