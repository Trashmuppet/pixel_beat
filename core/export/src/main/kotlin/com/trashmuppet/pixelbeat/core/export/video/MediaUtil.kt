package com.trashmuppet.pixelbeat.core.export.video

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat

/**
 * MediaCodec utility helpers.
 *
 * `MediaUtil.selectEncoder` and `selectColorFormat` walk the
 * platform's `MediaCodecList` for the best matching encoder for the
 * given MIME type. Per `12_EXPORT_PIPELINE.md` + ADR-005, when AAC is
 * unavailable on the SoC the caller must fall back to a PCM-only
 * MP4 — see [Mp4MediaCodecEncoder.encode] for that path.
 */
object MediaUtil {

    /** Returns the highest-priority H.264 encoder for Surface input, or null. */
    fun selectVideoEncoder(): MediaCodecInfo? {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        return list.codecInfos
            .filter { it.isEncoder && it.supportedTypes.any { t -> t.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) } }
            .sortedByDescending { score(it) }
            .firstOrNull()
    }

    /** Returns the highest-priority AAC encoder, or null on SoCs without one. */
    fun selectAacEncoder(): MediaCodecInfo? {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        return list.codecInfos
            .filter { it.isEncoder && it.supportedTypes.any { t -> t.equals(MediaFormat.MIMETYPE_AUDIO_AAC, ignoreCase = true) } }
            .sortedByDescending { score(it) }
            .firstOrNull()
    }

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

    private fun score(codec: MediaCodecInfo): Int {
        // Prefer hardware encoders flagged by vendor — they outpace
        // software ones 10x on commodity devices. Fall back to names
        // starting with "OMX.google" (Android software fallback).
        val name = codec.name.lowercase()
        return when {
            "google" in name -> 0
            "sw" in name -> 1
            name.startsWith("c2.android") -> 5
            else -> 10
        }
    }
}
