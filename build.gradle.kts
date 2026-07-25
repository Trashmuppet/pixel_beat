/*
 * Monochrome Beat — root build script
 *
 * All plugin / version coordination is handled via the version catalog
 * (gradle/libs.versions.toml). Modules apply them via `alias(libs.plugins.X)`.
 */

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
}
