package digital.tonima.mycarcompanion.core.billing.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import digital.tonima.mycarcompanion.core.billing.BillingManager
import digital.tonima.mycarcompanion.core.billing.BillingManagerImpl
import digital.tonima.mycarcompanion.core.billing.SubscriptionManager
import digital.tonima.mycarcompanion.core.billing.SubscriptionManagerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindBillingManager(
        billingManagerImpl: BillingManagerImpl
    ): BillingManager

    @Binds
    @Singleton
    abstract fun bindSubscriptionManager(
        subscriptionManagerImpl: SubscriptionManagerImpl
    ): SubscriptionManager
}
