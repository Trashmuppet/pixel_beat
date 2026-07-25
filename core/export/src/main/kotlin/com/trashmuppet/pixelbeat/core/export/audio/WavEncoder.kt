package com.trashmuppet.pixelbeat.core.export.audio

import java.io.File
import java.io.FileOutputStream

/**
 * RIFF WAVE 16-bit PCM mono 48 kHz encoder.
 *
 * Layout per the canonical RIFF spec:
 *  - "RIFF" magic
 *  - 4-byte little-endian file size minus 8
 *  - "WAVE" magic
 *  - "fmt " chunk (16-byte for PCM): format=1 (PCM), channels=1,
 *    sample_rate=48000, byte_rate=96000, block_align=2,
 *    bits_per_sample=16
 *  - "data" chunk followed by raw 16-bit little-endian samples.
 *
 * Determinism: no platform-specific code is involved. Same `samples`
 * ⇒ same bytes.
 */
class WavEncoder(private val sampleRate: Int = 48_000) {

    /**
     * Encode `samples` (32-bit floats in `[-1.0, 1.0]`) as 16-bit
     * PCM mono `outputFile`. Streaming — we chunk-write 16 KiB at a
     * time so peak memory stays flat.
     */
    fun encode(samples: FloatArray, outputFile: File) {
        val bytes = samples.size * 2
        val totalLen = 36 + bytes
        val outputFileSafe = outputFile.also { it.parentFile?.mkdirs() }
        FileOutputStream(outputFileSafe).use { out ->
            // RIFF container
            out.write("RIFF".toByteArray(Charsets.US_ASCII))
            out.write(intLe(totalLen))
            out.write("WAVE".toByteArray(Charsets.US_ASCII))
            // fmt chunk — PCM mono 48 kHz 16-bit.
            out.write("fmt ".toByteArray(Charsets.US_ASCII))
            out.write(intLe(16))                  // fmt body size
            out.write(shortLe(1))                 // audio format = PCM
            out.write(shortLe(1))                 // channels = 1 (mono)
            out.write(intLe(sampleRate))          // sample rate
            out.write(intLe(sampleRate * 2))      // byte rate
            out.write(shortLe(2))                 // block align
            out.write(shortLe(16))                // bits per sample
            // data chunk
            out.write("data".toByteArray(Charsets.US_ASCII))
            out.write(intLe(bytes))
            // PCM samples — chunk 16 KiB at a time so we don't allocate.
            val chunkSamples = 8 * 1024
            val buf = ByteArray(chunkSamples * 2)
            var written = 0
            while (written < samples.size) {
                val end = (written + chunkSamples).coerceAtMost(samples.size)
                var i = 0
                for (s in written until end) {
                    val clamped = samples[s].coerceIn(-1f, 1f)
                    val intSample = (clamped * 32_767f).toInt()
                    val lo = intSample and 0xFF
                    val hi = (intSample shr 8) and 0xFF
                    buf[i] = lo.toByte(); buf[i + 1] = hi.toByte()
                    i += 2
                }
                out.write(buf, 0, (end - written) * 2)
                written = end
            }
        }
    }

    private fun intLe(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte()
    )

    private fun shortLe(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte()
    )
}
