package com.trashmuppet.pixelbeat.billing.cache

import android.content.Context
import androidx.core.content.edit

/**
 * On-device entitlement cache. Survives restarts so the app honours
 * the offline entitlement contract from `15_BILLING.md` even when
 * Play is unreachable.
 *
 * Phase 5 replaces writes with verified Play receipts; reads stay here.
 */
class EntitlementCache(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isPro(): Boolean = prefs.getBoolean(KEY_PRO, false)

    fun markPro(value: Boolean) {
        prefs.edit { putBoolean(KEY_PRO, value) }
    }

    private companion object {
        const val PREFS = "pixel_beat_entitlements"
        const val KEY_PRO = "is_pro"
    }
}
