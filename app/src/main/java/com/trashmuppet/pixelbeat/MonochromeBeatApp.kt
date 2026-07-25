package com.trashmuppet.pixelbeat

import android.app.Application

/**
 * Application entry point for Monochrome Beat.
 *
 * Phase 0 keeps this intentionally small. Phases 1+ will wire
 * PremiumManager, OfflineExportSession initialisation, and audio engine
 * pre-warming here. The class exists now only so the AndroidManifest
 * reference resolves and the build is green from day one.
 */
class MonochromeBeatApp : Application()
