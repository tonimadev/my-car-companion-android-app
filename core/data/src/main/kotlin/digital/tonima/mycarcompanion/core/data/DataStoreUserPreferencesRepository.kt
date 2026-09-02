package digital.tonima.mycarcompanion.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import digital.tonima.mycarcompanion.core.model.ConsumptionUnit
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val CONSUMPTION_UNIT = stringPreferencesKey("consumption_unit")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val IS_PRO_USER = booleanPreferencesKey("is_pro_user")
        val IS_AI_USER = booleanPreferencesKey("is_ai_user")
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

    override val consumptionUnit: Flow<ConsumptionUnit> = dataStore.data.map { preferences ->
        val unitName = preferences[PreferencesKeys.CONSUMPTION_UNIT] ?: ConsumptionUnit.KM_L.name
        ConsumptionUnit.valueOf(unitName)
    }

    override suspend fun setConsumptionUnit(consumptionUnit: ConsumptionUnit) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONSUMPTION_UNIT] = consumptionUnit.name
        }
    }

    override val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = completed
        }
    }

    override val isProUser: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_PRO_USER] ?: false
    }

    override suspend fun setProUser(isPro: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_PRO_USER] = isPro
        }
    }

    override val isAiUser: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_AI_USER] ?: false
    }

    override suspend fun setAiUser(isAi: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_AI_USER] = isAi
        }
    }
}
