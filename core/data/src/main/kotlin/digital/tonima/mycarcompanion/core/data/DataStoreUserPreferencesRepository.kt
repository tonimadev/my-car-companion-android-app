package digital.tonima.mycarcompanion.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
    }

    override val distanceUnit: Flow<DistanceUnit> = dataStore.data.map { preferences ->
        val unitName = preferences[PreferencesKeys.DISTANCE_UNIT] ?: DistanceUnit.KM.name
        DistanceUnit.valueOf(unitName)
    }

    override suspend fun setDistanceUnit(distanceUnit: DistanceUnit) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISTANCE_UNIT] = distanceUnit.name
        }
    }
}
