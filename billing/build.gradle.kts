plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.trashmuppet.pixelbeat.billing"
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
}

dependencies {
    implementation(project(":premium"))

    // `EntitlementCache.markPro(value)` uses the
    // `androidx.core.content.edit { putBoolean(...) }` extension
    // function — the hardcoded `7.0.0` here used to be fine without
    // core-ktx but the newer `GooglePlayPremiumManager` reads
    // `prefs.edit()` extension calls into the cache path.
    implementation(libs.androidx.core.ktx)

    // Pin to the catalog so a Play ktx bump is single-source.
    implementation(libs.play.billing.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
