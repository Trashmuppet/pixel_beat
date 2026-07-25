#!/usr/bin/env bash
#
# Monochrome Beat — minimal Gradle wrapper.
#
# This script avoids needing a separate gradle-wrapper.jar. It prefers a
# system-installed `gradle` (Android Studio bundles one) and falls back to
# downloading Gradle 8.10.2 once and executing it directly.
#
# To regenerate a complete Gradle Wrapper with the standard jar+properties
# files, run `gradle wrapper --gradle-version 8.10.2` locally at any time.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRADLE_VERSION="${GRADLE_VERSION:-8.10.2}"
DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
INSTALL_DIR="$GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
GRADLE_BIN="$INSTALL_DIR/gradle-${GRADLE_VERSION}/bin/gradle"

run_gradle() {
    exec "$GRADLE_BIN" "$@" -p "$SCRIPT_DIR"
}

# 1. Prefer a system-installed gradle matching the pinned version.
if command -v gradle > /dev/null; then
    DETECTED_VERSION="$(gradle --version 2>/dev/null | awk '/^Gradle /{print $2; exit}' || true)"
    if [ "${DETECTED_VERSION:-}" = "$GRADLE_VERSION" ]; then
        exec gradle "$@" -p "$SCRIPT_DIR"
    fi
    if [ -z "${GRADLE_WRAPPER_REQUIRE_EXACT:-}" ]; then
        # System gradle version differs — try it anyway, it is usually compatible.
        exec gradle "$@" -p "$SCRIPT_DIR"
    fi
fi

# 2. Otherwise, download once into the Gradle user home cache.
if [ ! -x "$GRADLE_BIN" ]; then
    echo "Downloading Gradle $GRADLE_VERSION ..."
    mkdir -p "$GRADLE_USER_HOME/wrapper/dists"
    TMP="$(mktemp -d)"
    if command -v curl > /dev/null; then
        curl -fsSL "$DIST_URL" -o "$TMP/gradle.zip"
    elif command -v wget > /dev/null; then
        wget -q "$DIST_URL" -O "$TMP/gradle.zip"
    else
        echo "Either curl or wget is required to bootstrap Gradle." >&2
        exit 1
    fi
    mkdir -p "$INSTALL_DIR"
    unzip -q "$TMP/gradle.zip" -d "$GRADLE_USER_HOME/wrapper/dists"
    mv "$GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION}" "$INSTALL_DIR"
    rm -rf "$TMP"
fi

run_gradle "$@"
