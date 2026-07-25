/*
 * :core:timeline — pure Kotlin/JVM library.
 *
 * Implements `TimelineCompiler` from `07_TIMELINE_ENGINE.md`. The compiler
 * is the only authority for translating `.mbeat` data into ordered
 * HitEvents. It must be deterministic, allocation-light, and free of
 * wall-clock or UI dependencies.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
