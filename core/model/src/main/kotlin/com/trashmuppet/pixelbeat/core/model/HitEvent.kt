package com.trashmuppet.pixelbeat.core.model

import kotlinx.serialization.Serializable

/**
 * A single musical event on the timeline, the immutable units produced by
 * `TimelineCompiler` and consumed by both `RealtimeTransport` (Phase 1)
 * and the offline render session (`12_EXPORT_PIPELINE.md`).
 *
 * `tick` is in samples at the project's internal rate (48 kHz from
 * `08_AUDIO_ENGINE.md`); ordering is stable and deterministic.
 */
@Serializable
data class HitEvent(
    val tick: Long,
    val trackId: String,
    val velocity: Float = 1.0f,
    /**
     * Position of this hit within the track's own cycle
     * (uses track.lengthSteps when present, else pattern.lengthSteps).
     * Phase 8: enables polyrhythmic scenes / phase-aware rendering.
     */
    val tickModulo: Long = 0L
)
