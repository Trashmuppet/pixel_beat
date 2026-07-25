/*
 * :core:timeline — pure Kotlin/JVM library.
 *
 * Owns the authoritative musical time source per
 * `07_TIMELINE_ENGINE.md`. ADR-001 codifies this rule.
 *
 * Phase 1 locks in:
 *  - Deterministic compile (same input → same `CompiledTimeline` bytes).
 *  - RealtimeTransport interface for the real-time engine.
 *  - OfflineRenderSession interface for export.
 *  - JUnit 5 golden-fixture tests in `src/test`.
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

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
    }
}
