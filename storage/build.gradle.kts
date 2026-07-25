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

// Phase 6 close-out (round 11 fix): pin the Kotlin JVM toolchain
// explicitly so KSP receives a populated `jdkHome` argument. Without
// this, the Kotlin compiler is invoked with `jdkHome = null` and KSP
// cannot resolve `java.*` / standard library types during AST
// construction. Room's `@Database` annotation processor then fails
// with `[MissingType]: Element StorageDatabase references a type
// that is not present`. The same corrupted-state failure mode also
// produces garbage "Unclosed comment" lines with impossible line
// numbers in unrelated files (KSP's error reporter operating in a
// degraded mode). Pinning the toolchain to 17 matches the
// `compileOptions` / `kotlinOptions.jvmTarget` configuration and
// makes the JDK visible to KSP.
kotlin {
    jvmToolchain(17)
}

ksp {
    // Phase 6+: emit `1.json` + `2.json` schema snapshots so
    // `MigrationTestHelper` in `StorageDatabaseMigrationTest` can
    // boot the v1 fixture and assert the additive migration. Path
    // is canonical to the AGP 8.7 / KSP 2.0.21 / Room 2.7+ DSL.
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

    // Phase 6 close-out: Robolectric + Room MigrationTestHelper +
    // `FrameworkSQLiteOpenHelperFactory` requires the
    // `androidx.sqlite:sqlite-framework` artifact.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.sqlite.framework)
}