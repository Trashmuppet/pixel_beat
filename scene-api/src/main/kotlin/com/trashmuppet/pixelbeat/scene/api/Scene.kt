package com.trashmuppet.pixelbeat.scene.api

import com.trashmuppet.pixelbeat.core.model.HitEvent
import com.trashmuppet.pixelbeat.core.model.MBeatProject

/**
 * A Scene transforms scheduled `HitEvent`s into a sequence of
 * `SceneRenderState`s at the simulation's fixed rate (240 Hz per
 * `09_SCENE_SYSTEM.md`).
 *
 * A Scene MUST NOT advance musical time — the timeline engine owns time.
 * A Scene MUST NOT do I/O, allocate during the render path, or depend on
 * wall-clock.
 */
interface Scene {
    val id: SceneId

    /** Replace the scene's source material with a new project. */
    fun load(project: MBeatProject)

    /** Feed in a window of upcoming hits. Pure — no side effects. */
    fun schedule(hits: List<HitEvent>)

    /** Advance simulation by one tick and return the new state. */
    fun step(): SceneRenderState
}

/**
 * Stable identifier for a scene pack. Combines the pack ID, pack
 * version, and runtime compatibility version (`09_SCENE_SYSTEM.md`).
 */
data class SceneId(
    val packId: String,
    val packVersion: Int,
    val runtimeVersion: Int
)
