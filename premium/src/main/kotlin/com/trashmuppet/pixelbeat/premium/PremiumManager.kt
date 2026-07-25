package com.trashmuppet.pixelbeat.premium

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The single source of truth for "is this user Pro?" that UI features
 * depend on. Per `15_BILLING.md`:
 *  - Project files MUST never depend on entitlement state.
 *  - Core beat creation remains free.
 *  - The offline entitlement cache is honoured here.
 *
 * Features query via `observe()`; the implementation in `:billing`
 * hydrates from Play Billing + the on-device cache.
 */
interface PremiumManager {

    fun observe(): Flow<ProState>

    /** Synchronous "do they have pro right now" — used by export gating. */
    fun isPro(): Boolean

    /** Force-rehydrate from Play after a successful purchase. */
    suspend fun refresh()
}

enum class ProState {
    /** Core beat creation is always free, so non-Pro is a valid state. */
    Free,

    /** Pro is unlocked via the £1.99 one-time purchase. */
    Pro
}

/**
 * Default Phase 0 implementation — Free. The real `PremiumManager`
 * (Play Billing + offline entitlement cache) lands in Phase 0's billing
 * stage.
 */
class FreePremiumManager : PremiumManager {
    override fun observe(): Flow<ProState> = flowOf(ProState.Free)
    override fun isPro(): Boolean = false
    override suspend fun refresh() = Unit
}
