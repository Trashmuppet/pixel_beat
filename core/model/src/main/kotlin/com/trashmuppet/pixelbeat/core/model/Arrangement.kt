package com.trashmuppet.pixelbeat.core.model

import kotlinx.serialization.Serializable

/**
 * The order in which patterns play back. Patterns referenced in
 * `patternChain` must exist in `MBeatProject.patterns`.
 *
 * `loopBars` is optional: when `endBar > startBar` playback loops
 * between those bar indices (end-exclusive).
 */
@Serializable
data class Arrangement(
    val patternChain: List<String>,
    val loopBars: LoopBoundary = LoopBoundary()
) {
    init {
        require(patternChain.isNotEmpty()) { "patternChain cannot be empty" }
    }
}

@Serializable
data class LoopBoundary(
    val startBar: Int = 0,
    val endBar: Int = 0
) {
    /** True when this arrangement should loop. */
    val isLooping: Boolean get() = endBar > startBar

    init {
        require(startBar >= 0) { "startBar must be non-negative" }
        require(endBar >= startBar) { "endBar must be >= startBar" }
    }
}
