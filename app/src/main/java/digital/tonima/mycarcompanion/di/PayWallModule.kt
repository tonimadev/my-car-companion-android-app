package digital.tonima.mycarcompanion.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import digital.tonima.paywall.core.PayWallConfig
import digital.tonima.paywall.core.PayWallManager
import digital.tonima.paywall.play.PayWallManagerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PayWallModule {

    @Provides
    @Singleton
    fun providePayWallConfig(): PayWallConfig {
        return PayWallConfig(
            inAppProductIds = setOf("remove_ads_premium"),
            subscriptionProductIds = setOf("month_subscription")
        )
    }

    @Provides
    @Singleton
    fun providePayWallManager(
        @ApplicationContext context: Context,
        config: PayWallConfig
    ): PayWallManager {
        return PayWallManagerImpl(context, config)
    }
}
