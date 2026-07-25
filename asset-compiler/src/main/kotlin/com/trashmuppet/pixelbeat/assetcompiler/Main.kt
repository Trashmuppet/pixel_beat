package com.trashmuppet.pixelbeat.assetcompiler

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import javax.imageio.ImageIO
import kotlin.system.exitProcess

/** Sprite position/size override declared in the manifest JSON. */
@Serializable
data class SpriteMeta(val x: Int = 0, val y: Int = 0, val w: Int = 0, val h: Int = 0)

/**
 * Author-supplied manifest describing the pack.
 * Loaded from `manifest.json` in the input directory.
 */
@Serializable
data class Manifest(
    val packId: String? = null,
    val packVersion: Int? = null,
    val runtimeVersion: Int = 1,
    val sprites: Map<String, SpriteMeta> = emptyMap()
)

/**
 * CLI entry point — validates PNG sprites + manifest, emits a deterministic
 * `.mbscene` binary blob.
 *
 * Usage:
 * ```
 * ./gradlew :asset-compiler:run --args="--in scenes/foo --out build/foo.mbscene --packId foo --version 1"
 * ```
 *
 * Per [11_ASSET_COMPILER.md] the output is byte-deterministic — running twice
 * on the same input yields bit-identical bytes. The output format is:
 *
 * | Offset | Length | Field |
 * |---|---|---|
 * | 0 | 8 | magic ASCII `MBSCENE` |
 * | 8 | 8 | zero pad |
 * | 16 | 4 | u32 LE packVersion |
 * | 20 | 4 | u32 LE runtimeVersion |
 * | 24 | 4 | u32 LE contentHash (CRC32 of payload) |
 * | 28 | 4 | u32 LE payload byte length |
 * | 32 | N | payload (sprites alphabetised + manifest) |
 */
fun main(args: Array<String>) {
    var inDir = ""
    var outPath = ""
    var packIdCli = ""
    var packVersionCli = -1

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--in" -> { inDir = args[i + 1]; i += 2 }
            "--out" -> { outPath = args[i + 1]; i += 2 }
            "--packId" -> { packIdCli = args[i + 1]; i += 2 }
            "--version" -> { packVersionCli = args[i + 1].toInt(); i += 2 }
            else -> i++
        }
    }

    if (inDir.isEmpty() || outPath.isEmpty()) {
        System.err.println("Usage: --in <dir> --out <file> [--packId <id>] [--version <ver>]")
        exitProcess(1)
    }

    val dir = File(inDir)
    require(dir.isDirectory) { "Input must be a directory: $inDir" }

    val manifestFile = File(dir, "manifest.json")
    require(manifestFile.exists()) { "manifest.json not found in $inDir" }
    val manifestStr = manifestFile.readText()
    val manifest = Json { ignoreUnknownKeys = true }.decodeFromString<Manifest>(manifestStr)

    val packId = packIdCli.takeIf { it.isNotEmpty() } ?: manifest.packId ?: error("Missing packId")
    val packVersion = packVersionCli.takeIf { it != -1 } ?: manifest.packVersion ?: error("Missing packVersion")

    val payloadStream = ByteArrayOutputStream()

    // Read all PNGs alphabetically to maintain determinism.
    val pngFiles = dir.listFiles { f -> f.extension.equals("png", ignoreCase = true) }
        ?.sortedBy { it.name }
        ?: emptyList()

    for (file in pngFiles) {
        val spriteId = file.nameWithoutExtension
        val meta = manifest.sprites[spriteId] ?: SpriteMeta()

        val image = ImageIO.read(file) ?: error("Cannot read image: ${file.name}")
        val w = image.width
        val h = image.height

        // Per [11_ASSET_COMPILER.md]: integer dimensions only.
        require(w > 0 && h > 0) { "Empty image: ${file.name}" }

        val rowBytes = (w + 7) / 8
        val pixels = ByteArray(rowBytes * h)
        // Validations: 1-bit colours only, integer dimensions, no partial alpha.
        for (y in 0 until h) {
            for (x in 0 until w) {
                val argb = image.getRGB(x, y)
                val alpha = (argb ushr 24) and 0xFF
                val r = (argb ushr 16) and 0xFF
                val g = (argb ushr 8) and 0xFF
                val b = argb and 0xFF

                require(alpha == 0 || alpha == 255) {
                    "Non-binary alpha at $x,$y in ${file.name}"
                }
                if (alpha == 255) {
                    require((r == 0 && g == 0 && b == 0) || (r == 255 && g == 255 && b == 255)) {
                        "Non 1-bit color at $x,$y in ${file.name} - got r=$r g=$g b=$b"
                    }
                }

                val bit = if (alpha == 255 && r == 255) 1 else 0
                if (bit == 1) {
                    val byteIdx = y * rowBytes + (x / 8)
                    val mask = (1 shl (7 - (x % 8)))
                    pixels[byteIdx] = (pixels[byteIdx].toInt() or mask).toByte()
                }
            }
        }

        payloadStream.write(spriteId.toByteArray(Charsets.UTF_8))
        payloadStream.write(0) // null terminator

        val metrics = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        metrics.putShort(meta.x.toShort())
        metrics.putShort(meta.y.toShort())
        metrics.putShort(w.toShort())
        metrics.putShort(h.toShort())
        payloadStream.write(metrics.array())
        payloadStream.write(pixels)
    }

    // Append manifest JSON null-terminated per spec.
    payloadStream.write(manifestStr.toByteArray(Charsets.UTF_8))
    payloadStream.write(0)

    val payloadBytes = payloadStream.toByteArray()
    // CRC32 is platform-stable across JVMs and gives deterministic byte output.
    val crc = CRC32().apply { update(payloadBytes) }.value

    val outFile = File(outPath)
    outFile.parentFile?.mkdirs()

    val outBuffer = ByteBuffer
        .allocate(16 + 4 + 4 + 4 + 4 + payloadBytes.size)
        .order(ByteOrder.LITTLE_ENDIAN)
    // 16-byte magic.
    val magic = "MBSCENE".toByteArray(Charsets.US_ASCII)
    outBuffer.put(magic)
    outBuffer.put(ByteArray(16 - magic.size)) // zero pad

    outBuffer.putInt(packVersion)
    outBuffer.putInt(manifest.runtimeVersion)
    outBuffer.putInt(crc.toInt())
    outBuffer.putInt(payloadBytes.size)
    outBuffer.put(payloadBytes)

    outFile.writeBytes(outBuffer.array())
    println("Compiled ${pngFiles.size} sprites to $outPath (packId=$packId, version=$packVersion, hash=$crc)")
    // Use the packId var so the compiler does not flag it as unused; the println already shows it.
    if (packId.isEmpty()) error("packId resolved to empty string")
}
