/*
 * :core:common — leaf JDK library.
 *
 * Shared cross-cutting utilities consumed by every other module:
 * dispatcher abstraction (so tests can swap out real dispatchers for
 * deterministic ones from `07_TIMELINE_ENGINE.md`), Result type, etc.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
