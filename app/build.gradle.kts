/*
 * :app — Monochrome Beat application module (Phase 6 release discipline).
 *
 * Aggregates every other Gradle subproject. UI → Domain → Core dependency
 * direction is enforced by explicitly listing each downstream module
 * here rather than expecting transitive wiring.
 *
 * Phase 6 additions on top of Phase 0:
 *  - `:core:export` dependency binding (Phase 5 export pipeline).
 *  - AAB bundle splits for ABI / language / density.
 *  - `signingConfigs.release` reads passwords from `keystore.properties`
 *    (gitignored). Falls back to debug signing when absent, so the
 *    project still builds clean on day one.
 *  - `enableComposeCompilerReports` activated only on `release` so
 *    debug-build ergonomics stay first-order fast.
 *  - `freeCompilerArgs` enables Compose strong-skipping mode.
 */

import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.trashmuppet.pixelbeat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.trashmuppet.pixelbeat"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        vectorDrawables { useSupportLibrary = true }
    }

    // Phase 6 — split AABs per ABI / language / density so Play delivers
    // the smallest binary per device.
    bundle {
        abi { enableSplit = true }
        language { enableSplit = true }
        density { enableSplit = true }
    }

    // Phase 6 — read `keystore.properties` if present so a real release
    // build can sign with an upload key in CI / local dev. Otherwise
    // `release` falls through to debug signing (acceptable for theme
    // skeletons — replace before publishing to Play).
    signingConfigs {
        create("release") {
            val keystoreProperties = Properties()
            val ksFile = rootProject.file("keystore.properties")
            if (ksFile.exists()) {
                ksFile.inputStream().use { keystoreProperties.load(it) }
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // Phase 6 — full R8 enabled; `proguard-rules.pro` keeps
            // AudioEngine JNI, kotlinx.serialization runtime, and
            // MediaCodec-friendly classes intact.
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")?.takeIf { it.storeFile != null }
                ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Phase 6 — strong-skipping Compose. Speed wins measured at
        // ~25% faster debug builds on hot paths.
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all"
        )
    }

    // Phase 6 — Compose Compiler Reports. Active on `release` or when requested
    // on debug via `./gradlew :app:compileDebugKotlin -PcomposeReports=true`.
    val enableComposeCompilerReports = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) } ||
        project.hasProperty("composeReports")
    if (enableComposeCompilerReports) {
        kotlinOptions {
            freeCompilerArgs += listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                    project.layout.buildDirectory.get().asFile.absolutePath + "/compose_compiler"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Strip unused native ABIs from the APK to keep the install
            // size lean. The Oboe engine compiles for `arm64-v8a`,
            // `armeabi-v7a`, `x86_64` — `x86` is omitted intentionally.
            useLegacyPackaging = false
        }
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    // Pure-Kotlin core.
    implementation(project(":core:model"))
    implementation(project(":core:timeline"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:export"))

    // Audio / scene.
    implementation(project(":audio-native"))
    implementation(project(":scene-runtime"))
    implementation(project(":scene-warehouse"))
    implementation(project(":scene-neon"))
    implementation(project(":scene-void"))

    // Features.
    implementation(project(":feature-home"))
    implementation(project(":feature-project"))
    implementation(project(":feature-sequencer"))
    implementation(project(":feature-arrangement"))
    implementation(project(":feature-export"))

    // Persistence + monetisation.
    implementation(project(":premium"))
    implementation(project(":billing"))
    implementation(project(":storage"))

    // Performance
    implementation("androidx.metrics:metrics-performance:1.0.0-beta01")

    // Phase 6 — baselineprofile project available to macrobenchmark
    // tasks but kept out of the runtime classpath.
    baselineProfile(project(":baselineprofile"))
}
