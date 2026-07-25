/*
 * :scene-warehouse — Android library.
 *
 * Default concrete Scene shipped with Monochrome Beat (`09_SCENE_SYSTEM.md`,
 * Phase 4 — Warehouse scene). Built as a real compiled `.mbscene`-style
 * declarative pack to be replaced by `asset-compiler` once Phase 4 lands.
 *
 * Provides a `WarehouseScene : Scene` and a thin Android entry point
 * that hosts the live preview at 60 FPS.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.trashmuppet.pixelbeat.scene.warehouse"
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":scene-api"))
    implementation(project(":scene-runtime"))
    implementation(project(":core:model"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.graphics)
    debugImplementation(libs.compose.ui.tooling)
}
