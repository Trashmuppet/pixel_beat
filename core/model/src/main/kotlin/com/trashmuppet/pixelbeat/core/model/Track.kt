package com.trashmuppet.pixelbeat.core.model

import kotlinx.serialization.Serializable

/**
 * One instrument in a pattern.
 *
 * `steps[i]` is `true` ↔ that step index (0..lengthSteps-1) is active.
 * Storing the full bar (rather than a list of active indices) makes the
 * pattern editor UI (`18_COMPONENT_LIBRARY-1.md`) trivial: just flip the
 * cell at the relevant index.
 *
 * Phase 8 additions (all nullable → full backwards compat with v2 `.mbeat`):
 *  - `velocities[i]` (0..1) per-step velocity — accents and ghost notes.
 *  - `lengthSteps` per-track cycle length — drives polyrhythms.
 *  - `swingOverride` per-track swing — e.g. straight kick under swung hats.
 *
 * `ignoreUnknownKeys = true` on the JSON config guarantees old parsers
 * drop these new fields silently; new readers default them when the
 * v2 doc predates the feature.
 */
@Serializable
data class Track(
    val id: String,
    val kind: DrumKind = DrumKind.OTHER,
    val steps: List<Boolean>,
    val volumeDb: Float = 0f,
    val panCb: Float = 0f,
    val mute: Boolean = false,
    /** Per-step velocity in [0, 1]. Null or shorter than [steps] → default 1.0. */
    val velocities: List<Float>? = null,
    /** Per-track cycle length. Null → inherit pattern's [Pattern.lengthSteps]. */
    val lengthSteps: Int? = null,
    /** Per-track swing override. Null → inherit project's [MBeatProject.swing]. */
    val swingOverride: SwingMode? = null
) {
    init {
        require(steps.isNotEmpty()) { "track.steps cannot be empty" }
        require(lengthSteps == null || lengthSteps in 1..64) {
            "track.lengthSteps $lengthSteps outside [1,64]"
        }
        require(velocities == null || velocities.size == steps.size) {
            "velocities size (${velocities?.size}) must match steps size (${steps.size})"
        }
        require(volumeDb in MIN_DB..MAX_DB) { "volumeDb $volumeDb outside [$MIN_DB,$MAX_DB]" }
        require(panCb in -100f..100f) { "panCb $panCb outside [-100,100]" }
    }

    companion object {
        const val MIN_DB = -60f
        const val MAX_DB = 12f
    }
}
