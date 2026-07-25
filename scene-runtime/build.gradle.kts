/*
 * :scene-runtime — Android library with Compose Canvas.
 *
 * Per `10_RENDERER.md`:
 *  - Reads SceneRenderState only — never mutates simulation state.
 *  - Outputs only #000000 / #FFFFFF, integer scaling, no AA.
 *
 * The simulation does NOT live here — animation is owned by `Scene`
 * implementations (e.g. `:scene-warehouse`); this module only paints
 * the produced states onto the Compose Canvas.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.trashmuppet.pixelbeat.scene.runtime"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
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
}

dependencies {
    implementation(project(":scene-api"))
    implementation(project(":core:model"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.graphics)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
    }
}
