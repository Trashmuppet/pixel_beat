/*
 * :asset-compiler — pure Kotlin/JVM application module.
 *
 * Per `11_ASSET_COMPILER.md` takes artist-authored PNG sprites + JSON
 * animation manifests and produces deterministic, validated `.mbscene`
 * packs. Validation rules:
 *  - 1-bit colours only (#000000 / #FFFFFF).
 *  - Integer dimensions.
 *  - One-pixel outlines.
 *  - No alpha, no anti-aliasing.
 *  - Runtime compatibility metadata emitted per pack.
 *
 * Phase 0 ships a stub `compile(input, output)` with hash output.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.trashmuppet.pixelbeat.assetcompiler.MainKt")
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
