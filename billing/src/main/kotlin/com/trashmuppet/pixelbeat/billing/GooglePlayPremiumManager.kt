package com.trashmuppet.pixelbeat.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.trashmuppet.pixelbeat.premium.PremiumManager
import com.trashmuppet.pixelbeat.premium.PremiumState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google Play Billing implementation of [PremiumManager].
 *
 * One-time INAPP product: `pro_unlock` at £1.99.
 *
 * Offline cache (SharedPreferences) delivers the last known
 * entitlement on cold start before the async Play query completes.
 * [15_BILLING.md] requires the cache so the user is never locked
 * out of pro features they already paid for during network outage.
 *
 * No subscriptions, no accounts, no telemetry.
 */
class GooglePlayPremiumManager(
    private val context: Context,
    private val debugBypass: Boolean = false
) : PremiumManager, PurchasesUpdatedListener {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _entitlementState = MutableStateFlow<PremiumState>(readCache())
    override val entitlementState: StateFlow<PremiumState> = _entitlementState.asStateFlow()

    private val billingClient: BillingClient = if (debugBypass) {
        // Bypass: no real billing client needed in debug.
        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        BillingClient.newBuilder(context).setListener(this).build()
    } else {
        BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build()
    }

    private var productDetails: ProductDetails? = null

    override suspend fun refreshEntitlement() {
        if (debugBypass) {
            _entitlementState.value = PremiumState.Pro
            return
        }
        if (_entitlementState.value !is PremiumState.Pro) {
            _entitlementState.value = PremiumState.Pending
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                } else {
                    // Fall back to cached state on billing errors.
                    _entitlementState.value = readCache()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Reconnect on next refreshEntitlement call.
            }
        })
    }

    override suspend fun launchPurchaseFlow(activity: Any) {
        if (debugBypass) return
        require(activity is Activity) { "Expected an Android Activity, got ${activity::class.simpleName}" }

        // Ensure billing client is connected before launching the flow.
        suspendCancellableCoroutine<Unit> { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(Unit)
                    } else {
                        cont.resume(Unit) // proceed — launch will surface the error
                    }
                }
                override fun onBillingServiceDisconnected() {
                    cont.resume(Unit)
                }
            })
        }

        // Fetch product details if not cached.
        val details = productDetails ?: fetchProductDetails()
        productDetails = details

        val productDetailsParamsList = listOf(
            ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            if (purchases.any { it.products.contains(PRODUCT_ID) }) {
                updateCache(PremiumState.Pro)
                _entitlementState.value = PremiumState.Pro
            }
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val isPro = purchases.any { it.products.contains(PRODUCT_ID) }
                val newState = if (isPro) PremiumState.Pro else PremiumState.Free
                updateCache(newState)
                _entitlementState.value = newState
            } else {
                _entitlementState.value = readCache()
            }
        }
    }

    private fun fetchProductDetails(): ProductDetails {
        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryParams) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && details.isNotEmpty()) {
                productDetails = details.first()
            }
        }

        // Fallback — return whatever we have (null-safe caller handles it).
        return productDetails ?: error(
            "Product details for '$PRODUCT_ID' not available. " +
            "Ensure the product is configured in Google Play Console."
        )
    }

    // ------------------------------------------------------------------
    // Offline cache
    // ------------------------------------------------------------------

    private fun readCache(): PremiumState {
        if (debugBypass) return PremiumState.Pro
        val state = prefs.getString(KEY_STATE, "FREE") ?: "FREE"
        return if (state == "PRO") PremiumState.Pro else PremiumState.Free
    }

    private fun updateCache(state: PremiumState) {
        prefs.edit()
            .putString(KEY_STATE, if (state is PremiumState.Pro) "PRO" else "FREE")
            .putLong(KEY_LAST_PURCHASE_MS, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "billing_cache"
        private const val KEY_STATE = "premium_state"
        private const val KEY_LAST_PURCHASE_MS = "last_purchase_time_ms"
        private const val PRODUCT_ID = "pro_unlock"
    }
}
