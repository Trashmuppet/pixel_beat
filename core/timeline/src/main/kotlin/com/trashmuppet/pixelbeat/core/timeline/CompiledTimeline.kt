package com.trashmuppet.pixelbeat.core.timeline

import com.trashmuppet.pixelbeat.core.model.HitEvent
import com.trashmuppet.pixelbeat.core.model.MBeatProject

/**
 * The output of `TimelineCompiler.compile` — an ordered, deterministic
 * stream of musical events spanning the project.
 *
 * Chunked per the docs (`12_EXPORT_PIPELINE.md` "streaming, no
 * unbounded buffering"): rendering / playback pull `chunkAt(sample)`
 * or iterate over `events` directly. Both real-time transport and
 * offline render consume the same `CompiledTimeline` so output is
 * byte-identical.
 *
 * `totalSamples` is computed at compile time so duration budget is
 * known before audio render begins.
 */
data class CompiledTimeline(
    val project: MBeatProject,
    val events: List<HitEvent>,
    val totalSamples: Long,
    val internalSampleRate: Int,
    val projectHash: Long
) {
    fun eventsInRange(startSample: Long, endSample: Long): List<HitEvent> {
        val lo = binarySearchFloor(events, startSample)
        val hi = binarySearchCeiling(events, endSample)
        return events.subList(lo, hi)
    }
}

private fun binarySearchFloor(events: List<HitEvent>, target: Long): Int {
    var lo = 0
    var hi = events.size
    while (lo < hi) {
        val mid = (lo + hi) ushr 1
        if (events[mid].tick < target) lo = mid + 1 else hi = mid
    }
    return lo
}

private fun binarySearchCeiling(events: List<HitEvent>, target: Long): Int {
    var lo = 0
    var hi = events.size
    while (lo < hi) {
        val mid = (lo + hi) ushr 1
        if (events[mid].tick <= target) lo = mid + 1 else hi = mid
    }
    return lo
}
