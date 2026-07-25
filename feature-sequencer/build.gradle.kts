/*
 * :feature-sequencer — pattern editor destination.
 *
 * Phase 0 ships a minimal 16-step monochrome grid placeholder. Phase 3
 * (`02_ROADMAP.md`) wires the real pattern editor from `18_COMPONENT_LIBRARY-1.md`.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.trashmuppet.pixelbeat.feature.sequencer"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:timeline"))
    implementation(project(":storage"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    // ViewModel + viewModelScope + ViewModelProvider live here.
    // Transitive on lifecycle-viewmodel-ktx which provides viewModelScope.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.compose.ui.tooling)
}
