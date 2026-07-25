package com.trashmuppet.pixelbeat.billing

import android.content.Context
import com.trashmuppet.pixelbeat.billing.cache.EntitlementCache
import com.trashmuppet.pixelbeat.premium.PremiumManager
import com.trashmuppet.pixelbeat.premium.ProState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase-0 implementation of `PremiumManager` backed by an on-device
 * entitlement cache.
 *
 * `15_BILLING.md` is explicit: offline entitlements must work after a
 * successful purchase, and project files must never depend on
 * entitlement state. The cache is the authoritative source here; the
 * Play Billing Client only `refresh()`es it.
 *
 * Replace the inner `Mut` state with a real BillingClient integration
 * once a Play service account is wired up. The public surface for the
 * rest of the app does not change.
 */
class SkeletonBillingService(
    context: Context,
    private val cache: EntitlementCache = EntitlementCache(context)
) : PremiumManager {

    private val state = MutableStateFlow(loadInitial())

    override fun observe(): Flow<ProState> = state.asStateFlow()

    override fun isPro(): Boolean = state.value == ProState.Pro

    override suspend fun refresh() {
        // Phase 5 hook: query BillingClient.queryPurchasesAsync and pipe
        // its result through `cache.markPro()`. For Phase 0 the cache
        // alone is authoritative.
    }

    private fun loadInitial(): ProState =
        if (cache.isPro()) ProState.Pro else ProState.Free
}
