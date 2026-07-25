/*
 * :audio-native — Android library wrapping the C++ real-time engine.
 *
 * Per `08_AUDIO_ENGINE.md` and `04_REPOSITORY_STRUCTURE.md`:
 *  - The native side runs Oboe / AAudio at 48 kHz with an
 *    allocation-free, lock-free callback.
 *  - This module owns the audio engine only — it must not import
 *    storage, scene-runtime, or any Compose / Android UI dependencies.
 *  - It exposes a narrow Kotlin interface; the platform surface is the
 *    JNI functions exported from src/main/cpp/audio_engine.cpp.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.trashmuppet.pixelbeat.audio"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++20"
                arguments += "-DANDROID_STL=c++_static"
            }
        }

        ndk {
            // Oboe ships for these ABIs; abiFilters keeps the APK lean.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
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
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
}
