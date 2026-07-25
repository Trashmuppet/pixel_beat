/*
 * :premium — pure Kotlin/JVM library.
 *
 * Exposes the `PremiumManager` abstraction that every UI feature queries
 * (`04_REPOSITORY_STRUCTURE.md`, `15_BILLING.md`). The actual entitlement
 * source lives in :billing — features depend on this contract, not on
 * the implementation.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
