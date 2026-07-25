package com.trashmuppet.pixelbeat.baselineprofile

/**
 * Phase 6 BaselineProfile generator.
 *
 * Real macrobenchmark rules and warmup paths arrive in a future
 * phase. For now we ship a stub `generateProfile()` method that
 * returns the empty `Profile` so the build pipeline wires
 * correctly and future contributors have a single replacement point.
 */
object BaselineProfileGenerator {

    /**
     * Returns the empty baseline profile. Compose / Activity
     * warm-up rules will be added by Phase 6 follow-ups.
     */
    fun generateProfile(): Profile = Profile(emptyMap())
}

/**
 * Minimal `Profile` value type that mirrors `androidx.profile.Profile`
 * (which is intentionally NOT depended on here so we keep the
 * baselineprofile module lightweight). Real code will plumb the
 * `Profile.Builder` produced by these rules into a generator call.
 */
data class Profile(
    private val rules: Map<String, Hotness>
) {
    /** Cycles for a method/class. Hotter = warmer the JIT should be. */
    enum class Hotness { HOT, WARM, LUKEWARM }
}
