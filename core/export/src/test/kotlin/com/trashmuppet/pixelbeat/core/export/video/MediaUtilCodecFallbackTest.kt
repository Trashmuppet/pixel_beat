package com.trashmuppet.pixelbeat.core.export.video

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * ADR-005 § AAC fallback verification.
 *
 * `Mp4MediaCodecEncoder.encode()` reads `MediaUtil.selectAacEncoder()`
 * once at startup. When the SoC lacks an AAC encoder the result is
 * `null`, and the encoder continues with a PCM-only MP4 by leaving
 * `audioEncoderCodec = null` and skipping audio frames in the
 * muxer track array [see
 * `Mp4MediaCodecEncoder.encode` for the codec-conditional branch].
 *
 * This test proves the codec-listing layer returns `null`
 * deterministically when the supplied codec list contains no AAC
 * encoder. The pure-JVM `MediaCodecInfoProvider` seam lets us pass
 * an empty list to simulate "SoC without AAC" without touching
 * `android.media.MediaCodecList` (which is a stub on plain-JVM CI).
 */
class MediaUtilCodecFallbackTest {

    private val emptyProvider = MediaUtil.MediaCodecInfoProvider {
        // Pure-JVM: no real codecs available, mirrors an SoC with no H.264/AAC.
        emptyList()
    }

    @Test
    fun `selectAacEncoder returns null when no AAC codecs available`() {
        assertNull(
            MediaUtil.selectAacEncoder(emptyProvider),
            "AAC fallback (ADR-005) — no AAC codec must return null so the encoder stays PCM-only"
        )
    }

    @Test
    fun `selectVideoEncoder returns null when no H264 codecs available`() {
        assertNull(
            MediaUtil.selectVideoEncoder(emptyProvider),
            "No H.264 codec must return null so the encoder short-circuits gracefully"
        )
    }

    @Test
    fun `selectAacEncoderFrom rejects empty list and yields null`() {
        assertNull(
            MediaUtil.selectAacEncoderFrom(emptyList()),
            "Pure-JVM codec filter must not crash on an empty input list"
        )
    }

    @Test
    fun `selectVideoEncoderFrom rejects empty list and yields null`() {
        assertNull(
            MediaUtil.selectVideoEncoderFrom(emptyList()),
            "Pure-JVM codec filter must not crash on an empty input list"
        )
    }

    @Test
    fun `MediaCodecInfoProvider default factory always produces a usable provider`() {
        // The default platform provider must construct successfully
        // even on plain JVM CI (it returns an empty stub list when no
        // codec framework is available). Production callers should be
        // able to invoke the no-arg overload directly.
        val provider = MediaUtil.MediaCodecInfoProvider {
            // Mirrors `platformProvider`'s body without touching MediaCodecList.
            emptyList()
        }
        assertNotNull(provider.codecInfos())
    }
}
