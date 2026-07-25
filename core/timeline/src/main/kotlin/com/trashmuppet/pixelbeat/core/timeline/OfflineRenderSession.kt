package com.trashmuppet.pixelbeat.core.timeline

/**
 * Offline rendering session — drives `export` determinism per
 * `12_EXPORT_PIPELINE.md`. The session pulls a chunk at a time from
 * `CompiledTimeline` and yields audio samples until the project is
 * exhausted.
 *
 * Streaming-only: implementations MUST NOT buffer the whole project.
 */
interface OfflineRenderSession {

    /** Open the session for `timeline`. Idempotent. */
    fun open(timeline: CompiledTimeline)

    /** Render `chunkSamples` more samples into a preallocated buffer. */
    fun renderChunk(chunkSamples: Int): FloatArray

    /** True when no more audio remains. */
    fun isExhausted(): Boolean

    /** Release all resources. */
    fun close()
}
