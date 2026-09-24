package digital.tonima.mycarcompanion.feature.home

import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class DateTimeFormatterExtTest {

    private lateinit var originalTimeZone: TimeZone
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        originalLocale = Locale.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `formats instant as short month day and year`() {
        // 2023-11-14T22:13:20Z
        assertEquals("Nov 14, 2023", Instant.fromEpochMilliseconds(1_700_000_000_000L).formatToShortDate())
    }
}
