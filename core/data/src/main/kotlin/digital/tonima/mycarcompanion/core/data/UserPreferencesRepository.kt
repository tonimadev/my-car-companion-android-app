package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.model.ConsumptionUnit
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val distanceUnit: Flow<DistanceUnit>
    suspend fun setDistanceUnit(distanceUnit: DistanceUnit)

    val consumptionUnit: Flow<ConsumptionUnit>
    suspend fun setConsumptionUnit(consumptionUnit: ConsumptionUnit)
    
    val isOnboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)

    val isProUser: Flow<Boolean>
    suspend fun setProUser(isPro: Boolean)

    val isAiUser: Flow<Boolean>
    suspend fun setAiUser(isAi: Boolean)

    /** Brazilian state (UF, lower case) chosen for fuel prices, or null to use the national average. */
    val selectedState: Flow<String?>
    suspend fun setSelectedState(state: String?)
}
