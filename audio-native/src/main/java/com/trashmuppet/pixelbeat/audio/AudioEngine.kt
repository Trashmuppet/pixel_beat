package com.trashmuppet.pixelbeat.audio

import androidx.annotation.VisibleForTesting
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Kotlin façade over `libaudio_native.so`.
 *
 * Phase 2 expands the surface to include project-driven scheduling
 * (`loadProject` + `schedule`) and offline rendering
 * (`renderOffline`). Per `08_AUDIO_ENGINE.md`:
 *
 *  - Callback must be allocation / lock free (delegated to C++).
 *  - This module imports no UI / storage surface; the room-side
 *    project type is passed by value and translated to JNI arguments
 *    inside this facade.
 */
class AudioEngine private constructor() {

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()
    private val _projectHash = MutableStateFlow(0L)
    val projectHash: StateFlow<Long> = _projectHash.asStateFlow()
    private val _version = MutableStateFlow("audio_native/0.0.0")
    val version: StateFlow<String> = _version.asStateFlow()

    private var handle: Long = INVALID_HANDLE

    /** Create the native engine. Idempotent — returns true on success. */
    fun init(sampleRate: Int = 48_000): Boolean {
        if (handle != INVALID_HANDLE) return true
        handle = nativeCreate(sampleRate)
        if (handle == INVALID_HANDLE) return false
        _version.value = nativeVersion(handle)
        return true
    }

    /** Open the Oboe / AAudio stream. */
    fun start(): Boolean {
        require(handle != INVALID_HANDLE) { "AudioEngine.init must succeed first" }
        val ok = nativeStart(handle) == 0
        if (ok) _running.value = true
        return ok
    }

    /** Stop the stream. Does not release the engine. */
    fun stop(): Boolean {
        if (handle == INVALID_HANDLE) return false
        val ok = nativeStop(handle) == 0
        _running.value = false
        return ok
    }

    val isRunning: Boolean get() = handle != INVALID_HANDLE && nativeIsRunning(handle)

    /**
     * Hand a fresh `MBeatProject` to the engine. The engine resets all
     * internal state and queues the timeline's hits.
     */
    fun loadProject(project: MBeatProject, compiledEventsTickOffsets: LongArray) {
        require(handle != INVALID_HANDLE) { "AudioEngine.init must succeed first" }
        nativeLoadProject(handle, project.projectHash, project.patterns.sumOf { it.tracks.size })
        _projectHash.value = project.projectHash
        // Pre-schedule the first 4096 events to the audio queue.
        val max = minOf(compiledEventsTickOffsets.size, 4096)
        for (i in 0 until max) {
            nativeSchedule(handle, 0, compiledEventsTickOffsets[i], 1.0f)
        }
    }

    /**
     * Render `frames` samples of the currently loaded project offline.
     * Used by export (Phase 5).
     */
    fun renderOffline(frames: Int): FloatArray {
        require(handle != INVALID_HANDLE) { "AudioEngine.init must succeed first" }
        require(frames > 0) { "frames must be positive" }
        val out = FloatArray(frames)
        return if (nativeRenderOffline(handle, out, frames) == 0) out else FloatArray(0)
    }

    /** Release all native resources. */
    fun destroy() {
        if (handle == INVALID_HANDLE) return
        nativeStop(handle)
        nativeDestroy(handle)
        handle = INVALID_HANDLE
        _running.value = false
    }

    // Native surface — Phase 2.
    private external fun nativeCreate(sampleRate: Int): Long
    private external fun nativeDestroy(handle: Long): Int
    private external fun nativeStart(handle: Long): Int
    private external fun nativeStop(handle: Long): Int
    private external fun nativeLoadProject(handle: Long, projectHash: Long, trackCount: Int)
    private external fun nativeSchedule(handle: Long, voiceIndex: Int, samplesUntil: Long, velocity: Float)
    private external fun nativeRenderOffline(handle: Long, out: FloatArray, frames: Int): Int
    private external fun nativeIsRunning(handle: Long): Boolean
    private external fun nativeVersion(handle: Long): String

    companion object {
        init {
            System.loadLibrary("audio_native")
        }

        @Volatile
        private var instanceRef: AudioEngine? = null

        val instance: AudioEngine
            get() = instanceRef ?: synchronized(this) {
                instanceRef ?: AudioEngine().also { instanceRef = it }
            }

        private const val INVALID_HANDLE = 0L

        @VisibleForTesting
        fun resetForTest() {
            instance.destroy()
            instanceRef = null
        }
    }
}
