package digital.tonima.mycarcompanion.core.designsystem.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.TimeZone
import kotlin.time.Instant

class DateTimeFormatterExtTest {

    private lateinit var originalTimeZone: TimeZone

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
    fun `formats instant as day month year`() {
        // 2023-11-14T22:13:20Z
        assertEquals("14/11/2023", Instant.fromEpochMilliseconds(1_700_000_000_000L).formatToShortDate())
    }
}
