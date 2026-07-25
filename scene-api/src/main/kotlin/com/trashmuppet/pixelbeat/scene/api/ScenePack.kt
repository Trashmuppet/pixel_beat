package com.trashmuppet.pixelbeat.scene.api

/**
 * A bundled visualiser pack.
 *
 * Per [15_BILLING.md], core beat creation remains free — the bundled
 * Warehouse scene is always included. Additional scene packs are a
 * Pro feature gated through [com.trashmuppet.pixelbeat.premium.PremiumManager].
 */
data class ScenePack(
    val packId: String,
    val displayName: String,
    val description: String,
    /** Whether this pack requires the Pro Unlock. */
    val requiresPro: Boolean,
    /** Factory for the [Scene] instance. Pro packs throw [UnsupportedOperationException] until implemented. */
    val sceneFactory: () -> Scene
)
