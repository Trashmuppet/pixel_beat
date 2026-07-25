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
 *
 * Phase 6+: schema-location export is wired so `MigrationTestHelper`
 * can drive the v1 → v2 migration test on Robolectric. The
 * `testOptions.unitTests.isIncludeAndroidResources = true` flag lets
 * Robolectric find android resources during stub-resolution.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
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

    // Phase 6 close-out: Robolectric needs resources at unit-test time.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

ksp {
    // Phase 6+: emit `1.json` + `2.json` schema snapshots so
    // `MigrationTestHelper` in `StorageDatabaseMigrationTest` can
    // boot the v1 fixture and assert the additive migration. Path
    // is canonical to the AGP 8.7 / KSP 2.0.21 DSL.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // Phase 6+ rebuildable recent-projects cache per docs/14_STORAGE.md
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Phase 6 close-out: Robolectric + Room MigrationTestHelper.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.core)
}
