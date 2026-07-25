package com.trashmuppet.pixelbeat.core.timeline

import com.trashmuppet.pixelbeat.core.model.MBeatProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Real-time playback driver. Implementations live in `:audio-native`
 * (Phase 2 binds this to the Oboe/AAudio render thread).
 *
 * Per ADR-001 the transport owns sample-accurate timing. UI components
 * MUST NOT have timers — they query `position()` and subscribe to
 * `positionFlow` to redraw the playhead.
 */
interface RealtimeTransport {

    /**
     * Hand the compiled timeline to the transport and start audio
     * rendering. Implementations schedule hits lock-free against the
     * audio callback (SPSC ring buffer in `:audio-native`).
     */
    fun start(timeline: CompiledTimeline)

    /** Stop and release any audio buffer references. */
    fun stop()

    /** Sample-accurate playback position in the rendered timeline. */
    fun position(): Long

    /** Hot stream of position changes — drives Playhead redraw. */
    fun positionFlow(): Flow<Long>

    /** Update the live BPM at the next bar boundary. */
    fun setLiveBpm(bpm: Float)

    /** Replace the playing project (atomic, at next bar boundary). */
    fun setProject(project: MBeatProject)
}

/**
 * Trivial in-memory `RealtimeTransport` used by tests and by features
 * that need a deterministic on-screen playhead without yet having the
 * native engine bound. Not for production audio output.
 */
class TestRealtimeTransport : RealtimeTransport {

    private val flow = MutableSharedFlow<Long>(replay = 1, extraBufferCapacity = 16)

    override fun start(timeline: CompiledTimeline) {
        flow.tryEmit(0L)
    }

    override fun stop() {
        flow.tryEmit(-1L)
    }

    override fun position(): Long = flow.replayCache.firstOrNull() ?: 0L

    override fun positionFlow(): Flow<Long> = flow.asSharedFlow()

    override fun setLiveBpm(bpm: Float) = Unit
    override fun setProject(project: MBeatProject) = Unit
}
