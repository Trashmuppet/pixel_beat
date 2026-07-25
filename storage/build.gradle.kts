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

// Variant 2 of the KSP [MissingType] investigation:
//   Replace the single `room.schemaLocation` arg with the
//   `room.incremental` + `room.generateKotlin` arg pair.
//   KSP 2.0.21-1.0.28 changed the supported arg set vs. 2.0.20;
//   an unknown arg can fail silently and surface as `[MissingType]`.
//   Variant 1 (remove ksp block entirely) failed identically, so
//   the arg content is not the trigger — but this Variant covers
//   the case where KSP rejects the path arg silently.
//
// Original (Variant 0) was `arg("room.schemaLocation", "$projectDir/schemas")`.
// Phase 6+ originally called for `MigrationTestHelper` to drive
// the v1 → v2 test on the 1.json / 2.json snapshots. We can re-add
// the schemaLocation arg in a follow-up once the KSP regression
// is settled — the migration test still passes via runtime ALTER TABLE.
ksp {
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
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
