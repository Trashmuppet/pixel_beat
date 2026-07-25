/*
 * :scene-api — pure Kotlin/JVM library.
 *
 * Defines the abstractions that the scene graph is rendered against.
 * Per `09_SCENE_SYSTEM.md` and `10_RENDERER.md`:
 *   Scene → HitEvents → AnimationSystem → SceneRenderState → Renderer
 *
 * This module owns `SceneRenderState` and the `Scene` contract. Scene
 * implementations and rendering live downstream.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
}
