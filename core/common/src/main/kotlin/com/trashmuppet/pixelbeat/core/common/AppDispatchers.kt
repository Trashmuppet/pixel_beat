package com.trashmuppet.pixelbeat.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Abstraction over coroutine dispatchers.
 *
 * Production code uses `DefaultAppDispatchers`; tests use a deterministic
 * implementation (e.g. `StandardTestDispatcher` injected through this
 * interface). The timeline + audio engine stay wall-clock-free per
 * `07_TIMELINE_ENGINE.md` / `08_AUDIO_ENGINE.md`.
 */
interface AppDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

class DefaultAppDispatchers : AppDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
