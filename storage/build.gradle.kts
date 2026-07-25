/*
 * :storage — Android library.
 *
 * Owns .mbeat persistence. Per `14_STORAGE.md`:
 *  - `.mbeat` documents are the source of truth.
 *  - This module also houses the Room database, which is a rebuildable
 *    cache ONLY — never authoritative.
 *
 * Phase 0 ships a UTF-8 filesystem-backed `ProjectStore` plus a sample
 * `.mbeat` asset. Phase 1 layers Room on top using KSP.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.trashmuppet.pixelbeat.storage"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
