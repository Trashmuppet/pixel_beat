package com.trashmuppet.pixelbeat.scene.runtime

import com.trashmuppet.pixelbeat.core.model.HitEvent
import com.trashmuppet.pixelbeat.scene.api.Scene
import com.trashmuppet.pixelbeat.scene.api.SceneRenderState

/**
 * Fixed 240 Hz scene simulator.
 *
 * Per `09_SCENE_SYSTEM.md` and `10_RENDERER.md` the simulation must
 * never depend on wall-clock timing. The system advances only when a
 * caller invokes `advance(audioFrames, sampleRate)`, which converts
 * audio frames into scene ticks via:
 *
 *     ticksPerSecond = SCENE_TICK_HZ
 *     sceneTickHz   > AV rate of audio sample chunk / stats
 *     audioFramesPerTick = audioSampleRate / SCENE_TICK_HZ
 *
 * The system owns an `ArrayDeque<HitEvent>` of pending hits. Before
 * each `Scene.step()`, hits whose `tick` ≤ current scene tick are
 * pushed onto the scene via `Scene.schedule(hits)`. The scene
 * renders and returns its `SceneRenderState`.
 *
 * Determinism rules (`09_SCENE_SYSTEM.md`:
 *  - Time comes in as `audioFrames + sampleRate`.
 *  - `enqueueHit` orders hits in the deque by `tick`.
 *  - Identical input sequences produce byte-identical output.
 */
class AnimationSystem(val scene: Scene) {

    private val pendingHits: ArrayDeque<HitEvent> = ArrayDeque()
    private var currentTick: Long = 0L
    private var lastState: SceneRenderState? = null

    /** Submit a hit to be consumed on the scene tick whose index ≥ `hit.tick / 200`. */
    fun enqueueHit(hit: HitEvent) {
        val insertionIndex = pendingHits.indexOfFirst { it.tick > hit.tick }.coerceAtLeast(0)
        pendingHits.add(insertionIndex, hit)
    }

    /** Advance the simulation by `audioFrames` at `audioSampleRate`. */
    fun advance(audioFrames: Int, audioSampleRate: Int): SceneRenderState {
        require(audioFrames > 0) { "audioFrames must be positive" }
        require(audioSampleRate > 0) { "audioSampleRate must be positive" }
        val audioFramesPerTick = audioSampleRate / SCENE_TICK_HZ
        require(audioFramesPerTick > 0) { "audioSampleRate too small for 240Hz sim" }
        val ticksToAdvance = audioFrames / audioFramesPerTick
        require(ticksToAdvance > 0) {
            "audioFrames=$audioFrames < audioFramesPerTick=$audioFramesPerTick; cannot advance"
        }

        var state: SceneRenderState? = null
        repeat(ticksToAdvance) {
            // Drain pending hits that have arrived by this scene tick.
            while (pendingHits.isNotEmpty() && pendingHits.first().tick <= currentTick) {
                scene.schedule(listOf(pendingHits.removeFirst()))
            }
            state = scene.step()
            currentTick += 1
        }
        return state ?: emptyState()
    }

    /**
     * One explicit scene-tick advance (for tests / fine-grained UI
     * redraw). Equivalent to `advance(audioSampleRate / 240, audioSampleRate)`.
     */
    fun step(): SceneRenderState = advance(SCENE_AUDIO_FRAMES_PER_TICK, SCENE_AUDIO_FRAMES_PER_TICK_SR)

    fun reset() {
        pendingHits.clear()
        currentTick = 0L
        lastState = null
        // Scene state (e.g. WarehouseScene's cell layout) remains
        // owned by the Scene implementation. AnimationSystem only
        // owns its hit queue + tick counter.
    }

    fun currentTick(): Long = currentTick
    fun pendingCount(): Int = pendingHits.size

    /**
     * The most recent `SceneRenderState` returned by `Scene.step()`
     * after the last `advance`. Used by the export pipeline to pull
     * framebuffers for `GifEncoder`/`Mp4MediaCodecEncoder` without
     * coupling those encoders to the scene's internal queue.
     */
    fun latestRenderState(): SceneRenderState? = lastState

    private fun emptyState(): SceneRenderState = LAST_EMPTY

    companion object {
        const val SCENE_TICK_HZ: Int = 240

        // Internal audio rate consistent with `08_AUDIO_ENGINE.md` (48 kHz).
        const val DEFAULT_AUDIO_SAMPLE_RATE: Int = 48_000
        private const val SCENE_AUDIO_FRAMES_PER_TICK = DEFAULT_AUDIO_SAMPLE_RATE / SCENE_TICK_HZ
        private const val SCENE_AUDIO_FRAMES_PER_TICK_SR = DEFAULT_AUDIO_SAMPLE_RATE

        private val LAST_EMPTY: SceneRenderState = object : SceneRenderState {
            override val widthPx = 0
            override val heightPx = 0
            override val pixels: ByteArray = ByteArray(0)
            override val rowBytes = 0
            override val tick = 0L
        }
    }
}
