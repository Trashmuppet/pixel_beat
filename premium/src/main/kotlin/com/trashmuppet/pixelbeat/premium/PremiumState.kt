package com.trashmuppet.pixelbeat.premium

/**
 * Entitlement state for the one-time Pro Unlock.
 *
 * Per [15_BILLING.md]: no subscriptions, no telemetry, no accounts.
 * Project files never depend on entitlement state — this is a
 * runtime-only gate on UI affordances.
 */
sealed interface PremiumState {
    /** User has not purchased the Pro Unlock. */
    data object Free : PremiumState

    /** User owns the Pro Unlock — all features unlocked. */
    data object Pro : PremiumState

    /** Entitlement query is in flight (offline cache miss or Play slow). */
    data object Pending : PremiumState

    /** Billing service is unavailable or errored. */
    data class Error(val cause: Throwable) : PremiumState
}
