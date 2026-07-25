/*
 * :core:export — five-format-or-three-format? offline render pipeline.
 *
 * Per `12_EXPORT_PIPELINE.md` the export is THE reference implementation
 * for byte-equal playback. Three formats: WAV (audio only), MP4
 * (audio+video via MediaCodec/MediaMuxer), GIF (1-bit indexed LZW).
 *
 * Notably this module is an **Android library, not Compose-free JVM**
 * because every encoder is `android.media.*` (MediaCodec, MediaMuxer,
 * AAC) — we cannot drop to `org.jetbrains.kotlin.jvm` here.
 *
 * Compose dependencies are absent intentionally; the encoder pipeline
 * does NOT use Compose — it renders into GLES2 surfaces owned by
 * MediaCodec. This keeps the encoder thread off the Compose runtime.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.trashmuppet.pixelbeat.core.export"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:timeline"))
    implementation(project(":scene-api"))
    implementation(project(":scene-runtime"))

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
