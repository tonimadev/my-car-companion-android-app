package digital.tonima.mycarcompanion.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import digital.tonima.mycarcompanion.core.model.ConsumptionUnit
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreUserPreferencesRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.repository() = DataStoreUserPreferencesRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            tmp.newFile("prefs.preferences_pb").also { it.delete() }
        }
    )

    @Test
    fun `defaults are returned when nothing is stored`() = runTest {
        val repository = repository()

        assertEquals(DistanceUnit.KM, repository.distanceUnit.first())
        assertEquals(ConsumptionUnit.KM_L, repository.consumptionUnit.first())
        assertFalse(repository.isOnboardingCompleted.first())
        assertFalse(repository.isProUser.first())
        assertFalse(repository.isAiUser.first())
    }

    @Test
    fun `stored values are read back`() = runTest {
        val repository = repository()

        repository.setDistanceUnit(DistanceUnit.MILES)
        repository.setConsumptionUnit(ConsumptionUnit.MPG)
        repository.setOnboardingCompleted(true)
        repository.setProUser(true)
        repository.setAiUser(true)

        assertEquals(DistanceUnit.MILES, repository.distanceUnit.first())
        assertEquals(ConsumptionUnit.MPG, repository.consumptionUnit.first())
        assertTrue(repository.isOnboardingCompleted.first())
        assertTrue(repository.isProUser.first())
        assertTrue(repository.isAiUser.first())
    }
}
