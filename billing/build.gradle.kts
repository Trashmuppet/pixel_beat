/*
 * :billing — Android library.
 *
 * Wraps Google Play Billing (`15_BILLING.md`). Phase 0 ships a
 * SkeletonBillingService that honours the offline entitlement cache
 * contract; Phase 5 wires the full BillingClient flow.
 *
 * No subscriptions. No advertising. No accounts.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.trashmuppet.pixelbeat.billing"
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
    implementation(project(":core:common"))
    implementation(project(":premium"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.play.billing.ktx)
}
