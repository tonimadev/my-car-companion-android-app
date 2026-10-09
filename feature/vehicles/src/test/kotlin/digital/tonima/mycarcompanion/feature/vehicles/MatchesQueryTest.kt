package digital.tonima.mycarcompanion.feature.vehicles

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchesQueryTest {

    private val prisma = "PRISMA Sed. LT 1.4 8V FlexPower 4p"

    @Test
    fun `every typed word must appear in any order and ignoring case`() {
        assertTrue(matchesQuery(prisma, "prisma LT 1.4"))
        assertTrue(matchesQuery(prisma, "1.4 prisma"))
        assertTrue(matchesQuery(prisma, "  flexpower  "))
    }

    @Test
    fun `a missing word rejects the option`() {
        assertFalse(matchesQuery(prisma, "prisma LTZ"))
        assertFalse(matchesQuery(prisma, "onix"))
    }

    @Test
    fun `an empty query matches everything`() {
        assertTrue(matchesQuery(prisma, ""))
        assertTrue(matchesQuery(prisma, "   "))
    }
}
