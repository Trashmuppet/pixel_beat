/*
 * Monochrome Beat — Gradle settings
 *
 * Order: app → rendered features & runtime modules → core & abstractions.
 * "UI → Domain → Core" dependency direction is enforced by these `include`
 * lines plus the explicit `dependencies {}` blocks in each module.
 */

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "pixel_beat"

// Application
include(":app")

// Core (pure Kotlin modules — the foundation leaf nodes)
include(":core:model")
include(":core:timeline")
include(":core:common")

// Cross-cutting reusable Compose components (Phase 3).
include(":core:ui")

// Native C++ audio engine (real-time only — no UI / storage access)
include(":audio-native")

// Scene graph (animation owns visuals only — never musical timing)
include(":scene-api")
include(":scene-runtime")
include(":scene-warehouse")

// User-visible features
include(":feature-home")
include(":feature-project")
include(":feature-sequencer")
include(":feature-arrangement")
include(":feature-export")

// Monetisation + offline entitlements
include(":premium")
include(":billing")

// Persistence (.mbeat source of truth, Room as rebuildable cache)
include(":storage")

// Asset compiler (CLI tool for compiling .mbscene packs)
include(":asset-compiler")
