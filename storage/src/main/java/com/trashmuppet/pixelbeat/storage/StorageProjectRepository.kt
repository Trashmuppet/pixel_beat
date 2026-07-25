package com.trashmuppet.pixelbeat.storage

import android.content.Context
import com.trashmuppet.pixelbeat.core.common.AppDispatchers
import com.trashmuppet.pixelbeat.core.common.Result
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Default `ProjectRepository` implementation backed by `ProjectStore`
 * (UTF-8 filesystem persistence + atomic renames per `14_STORAGE.md`).
 *
 * `listRecent` walks `filesDir/Projects/*.mbeat` and decodes each
 * file. Unknown / unsupported docs are silently skipped per
 * `14_STORAGE.md` ("Preserve unsupported documents unchanged") which
 * means storage does not auto-migrate or throw on bad reads.
 */
class StorageProjectRepository(
    context: Context,
    private val dispatchers: AppDispatchers
) : ProjectRepository {

    private val store: ProjectStore = ProjectStore(context)
    private val root: File = File(context.filesDir, "Projects").apply { mkdirs() }

    override suspend fun load(id: String): Result<MBeatProject> = withContext(dispatchers.io) {
        store.load(id, context.assets)
    }

    override suspend fun save(project: MBeatProject): Result<Unit> = withContext(dispatchers.io) {
        store.save(project)
    }

    override suspend fun listRecent(limit: Int): Result<List<MBeatProject>> =
        withContext(dispatchers.io) {
            runCatching {
                root.listFiles { file -> file.isFile && file.name.endsWith(".mbeat", ignoreCase = true) }
                    ?.sortedByDescending { it.lastModified() }
                    ?.asSequence()
                    ?.mapNotNull { file ->
                        runCatching {
                            store.decode(file)
                        }.getOrNull()
                    }
                    ?.take(limit)
                    ?.toList()
                    ?: emptyList()
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Failure(it) }
            )
        }

    override suspend fun delete(id: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val target = File(root, "$id.mbeat")
            check(target.exists()) { "project $id not found" }
            check(target.delete()) { "could not delete $id" }
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Failure(it) }
        )
    }
}
