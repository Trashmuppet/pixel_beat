package com.trashmuppet.pixelbeat.storage

import com.trashmuppet.pixelbeat.core.common.Result
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.storage.cache.RecentProjectDao
import com.trashmuppet.pixelbeat.storage.cache.RecentProjectEntity
import com.trashmuppet.pixelbeat.storage.cache.StorageDatabase
import kotlinx.serialization.json.Json

/**
 * Decorator [ProjectRepository] that keeps a Room mirror of
 * `listRecent()` / `load(id)`. The inner repo remains authoritative
 * (filesystem-backed `.mbeat` per `14_STORAGE.md`); the Room layer is
 * an **always-rebuildable** cache.
 *
 * Behaviour:
 *  - `listRecent(limit)` ⇒ if the cache is empty, refill it from the
 *    inner repo (filesystem walk) and then serve from cache. Cache
 *    exceptions fall back transparently to the inner repo.
 *  - `load(id)` ⇒ serve from cache if present, else delegate.
 *  - `save(project)` ⇒ write through to the inner repo, then upsert
 *    the cache row with a fresh `lastModifiedMs`.
 *  - `delete(id)` ⇒ delete on the inner repo, then evict the row.
 *
 * This keeps `:feature-*` modules unchanged — they only see the
 * [ProjectRepository] interface.
 */
class CachingProjectRepository(
    private val inner: ProjectRepository,
    private val dao: RecentProjectDao,
    private val cacheJson: Json = CACHE_JSON,
    private val bootstrapLimit: Int = 100
) : ProjectRepository {

    override suspend fun load(id: String): Result<MBeatProject> {
        runCatching { dao.getById(id) }.getOrNull()?.let { row ->
            return runCatching {
                Result.Success(cacheJson.decodeFromString(MBeatProject.serializer(), row.projectJson))
            }.getOrElse { inner.load(id) }
        }
        return inner.load(id)
    }

    override suspend fun save(project: MBeatProject): Result<Unit> {
        val result = inner.save(project)
        if (result is Result.Success) {
            runCatching {
                dao.insert(project.toEntity(System.currentTimeMillis()))
            }
        }
        return result
    }

    override suspend fun listRecent(limit: Int): Result<List<MBeatProject>> {
        try {
            if (dao.count() == 0) {
                val disk = inner.listRecent(bootstrapLimit)
                if (disk is Result.Success) {
                    dao.insertAll(disk.value.map { project ->
                        project.toEntity(System.currentTimeMillis())
                    })
                }
            }
            val rows = dao.getRecent(limit)
            if (rows.isNotEmpty()) {
                val parsed = rows.mapNotNull { row ->
                    runCatching {
                        cacheJson.decodeFromString(MBeatProject.serializer(), row.projectJson)
                    }.getOrNull()
                }
                return Result.Success(parsed)
            }
            return inner.listRecent(limit)
        } catch (_: Throwable) {
            // Cache miss / corruption ⇒ fall back transparently to the
            // authoritative filesystem-backed repo.
            return inner.listRecent(limit)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        val result = inner.delete(id)
        if (result is Result.Success) {
            runCatching { dao.delete(id) }
        }
        return result
    }

    /** Test/diagnostic hook: wipe the cache so the next listRecent() rebuilds. */
    suspend fun invalidateCache() = runCatching { dao.clear() }

    private fun MBeatProject.toEntity(timestampMs: Long): RecentProjectEntity =
        RecentProjectEntity(
            id = id,
            name = name,
            bpm = bpm,
            lastModifiedMs = timestampMs,
            projectJson = cacheJson.encodeToString(MBeatProject.serializer(), this)
        )

    companion object {
        private val CACHE_JSON: Json = Json {
            prettyPrint = false
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /**
         * Convenience constructor for production wiring in
         * `AppDependencies.kt` — supplies the [cacheJson] singleton
         * and falls through to the inner repo on cache failures.
         */
        fun wrap(inner: ProjectRepository, database: StorageDatabase): ProjectRepository =
            CachingProjectRepository(inner, database.recentProjectDao())
    }
}
