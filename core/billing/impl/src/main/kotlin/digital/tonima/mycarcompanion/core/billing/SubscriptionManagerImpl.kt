package digital.tonima.mycarcompanion.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SubscriptionManager"
private const val MONTHLY_SUBSCRIPTION_PLAN = "month_subscription"

@Singleton
class SubscriptionManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SubscriptionManager {

    private val _isProUser = MutableStateFlow(false)
    override val isProUser = _isProUser.asStateFlow()

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }

    private var billingClient: BillingClient =
        BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

    override fun connect() {
        connectInternal(null)
    }

    private fun connectInternal(onConnected: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            onConnected?.invoke()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == OK) {
                    Log.d(TAG, "Subscription client setup finished.")
                    queryPurchases()
                    onConnected?.invoke()
                } else {
                    Log.e(TAG, "Subscription client setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Subscription service disconnected.")
            }
        })
    }

    private fun queryPurchases() {
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { billingResult, subsPurchases ->
            if (billingResult.responseCode == OK) {
                for (purchase in subsPurchases) {
                    handlePurchase(purchase)
                }
            }

            val hasSubsPremium = subsPurchases.any {
                it.products.contains(MONTHLY_SUBSCRIPTION_PLAN) &&
                        (it.isAcknowledged || it.purchaseState == PURCHASED)
            }
            _isProUser.value = hasSubsPremium
        }
    }

    override fun refresh() {
        queryPurchases()
    }

    override fun launchSubscriptionFlow(activity: Activity) {
        if (!billingClient.isReady) {
            Log.e(TAG, "Subscription client not ready. Attempting to reconnect.")
            connectInternal {
                launchSubscriptionFlow(activity)
            }
            return
        }

        val subsProduct = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(MONTHLY_SUBSCRIPTION_PLAN)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(subsProduct))
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { billingResult, queryProductDetailsResult ->
            val productDetailsList = queryProductDetailsResult.productDetailsList
            Log.d(
                TAG,
                "Subscription query finished. Response: ${billingResult.responseCode}, Details list size: ${productDetailsList.size}"
            )

            if (billingResult.responseCode == OK && !productDetailsList.isNullOrEmpty()) {
                val productDetails = productDetailsList[0]
                launchBillingFlow(activity, productDetails)
            } else {
                Log.e(
                    TAG,
                    "Subscription product details not found or error. ResponseCode: ${billingResult.responseCode}, DebugMsg: ${billingResult.debugMessage}"
                )
                Toast.makeText(
                    context,
                    "Assinatura não encontrada na loja. Verifique sua conexão ou conta de teste.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun launchBillingFlow(
        activity: Activity,
        productDetails: com.android.billingclient.api.ProductDetails
    ) {
        val offerToken = productDetails.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == "month_subscription" || it.basePlanId == "monthly-basic-plan" }
            ?.offerToken
            ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken

        if (offerToken == null) {
            Log.e(TAG, "No offer token found for product: ${productDetails.productId}")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == OK) {
                    Log.d(TAG, "Subscription acknowledged successfully. Updating pro status.")
                    _isProUser.value = true
                }
            }
        }
    }
}
