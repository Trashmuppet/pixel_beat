package com.trashmuppet.pixelbeat.assetcompiler

import com.trashmuppet.pixelbeat.core.common.Result
import com.trashmuppet.pixelbeat.core.common.runCatchingResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.security.MessageDigest

/**
 * Phase-0 deterministic compile step.
 *
 * Phase 4 replaces `compileSource` with a real PNG parser + colour
 * quantisation pipeline built around the rules in `11_ASSET_COMPILER.md`.
 * For now we walk the source directory, validate the manifest exists,
 * and emit a deterministic `.mbscene` JSON envelope with a SHA-256
 * content hash.
 */
class AssetCompiler(
    private val json: Json = Json { prettyPrint = false; encodeDefaults = true }
) {

    fun compile(source: Path, output: Path): Result<Path> = runCatchingResult {
        val manifest = source.resolve("manifest.json")
        require(manifest.toFile().isFile) { "missing manifest.json in $source" }

        val manifestBytes = manifest.toFile().readBytes()
        val hash = sha256(manifestBytes)

        val pack = CompiledPack(
            packId = packIdFromSource(source),
            packVersion = 1,
            runtimeVersion = 1,
            schemaVersion = 1,
            contentHash = hash,
            sourceFileCount = source.toFile().listFiles()?.size ?: 0
        )
        output.toFile().parentFile?.mkdirs()
        output.toFile().writeText(json.encodeToString(CompiledPack.serializer(), pack))
        output
    }

    private fun packIdFromSource(source: Path): String =
        source.fileName?.toString() ?: "unnamed-pack"

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}

@Serializable
data class CompiledPack(
    val packId: String,
    val packVersion: Int,
    val runtimeVersion: Int,
    val schemaVersion: Int,
    val contentHash: String,
    val sourceFileCount: Int
)
