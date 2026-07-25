package com.trashmuppet.pixelbeat.billing

import android.content.Context
import com.trashmuppet.pixelbeat.billing.cache.EntitlementCache
import com.trashmuppet.pixelbeat.premium.PremiumManager
import com.trashmuppet.pixelbeat.premium.PremiumState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase-0 implementation of [PremiumManager] backed by an on-device
 * entitlement cache.
 *
 * `15_BILLING.md` is explicit: offline entitlements must work after a
 * successful purchase, and project files must never depend on
 * entitlement state. The cache is the authoritative source here; the
 * Play Billing Client only `refresh()`es it.
 *
 * The public surface of [PremiumManager] is fixed by `:premium`;
 * the consumer code (`feature-*`, `MainActivity`) only sees
 *   - `entitlementState: StateFlow<PremiumState>`
 *   - `suspend refreshEntitlement()`
 *   - `suspend launchPurchaseFlow(activity: Any)`
 * so swapping this stub for [`GooglePlayPremiumManager`] is a
 * constructor-line change with zero downstream impact.
 */
class SkeletonBillingService(
    context: Context,
    private val cache: EntitlementCache = EntitlementCache(context)
) : PremiumManager {

    private val _state: MutableStateFlow<PremiumState> = MutableStateFlow(loadInitial())

    override val entitlementState: StateFlow<PremiumState> = _state.asStateFlow()

    override suspend fun refreshEntitlement() {
        // Phase 5 hook: query BillingClient.queryPurchasesAsync and pipe
        // its result through `cache.markPro()`. For Phase 0 the cache
        // alone is authoritative — the StateFlow already reflects what
        // was persisted in SharedPreferences at `loadInitial()` time,
        // so this is a deliberate refresh-confirmation no-op.
    }

    override suspend fun launchPurchaseFlow(activity: Any) {
        // Phase 5+ hook: defer to `GooglePlayPremiumManager.launchBillingFlow`
        // once the Play Billing integration is wired. For Phase 0 we
        // simply honour the offline cache so a previously-purchased
        // entitlement remains recognised without a Play round-trip.
        _state.value = if (cache.isPro()) PremiumState.Pro else PremiumState.Free
    }

    private fun loadInitial(): PremiumState =
        if (cache.isPro()) PremiumState.Pro else PremiumState.Free
}
