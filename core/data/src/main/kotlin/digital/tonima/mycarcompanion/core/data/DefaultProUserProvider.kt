package digital.tonima.mycarcompanion.core.data

import android.app.Activity
import digital.tonima.mycarcompanion.core.billing.BillingManager
import digital.tonima.mycarcompanion.core.billing.SubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultProUserProvider @Inject constructor(
    private val billingManager: BillingManager,
    private val subscriptionManager: SubscriptionManager,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ProUserProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val initialProStatus = runBlocking {
        userPreferencesRepository.isProUser.first()
    }

    private val initialAiStatus = runBlocking {
        userPreferencesRepository.isAiUser.first()
    }

    override val isProUser: StateFlow<Boolean> = combine(
        billingManager.isProUser,
        subscriptionManager.isProUser,
    ) { inApp, sub -> inApp || sub }
        .stateIn(scope, SharingStarted.Eagerly, initialProStatus)

    override val isAiUser: StateFlow<Boolean> = subscriptionManager.isProUser
        .stateIn(scope, SharingStarted.Eagerly, initialAiStatus)

    override fun launchPurchasePro(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }

    override fun launchSubscribeAi(activity: Activity) {
        subscriptionManager.launchSubscriptionFlow(activity)
    }

    init {
        billingManager.connect()
        subscriptionManager.connect()

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
        billingManager.refresh()
        subscriptionManager.refresh()
    }
}
