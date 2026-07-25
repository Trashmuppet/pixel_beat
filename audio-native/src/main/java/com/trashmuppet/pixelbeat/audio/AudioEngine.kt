package com.trashmuppet.pixelbeat.audio

/**
 * Kotlin façade over `libaudio_native.so`.
 *
 * The native side is initialised in a separate `EngineState` controlled
 * by JNI exports in `audio_engine.cpp`. Per `08_AUDIO_ENGINE.md`:
 *  - The callback must be allocation-free and lock-free. Module
 *    boundaries are deliberately tight so this contract is preserved
 *    as Phase 2 lands real Oboe / AAudio wiring.
 *  - No storage or UI access from this module.
 */
class AudioEngine private constructor() {

    /** Returns 0 on success. */
    fun init(): Int = nativeInit()

    /** Returns 0 on success. Idempotent — calling twice is a no-op. */
    fun start(): Int = nativeStart()

    /** Returns 0 on success. Idempotent. */
    fun stop(): Int = nativeStop()

    val isRunning: Boolean get() = nativeIsRunning().toBoolean()

    /** Native build version string for diagnostics. */
    val version: String get() = nativeVersion()

    private external fun nativeInit(): Int
    private external fun nativeStart(): Int
    private external fun nativeStop(): Int
    private external fun nativeIsRunning(): Boolean
    private external fun nativeVersion(): String

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
    }
}
