package com.trashmuppet.pixelbeat.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stable deterministic seed for a project.
 *
 * Used for every random choice downstream — sample-accurate scheduling,
 * scene procedural variation, mixer dithering. Same `ProjectSeed` →
 * identical output across machines and rebuilds.
 *
 * Per `01_PRODUCT_PILLARS.md` ("Deterministic") and ADR-001 the seed is
 * part of the source-of-truth document, never derived at runtime.
 */
@JvmInline
@Serializable
value class ProjectSeed(val value: Long)

/**
 * How off-beat swing is applied to odd-numbered steps in a pattern.
 *
 * `granularity` decides which steps feel pushed; `amount` is the push
 * fraction in samples relative to the nominal step offset, clamped
 * to [0.0, 1.0]. NONE is the identity.
 */
@Serializable
data class SwingMode(
    val granularity: Granularity = Granularity.NONE,
    val amount: Float = 0f
) {
    @Serializable
    enum class Granularity {
        @SerialName("none") NONE,
        @SerialName("eighth") EIGHTH,
        @SerialName("sixteenth") SIXTEENTH
    }

    init {
        require(amount in 0f..1f) { "swing amount $amount outside [0,1]" }
    }
}

/**
 * Six drum voices for V1 (`08_AUDIO_ENGINE.md`) plus an `OTHER` catch-all
 * for non-percussive tracks in future phases.
 */
@Serializable
enum class DrumKind {
    @SerialName("kick") KICK,
    @SerialName("snare") SNARE,
    @SerialName("closed_hat") CLOSED_HAT,
    @SerialName("open_hat") OPEN_HAT,
    @SerialName("clap") CLAP,
    @SerialName("tom") TOM,
    @SerialName("other") OTHER
}
