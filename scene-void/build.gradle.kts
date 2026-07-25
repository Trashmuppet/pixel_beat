/*
 * :scene-void — Pro-tier `Scene` implementation.
 *
 * Renders drum hits as neon outlines over an absolute-black field
 * (no background dots). The "minimal / dark layout" Pro tier —
 * visually distinct from both WarehouseScene (lit cells) and
 * NeonScene (lit background + per-kind shapes).
 *
 * Per `09_SCENE_SYSTEM.md` and ADR-004 this scene is deterministic
 * — same project seed + same hit stream ⇒ byte-identical output.
 */

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.trashmuppet.pixelbeat.scene.darkvoid"
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
