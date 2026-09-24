package digital.tonima.mycarcompanion.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Instant

class InstantConverterTest {

    private val converter = InstantConverter()

    @Test
    fun `converts timestamp to instant and back`() {
        val millis = 1_700_000_000_123L
        val instant = converter.fromTimestamp(millis)
        assertEquals(Instant.fromEpochMilliseconds(millis), instant)
        assertEquals(millis, converter.dateToTimestamp(instant))
    }

    @Test
    fun `null values are preserved`() {
        assertNull(converter.fromTimestamp(null))
        assertNull(converter.dateToTimestamp(null))
    }
}
