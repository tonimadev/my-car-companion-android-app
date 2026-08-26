package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.model.DistanceUnit
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val distanceUnit: Flow<DistanceUnit>
    suspend fun setDistanceUnit(distanceUnit: DistanceUnit)
    
    val isOnboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)

    val isProUser: Flow<Boolean>
    suspend fun setProUser(isPro: Boolean)

    val isAiUser: Flow<Boolean>
    suspend fun setAiUser(isAi: Boolean)
}
