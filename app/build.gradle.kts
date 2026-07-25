/*
 * :app — Monochrome Beat application module.
 *
 * Aggregates every other Gradle subproject. UI → Domain → Core dependency
 * direction is enforced by explicitly listing each downstream module here
 * rather than expecting transitive wiring.
 */

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

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // Phase 6 (release) wires full R8 + bundle config. Phase 0 keeps
            // the release build green without optimisation so the project is
            // buildable end-to-end.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM controls every other Compose artefact's version.
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    // Pure-Kotlin core (foundation; scene-api is consumed transitively via features)
    implementation(project(":core:model"))
    implementation(project(":core:timeline"))
    implementation(project(":core:common"))

    // Audio / scene
    implementation(project(":audio-native"))
    implementation(project(":scene-runtime"))
    implementation(project(":scene-warehouse"))

    // Features
    implementation(project(":feature-home"))
    implementation(project(":feature-project"))
    implementation(project(":feature-sequencer"))
    implementation(project(":feature-arrangement"))
    implementation(project(":feature-export"))

    // Persistence + monetisation
    implementation(project(":premium"))
    implementation(project(":billing"))
    implementation(project(":storage"))
}
