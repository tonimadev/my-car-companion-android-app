package digital.tonima.mycarcompanion.core.designsystem.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Instant

class DateTimeFormatterExtTest {

    private lateinit var originalTimeZone: TimeZone

    // 2023-11-14T22:13:20Z
    private val instant = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `short date follows locale conventions`() {
        assertEquals("14/11/2023", instant.formatToShortDate(Locale.forLanguageTag("pt-BR")))
        assertEquals("11/14/23", instant.formatToShortDate(Locale.US))
        assertEquals("2023/11/14", instant.formatToShortDate(Locale.JAPAN))
    }

    @Test
    fun `short date time includes time in locale format`() {
        assertEquals("14/11/2023 22:13", instant.formatToShortDateTime(Locale.forLanguageTag("pt-BR")))
    }
}
