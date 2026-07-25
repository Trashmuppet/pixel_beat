package com.trashmuppet.pixelbeat.core.model

import kotlinx.serialization.Serializable

/**
 * Authoritative project document for Monochrome Beat — schema v2.
 *
 * Per `14_STORAGE.md` this is the source of truth. Phase 1 locked the
 * schema for the rhythmic + arrangement surface; rendering / export
 * schemas stay downstream.
 *
 * Schema values align with `core/timeline/.../TimelineCompiler.kt`.
 */
@Serializable
data class MBeatProject(
    val id: String,
    val name: String,
    val bpm: Float,
    val seed: ProjectSeed,
    val swing: SwingMode,
    val arrangement: Arrangement,
    val patterns: List<Pattern>,
    val version: Int = MBEAT_CURRENT_VERSION,
    val schema: String = MBEAT_SCHEMA
) {
    init {
        require(bpm in MIN_BPM..MAX_BPM) { "bpm $bpm outside [$MIN_BPM,$MAX_BPM]" }
        require(patterns.isNotEmpty()) { "project must have at least one pattern" }
        require(arrangement.patternChain.isNotEmpty()) { "arrangement.patternChain cannot be empty" }
    }

    companion object {
        const val MBEAT_CURRENT_VERSION = 2
        const val MBEAT_SCHEMA = "mbeat-v2"
        const val MIN_BPM = 30f
        const val MAX_BPM = 300f
    }
}

/**
 * One section of a song. Patterns repeat at the same `lengthSteps` so
 * arrangement chaining is simple per-bar accounting.
 */
@Serializable
data class Pattern(
    val id: String,
    val lengthSteps: Int = 16,
    val tracks: List<Track>
) {
    init {
        require(lengthSteps in 1..64) { "lengthSteps $lengthSteps outside [1,64]" }
        require(tracks.isNotEmpty()) { "pattern must have at least one track" }
    }
}
