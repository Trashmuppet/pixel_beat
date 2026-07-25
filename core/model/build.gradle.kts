/*
 * :core:model — pure Kotlin/JVM library.
 *
 * Holds the immutable data classes that represent Monochrome Beat's
 * authoritative document format — versioned UTF-8 `.mbeat` JSON encoded
 * via kotlinx.serialization.
 *
 * Per `04_REPOSITORY_STRUCTURE.md` and `01_PRODUCT_PILLARS.md` this
 * module is the source of truth that everything else maps onto.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
