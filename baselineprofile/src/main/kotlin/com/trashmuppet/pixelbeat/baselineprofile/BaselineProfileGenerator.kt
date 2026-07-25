package com.trashmuppet.pixelbeat.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 8 BaselineProfile generator.
 *
 * Walks the critical user path — Home → Sequencer → Arrangement → Export —
 * to capture AOT compilation profiles for UI rendering and navigation
 * routes. The Baseline Profile Gradle Plugin feeds these rules into
 * `ProfileInstaller` at install time so the JIT/profile-guided compiler
 * pre-compiles the hot paths.
 *
 * Per 13_PERFORMANCE_AND_TESTING.md:
 * Cold app launch → Home interactive < 1 s budget.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun generateProfile() {
        baselineRule.collect(packageName = "com.trashmuppet.pixelbeat") {
            // 1. Cold start → HomeScreen.
            pressHome()
            startActivityAndWait()
            device.waitForIdle()

            // 2. Navigate to Sequencer ("Create Project" / "New Project").
            val newProjectButton = device.findObject(By.textContains("Create"))
            newProjectButton?.clickAndWait(Until.newWindow(), /* timeoutMs = */ 2_000)
            device.waitForIdle()

            // 3. Navigate to Arrangement ("Next" button on Sequencer).
            val nextButton = device.findObject(By.textContains("Next"))
            nextButton?.clickAndWait(Until.newWindow(), /* timeoutMs = */ 2_000)
            device.waitForIdle()

            // 4. Navigate to Export ("Export" button on Arrangement).
            val exportButton = device.findObject(By.textContains("Export"))
            exportButton?.clickAndWait(Until.newWindow(), /* timeoutMs = */ 2_000)
            device.waitForIdle()
        }
    }
}
