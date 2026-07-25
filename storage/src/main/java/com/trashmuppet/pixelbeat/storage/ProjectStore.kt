package com.trashmuppet.pixelbeat.storage

import android.content.Context
import com.trashmuppet.pixelbeat.core.common.Result
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.model.MBeatProject.Companion.MBEAT_CURRENT_VERSION
import com.trashmuppet.pixelbeat.core.model.MBeatProject.Companion.MBEAT_SCHEMA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * UTF-8 filesystem-backed `.mbeat` persistence.
 *
 * Per `14_STORAGE.md`:
 *  - `.mbeat` documents are versioned UTF-8.
 *  - Saves are atomic (write-to-temp, rename).
 *  - Offline only — no network.
 *  - Unsupported documents are preserved unchanged; we never auto-migrate.
 */
class ProjectStore(context: Context) {

    private val root: File = File(context.filesDir, "Projects").apply { mkdirs() }

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Read a project by id. Falls back to the bundled `sample.mbeat`
     * asset for the special id `sample`.
     */
    suspend fun load(id: String, assets: android.content.res.AssetManager): Result<MBeatProject> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = if (id == SAMPLE_ID) {
                    assets.open(SAMPLE_ASSET).use { it.readBytes() }
                } else {
                    File(root, "$id.mbeat").readBytes()
                }
                json.decodeFromString(MBeatProject.serializer(), bytes.decodeToString())
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Failure(it) }
            )
        }

    /** Atomically save a project as `<id>.mbeat` under filesDir/Projects. */
    suspend fun save(project: MBeatProject): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(project.schema == MBEAT_SCHEMA) {
                "Unsupported .mbeat schema '${project.schema}' (expected $MBEAT_SCHEMA)"
            }
            require(project.version == MBEAT_CURRENT_VERSION) {
                "Unsupported .mbeat version ${project.version} (expected $MBEAT_CURRENT_VERSION)"
            }
            val target = File(root, "${project.projectId}.mbeat")
            val tmp = File(root, "${project.projectId}.mbeat.tmp")
            tmp.writeText(json.encodeToString(MBeatProject.serializer(), project), Charsets.UTF_8)
            // Atomic rename — POSIX guarantees `rename` atomically replaces.
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Failure(it) }
        )
    }

    /**
     * Decode an `.mbeat` file directly without id-based path lookup.
     * Used by `StorageProjectRepository.listRecent` and tests.
     *
     * Per `14_STORAGE.md` "Preserve unsupported documents unchanged":
     * unknown schemas raise so callers can decide whether to keep or
     * skip the file.
     */
    fun decode(file: File): MBeatProject =
        json.decodeFromString(MBeatProject.serializer(), file.readText(Charsets.UTF_8))

    companion object {
        const val SAMPLE_ID = "sample"
        const val SAMPLE_ASSET = "sample.mbeat"
    }
}
