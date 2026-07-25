/*
 * :scene-neon — Pro-tier `Scene` implementation.
 *
 * Renders drum hits as Procedurally-laid-out pulse shapes (square,
 * diamond, dot, plus, cross) per drum kind. Visually distinct from
 * `WarehouseScene` so free users see a clear Pro-tier reward.
 *
 * Per `09_SCENE_SYSTEM.md` and ADR-004, this scene is deterministic
 * — same project seed + same hit stream ⇒ byte-identical output.
 */

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.trashmuppet.pixelbeat.scene.neon"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":scene-api"))
}
