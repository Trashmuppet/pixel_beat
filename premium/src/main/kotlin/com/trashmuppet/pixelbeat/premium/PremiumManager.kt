package com.trashmuppet.pixelbeat.premium

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over the billing backend.
 *
 * Feature modules depend on this interface, never on the Google Play
 * Billing implementation in [:billing]. Per [15_BILLING.md]:
 *
 * ```
 * feature modules → PremiumManager → billing module → Google Play Billing
 * ```
 *
 * [entitlementState] emits the current entitlement. On cold start it
 * returns the cached value immediately (FAST path), then
 * [refreshEntitlement] updates the cache from Play in the background.
 */
interface PremiumManager {
    /** Cold-start cached value, updated asynchronously by [refreshEntitlement]. */
    val entitlementState: StateFlow<PremiumState>

    /**
     * Query the billing backend for the latest entitlement.
     * Implementations should:
     *  1. Set [entitlementState] to [PremiumState.Pending] if not Pro.
     *  2. Query Play (or equivalent).
     *  3. Update the offline cache and [entitlementState].
     *  4. Fall back to the cached value on network / service errors.
     */
    suspend fun refreshEntitlement()
}
