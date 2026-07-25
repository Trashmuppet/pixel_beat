package com.trashmuppet.pixelbeat.core.export.video

import com.trashmuppet.pixelbeat.scene.api.SceneRenderState
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * 1-bit indexed palette GIF89a encoder.
 *
 * Determinism rules (locked in ADR-005):
 *  - LZW dictionary is cleared **once**, on the first frame. Never
 *    reset on subsequent frames. Code size grows from
 *    `minCodeSize+1` bits and **never** exceeds 12 bits; codes 4096
 *    and above emit as-is but never widen beyond 12.
 *  - Same input sequence → identical bytes deterministically.
 *  - Output writes a `NETSCAPE2.0` looping extension so the result
 *    replays continuously in every browser-tested renderer.
 *
 * Frame index conversion (1-bit MSB-first per row byte) shared with
 * `SceneRenderer.toImageBitmap` to keep the two code paths in sync.
 */
class GifEncoder {
    private var output: FileOutputStream? = null
    private var width: Int = 0
    private var height: Int = 0
    private var firstFrame: Boolean = true
    private val subBlocks = ByteArrayOutputStream()

    fun begin(outputFile: File, width: Int, height: Int) {
        val safe = outputFile.also { it.parentFile?.mkdirs() }
        val out = FileOutputStream(safe)
        output = out
        this.width = width
        this.height = height
        firstFrame = true

        // GIF header.
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))

        // Logical screen descriptor (7 bytes):
        //   packed byte 0x80: global color table present (256 entries is
        //   overkill for a 2-entry palette; we override below with size=0)
        out.write(byteArrayOf(
            (width and 0xFF).toByte(), ((width shr 8) and 0xFF).toByte(),
            (height and 0xFF).toByte(), ((height shr 8) and 0xFF).toByte(),
            0x80.toByte(),                          // packed (size field = 0 → 2 entries)
            0,                                     // background color index
            0                                      // pixel aspect ratio
        ))
        // Global color table — 6 bytes for 2 entries (black + white).
        out.write(byteArrayOf(
            0, 0, 0,                              // index 0 = #000000
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte() // index 1 = #FFFFFF
        ))
        // NETSCAPE 2.0 application extension — infinite loop.
        out.write(byteArrayOf(0x21, 0xFF, 0x0B))
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(byteArrayOf(0x03, 0x01, 0x00, 0x00, 0x00))
    }

    fun writeFrame(state: SceneRenderState, frameDurationMs: Int) {
        val out = output ?: error("begin() must run before writeFrame()")
        require(state.widthPx == width && state.heightPx == height) {
            "frame dimensions ${state.widthPx}x${state.heightPx} != GIF header ${width}x${height}"
        }

        // Graphic Control Extension (8 bytes payload + 1-byte terminator).
        out.write(byteArrayOf(0x21, 0xF9, 0x04))
        val delayCs = (frameDurationMs / 10).coerceAtLeast(2)
        out.write(byteArrayOf(
            0x00,                                   // packed: no transparent colour
            (delayCs and 0xFF).toByte(),
            ((delayCs shr 8) and 0xFF).toByte(),
            0,                                      // transparent colour index (unused)
            0                                       // block terminator
        ))
        // Image descriptor (10 bytes).
        out.write(byteArrayOf(
            0x2C,
            0, 0, 0, 0,                             // left, top
            (width and 0xFF).toByte(), ((width shr 8) and 0xFF).toByte(),
            (height and 0xFF).toByte(), ((height shr 8) and 0xFF).toByte(),
            0                                       // packed: no local table, no interlace
        ))
        // LZW minimum code size: 2 bits would be wrong; GIF spec
        // mandates `minCodeSize = bitsPerPixel` where palette size 2
        // ⇒ 1 bit. Effective LZW codes start at `minCodeSize + 1 = 2`
        // bits with clear = 4, eod = 5.
        out.write(0x01)
        writeLzw(out, rasterize(state))
    }

    fun finish() {
        val out = output ?: return
        out.write(0x3B) // GIF trailer
        out.close()
        output = null
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /**
     * Convert 1-bit MSB-first row bytes into a palette-indexed raster
     * (each value = 0 for black or 1 for white) for LZW consumption.
     */
    private fun rasterize(state: SceneRenderState): ByteArray {
        val out = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val byte = state.pixels[y * state.rowBytes + (x ushr 3)]
                val bit = (byte.toInt() ushr (7 - (x and 7))) and 1
                out[y * width + x] = bit.toByte()
            }
        }
        return out
    }

    /**
     * LZW image data with strict clear-once + max-12-bit determinism.
     * GIF sub-blocks cap at 255 bytes; we chunk the bit-packed output
     * into those blocks before the terminator.
     */
    private fun writeLzw(out: FileOutputStream, indices: ByteArray) {
        val minCodeSize = 1
        val clearCode = 1 shl minCodeSize           // 2
        val eodCode = clearCode + 1                  // 3
        val subBlock = ByteArrayOutputStream()
        val bitOut = BitOutputStream(subBlock)

        var codeSize = minCodeSize + 1               // starts at 2 bits
        var nextCode = eodCode + 1                   // 4
        val dict = HashMap<Long, Int>(16 * 1024)
        var prefix = -1

        // Emit the CLEAR code exactly once — never again.
        bitOut.write(clearCode, codeSize)

        for (i in indices.indices) {
            val current = indices[i].toInt() and 0xFF
            if (prefix == -1) {
                prefix = current
                continue
            }
            val key = (prefix.toLong() shl 8) or current.toLong()
            val existing = dict[key]
            if (existing != null) {
                prefix = existing
            } else {
                bitOut.write(prefix, codeSize)
                if (nextCode < 4096) {
                    dict[key] = nextCode++
                    if (nextCode > (1 shl codeSize) && codeSize < 12) {
                        codeSize++
                    }
                }
                prefix = current
            }
        }
        if (prefix != -1) {
            bitOut.write(prefix, codeSize)
        }
        bitOut.write(eodCode, codeSize)
        bitOut.flush()

        // Chunk into 255-byte GIF sub-blocks.
        val data = subBlock.toByteArray()
        var offset = 0
        while (offset < data.size) {
            val size = minOf(255, data.size - offset)
            out.write(size)
            out.write(data, offset, size)
            offset += size
        }
        out.write(0)
    }
}

/**
 * LSB-first bit accumulator. Buffers incoming code bits into bytes
 * the right way round for the GIF LZW spec.
 */
private class BitOutputStream(private val out: ByteArrayOutputStream) {
    private var accumulator: Int = 0
    private var bitCount: Int = 0

    fun write(code: Int, bits: Int) {
        accumulator = accumulator or ((code and ((1 shl bits) - 1)) shl bitCount)
        bitCount += bits
        while (bitCount >= 8) {
            out.write(accumulator and 0xFF)
            accumulator = accumulator ushr 8
            bitCount -= 8
        }
    }

    fun flush() {
        if (bitCount > 0) {
            out.write(accumulator and 0xFF)
            accumulator = 0
            bitCount = 0
        }
    }
}
