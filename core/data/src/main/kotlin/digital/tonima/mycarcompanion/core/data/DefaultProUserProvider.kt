package digital.tonima.mycarcompanion.core.data

import android.app.Activity

import digital.tonima.paywall.core.PayWallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultProUserProvider @Inject constructor(
    private val payWallManager: PayWallManager,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ProUserProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val initialProStatus = runBlocking {
        userPreferencesRepository.isProUser.first()
    }

    private val initialAiStatus = runBlocking {
        userPreferencesRepository.isAiUser.first()
    }

    override val isProUser: StateFlow<Boolean> = payWallManager.ownedProductIds
        .map { owned -> 
            owned.contains("remove_ads_premium") || owned.contains("month_subscription")
        }
        .stateIn(scope, SharingStarted.Eagerly, initialProStatus)

    override val isAiUser: StateFlow<Boolean> = payWallManager.ownedProductIds
        .map { owned ->
            owned.contains("month_subscription")
        }
        .stateIn(scope, SharingStarted.Eagerly, initialAiStatus)

    override fun launchPurchasePro(activity: Activity) {
        payWallManager.launchPurchase(activity, "remove_ads_premium")
    }

    override fun launchSubscribeAi(activity: Activity) {
        payWallManager.launchSubscription(activity, "month_subscription")
    }

    init {
        payWallManager.connect()

        // Persistir status pro (sem anúncios)
        isProUser.onEach { isPro ->
            userPreferencesRepository.setProUser(isPro)
        }.launchIn(scope)

        // Persistir status AI
        isAiUser.onEach { isAi ->
            userPreferencesRepository.setAiUser(isAi)
        }.launchIn(scope)
    }

    override fun refresh() {
        payWallManager.refresh()
    }
}
