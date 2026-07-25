package com.trashmuppet.pixelbeat.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.trashmuppet.pixelbeat.premium.PremiumManager
import com.trashmuppet.pixelbeat.premium.PremiumState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private val context: Context
) : PremiumManager, PurchasesUpdatedListener {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _entitlementState = MutableStateFlow<PremiumState>(readCache())
    override val entitlementState: StateFlow<PremiumState> = _entitlementState.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    override suspend fun refreshEntitlement() {
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

    // ------------------------------------------------------------------
    // Offline cache
    // ------------------------------------------------------------------

    private fun readCache(): PremiumState {
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
