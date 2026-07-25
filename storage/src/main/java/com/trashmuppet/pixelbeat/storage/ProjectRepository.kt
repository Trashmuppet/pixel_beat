package com.trashmuppet.pixelbeat.storage

import com.trashmuppet.pixelbeat.core.common.Result
import com.trashmuppet.pixelbeat.core.model.MBeatProject

/**
 * The contract feature ViewModels depend on for project storage.
 *
 * Lives in `:storage` per `04_REPOSITORY_STRUCTURE.md` ("storage owns
 * .mbeat persistence"). Feature modules must consume this interface,
 * never `ProjectStore` directly, so unit tests can swap an in-memory
 * fake.
 */
interface ProjectRepository {

    /** Load by id (or `"sample"` for the bundled fixture). */
    suspend fun load(id: String): Result<MBeatProject>

    /** Idempotent atomic save. Returns failure on schema mismatch. */
    suspend fun save(project: MBeatProject): Result<Unit>

    /** Most-recent-first list of recently authored projects on disk. */
    suspend fun listRecent(limit: Int = 16): Result<List<MBeatProject>>

    /** Delete a project. Reserved for Phase 6 housekeeping. */
    suspend fun delete(id: String): Result<Unit>
}
