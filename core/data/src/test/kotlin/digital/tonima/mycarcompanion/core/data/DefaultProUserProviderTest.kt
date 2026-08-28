package digital.tonima.mycarcompanion.core.data

import digital.tonima.paywall.core.PayWallManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultProUserProviderTest {

    private val testDispatcher = StandardTestDispatcher()
    private val payWallManager = mockk<PayWallManager>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    
    private lateinit var proUserProvider: DefaultProUserProvider

    private val ownedProductsFlow = MutableStateFlow<Set<String>>(emptySet())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { payWallManager.ownedProductIds } returns ownedProductsFlow
        every { userPreferencesRepository.isProUser } returns MutableStateFlow(false)
        every { userPreferencesRepository.isAiUser } returns MutableStateFlow(false)
        
        proUserProvider = DefaultProUserProvider(payWallManager, userPreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isProUser is true when product is in ownedProductIds`() = runTest {
        assertFalse(proUserProvider.isProUser.value)
        
        ownedProductsFlow.value = setOf("remove_ads_premium")
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(proUserProvider.isProUser.value)
    }

    @Test
    fun `isAiUser is true when subscription is in ownedProductIds`() = runTest {
        assertFalse(proUserProvider.isAiUser.value)
        
        ownedProductsFlow.value = setOf("month_subscription")
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(proUserProvider.isAiUser.value)
        // Subscription also gives Pro status
        assertTrue(proUserProvider.isProUser.value)
    }

    @Test
    fun `launchPurchasePro calls paywallManager`() {
        val activity = mockk<android.app.Activity>()
        proUserProvider.launchPurchasePro(activity)
        verify { payWallManager.launchPurchase(activity, "remove_ads_premium") }
    }
}
