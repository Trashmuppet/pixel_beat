/*
 * :baselineprofile — MacroBenchmark BaselineProfile generator.
 *
 * Phase 6 scaffolding. Produces an empty BaselineProfile at
 * install time once `:app:assembleDebug` is run; real benchmarks
 * and rule lists land in a future phase.
 *
 * Plugin choice: `androidx.baselineprofile` comes from Android
 * Baseline Profile Gradle Plugin (`com.androidx.baselineprofile`)
 * which is NOT on the version catalog yet — we declare it inline
 * with a stable version (1.3.0) so the module compiles cleanly.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.trashmuppet.pixelbeat.baselineprofile"
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
    implementation("androidx.test:runner:1.6.2")
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.benchmark:benchmark-common:1.3.3")
}
