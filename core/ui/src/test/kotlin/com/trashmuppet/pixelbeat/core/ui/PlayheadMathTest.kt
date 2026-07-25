package com.trashmuppet.pixelbeat.core.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Pure-math tests for [PlayheadMath]. Cited in ADR-004 the playhead
 * math must be a pure function of sample position + tempo; this
 * suite pins the contract independently of Compose.
 */
class PlayheadMathTest {

    @Test
    fun `sixteenthSampleCount at 120bpm 48kHz equals 6000 samples`() {
        // 1 sixteenth = (60 / 120) / 4 seconds = 0.125 s → 6000 samples @ 48 kHz
        assertEquals(6000L, PlayheadMath.sixteenthSampleCount(48_000, 120f))
    }

    @Test
    fun `sixteenthSampleCount doubles at 60bpm`() {
        assertEquals(12_000L, PlayheadMath.sixteenthSampleCount(48_000, 60f))
    }

    @Test
    fun `sixteenthSampleCount halves at 240bpm`() {
        assertEquals(3000L, PlayheadMath.sixteenthSampleCount(48_000, 240f))
    }

    @Test
    fun `mid-sixteenth maps to half of one step window`() {
        val f = PlayheadMath.sampleToStepFraction(3000L, 6000L, 16)
        // stepFloat = 0 + 0.5 = 0.5; fraction = 0.5 / 16
        assertEquals(0.5f / 16f, f, 0.0001f)
    }

    @Test
    fun `sample at end of sixteenth maps to one step wide`() {
        val f = PlayheadMath.sampleToStepFraction(5999L, 6000L, 16)
        // stepFloat = 0 + (5999 / 6000) \u2248 0.99983; / 16
        assertEquals(5999f / 6000f / 16f, f, 0.0001f)
    }

    @Test
    fun `boundary at sixteenth wrap is exactly one step`() {
        // sample == sixteenthSamples \u21d2 stepFloat bins to step 1 with fracInStep = 0
        val f = PlayheadMath.sampleToStepFraction(6000L, 6000L, 16)
        assertEquals(1f / 16f, f, 0.0001f)
    }

    @Test
    fun `wrap after totalSteps sixteenths resets to zero fraction`() {
        // 16 full sixteenths \u2192 16 / 16 % 16 = 0 \u2192 stepFloat = 0
        val f = PlayheadMath.sampleToStepFraction(6000L * 16L, 6000L, 16)
        assertEquals(0f, f, 0.0001f)
    }

    @Test
    fun `wrap maintains progress at non-boundary fraction`() {
        // testFraction at sample = 17.5 sixteenths should wrap to 1.5 / 16
        val f = PlayheadMath.sampleToStepFraction(6000L * 17L + 3000L, 6000L, 16)
        // stepIndex = ((17 * 6000 + 3000) / 6000) % 16 = 17 % 16 = 1
        // fracInStep = (3000 / 6000) = 0.5; stepFloat = 1.5; / 16
        assertEquals(1.5f / 16f, f, 0.0001f)
    }

    @Test
    fun `totalSteps 0 returns 0 (graceful)`() {
        assertEquals(0f, PlayheadMath.sampleToStepFraction(12345L, 6000L, 0), 0.0001f)
    }

    @Test
    fun `negative sample clamps to 0 fraction`() {
        assertEquals(0f, PlayheadMath.sampleToStepFraction(-1L, 6000L, 16), 0.0001f)
    }

    @Test
    fun `invalid sample rate rejected`() {
        assertThrows<IllegalArgumentException> {
            PlayheadMath.sixteenthSampleCount(0, 120f)
        }
    }

    @Test
    fun `invalid bpm rejected`() {
        assertThrows<IllegalArgumentException> {
            PlayheadMath.sixteenthSampleCount(48_000, 0f)
        }
        assertThrows<IllegalArgumentException> {
            PlayheadMath.sixteenthSampleCount(48_000, -1f)
        }
    }
}
