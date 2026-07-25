/*
 * :baselineprofile — MacroBenchmark BaselineProfile generator.
 *
 * Phase 8: real macrobenchmark rules target the critical user path
 * (Home → Sequencer → Arrangement → Export) so the JIT/profile-guided
 * compiler can AOT the hot paths.
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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    implementation("androidx.test:runner:1.6.2")
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.benchmark:benchmark-common:1.3.3")

    // Phase 8 — real BaselineProfile generation dependencies.
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.2.4")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
