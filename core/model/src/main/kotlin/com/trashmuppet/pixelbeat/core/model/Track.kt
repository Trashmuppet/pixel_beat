package com.trashmuppet.pixelbeat.core.model

import kotlinx.serialization.Serializable

/**
 * One instrument in a pattern.
 *
 * `steps[i]` is `true` ↔ that step index (0..lengthSteps-1) is active.
 * Storing the full bar (rather than a list of active indices) makes the
 * pattern editor UI (`18_COMPONENT_LIBRARY-1.md`) trivial: just flip the
 * cell at the relevant index.
 */
@Serializable
data class Track(
    val id: String,
    val kind: DrumKind = DrumKind.OTHER,
    val steps: List<Boolean>,
    val volumeDb: Float = 0f,
    val panCb: Float = 0f,
    val mute: Boolean = false
) {
    init {
        require(steps.isNotEmpty()) { "track.steps cannot be empty" }
        require(volumeDb in MIN_DB..MAX_DB) { "volumeDb $volumeDb outside [$MIN_DB,$MAX_DB]" }
        require(panCb in -100f..100f) { "panCb $panCb outside [-100,100]" }
    }

    companion object {
        const val MIN_DB = -60f
        const val MAX_DB = 12f
    }
}
