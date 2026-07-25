/*
 * :core:ui — reusable Compose components.
 *
 * Phase 3 introduces a small, opinionated library of stateless
 * composables shared by every feature module. Held under `:core:ui`
 * so feature modules never own the 1-bit palette directly — they
 * import it from here.
 *
 * Architectural rules (`16_UI_BIBLE.md`, `17_DESIGN_BIBLE-1.md`,
 * `18_COMPONENT_LIBRARY-1.md`): stateless, monochrome, 48dp tap
 * targets, single primary action per screen, accessibility labels
 * on every interactive element.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.trashmuppet.pixelbeat.core.ui"
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
    api(project(":core:model"))

    val composeBom = platform(libs.compose.bom)
    api(composeBom)

    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.compose.material3)
    api(libs.compose.ui.graphics)
    debugImplementation(libs.compose.ui.tooling)

    // PlayheadMath unit tests — verify sixteenth-sample + sample→fraction
    // math without spinning a Compose runtime. ADR-004 invariant.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

// Phase 6 §13 docs require JUnit 5 platform for module-level unit tests.
tasks.withType<Test> { useJUnitPlatform() }
