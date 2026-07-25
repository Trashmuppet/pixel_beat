package com.trashmuppet.pixelbeat.core.export.video

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat

/**
 * MediaCodec utility helpers.
 *
 * `MediaUtil.selectEncoder` and `selectColorFormat` walk a codec list
 * for the best matching encoder for the given MIME type. Per
 * `12_EXPORT_PIPELINE.md` + ADR-005, when AAC is unavailable on the
 * SoC the caller must fall back to a PCM-only MP4 — see
 * [Mp4MediaCodecEncoder.encode] for that path.
 *
 * The codec lookup is wired through a [MediaCodecInfoProvider] seam
 * so unit tests can pass an empty / stub provider and exercise the
 * "no AAC available" branch deterministically (the runtime
 * `MediaCodecList(REGULAR_CODECS)` constructor is `@hide`-shadowed
 * on plain JVM CI and is a no-op stub under Robolectric).
 */
object MediaUtil {

    /**
     * DI seam for codec discovery. Production returns the platform's
     * `MediaCodecList(REGULAR_CODECS)`; tests can supply a closed-over
     * list to exercise the absent-codec fallback without a real
     * device.
     */
    fun interface MediaCodecInfoProvider {
        fun codecInfos(): List<MediaCodecInfo>
    }

    /** Production codec source. Lazily initialised by the platform. */
    private val platformProvider: MediaCodecInfoProvider = MediaCodecInfoProvider {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.toList()
    }

    /** Returns the highest-priority H.264 encoder for Surface input, or null. */
    fun selectVideoEncoder(provider: MediaCodecInfoProvider = platformProvider): MediaCodecInfo? =
        selectVideoEncoderFrom(provider.codecInfos())

    /** Returns the highest-priority AAC encoder, or null on SoCs without one. */
    fun selectAacEncoder(provider: MediaCodecInfoProvider = platformProvider): MediaCodecInfo? =
        selectAacEncoderFrom(provider.codecInfos())

    /**
     * Pure-JVM filter exposed for tests. Walks the supplied [infos]
     * list and returns the highest-scored HDR AVC encoder, or null
     * when none match.
     */
    fun selectVideoEncoderFrom(infos: List<MediaCodecInfo>): MediaCodecInfo? =
        infos.asSequence()
            .filter { it.isEncoder }
            .filter { it.supportedTypes.any { t -> t.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) } }
            .sortedByDescending { score(it.name) }
            .firstOrNull()

    /**
     * Pure-JVM filter exposed for tests. Walks the supplied [infos]
     * list and returns the highest-scored AAC encoder, or null when
     * none match (the AAC-fallback path of ADR-005).
     */
    fun selectAacEncoderFrom(infos: List<MediaCodecInfo>): MediaCodecInfo? =
        infos.asSequence()
            .filter { it.isEncoder }
            .filter { it.supportedTypes.any { t -> t.equals(MediaFormat.MIMETYPE_AUDIO_AAC, ignoreCase = true) } }
            .sortedByDescending { score(it.name) }
            .firstOrNull()

    /** Negotiate a `COLOR_FormatSurface`-style input for the video encoder. */
    fun selectColorFormat(codecInfo: MediaCodecInfo, mimeType: String): Int {
        val caps = codecInfo.getCapabilitiesForType(mimeType)
        return caps.colorFormats.firstOrNull { it == MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface }
            ?: caps.colorFormats.firstOrNull { it == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible }
            ?: caps.colorFormats.first()
    }

    /**
     * MediaCodec H.264 encoders reject odd dimensions. Round up to
     * the next even number; the renderer must compensate via
     * integer-scaled viewports per `10_RENDERER.md`.
     */
    fun evenDim(value: Int): Int = if (value and 1 == 1) value + 1 else value

    private fun score(name: String): Int {
        // Prefer hardware encoders flagged by vendor — they outpace
        // software ones 10x on commodity devices. Fall back to names
        // starting with "OMX.google" (Android software fallback).
        val lname = name.lowercase()
        return when {
            "google" in lname -> 0
            "sw" in lname -> 1
            lname.startsWith("c2.android") -> 5
            else -> 10
        }
    }
}
