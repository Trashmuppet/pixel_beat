package com.trashmuppet.pixelbeat.storage

import com.trashmuppet.pixelbeat.core.common.Result
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.model.Pattern
import com.trashmuppet.pixelbeat.core.model.Track
import com.trashmuppet.pixelbeat.core.model.DrumKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import com.trashmuppet.pixelbeat.storage.cache.RecentProjectDao
import com.trashmuppet.pixelbeat.storage.cache.RecentProjectEntity

/**
 * Smoke tests for [CachingProjectRepository].
 *
 * Uses an in-memory fake DAO to keep these pure JVM (no Android
 * instrumentation). The cache rebuild / invalidation / delete-eviction
 * invariants are the load-bearing behaviour of the decorator.
 */
class CachingProjectRepositoryTest {

    private fun blankProject(id: String, name: String, bpm: Float = 120f): MBeatProject =
        MBeatProject(
            id = id,
            name = name,
            bpm = bpm,
            seed = com.trashmuppet.pixelbeat.core.model.ProjectSeed(0L),
            swing = com.trashmuppet.pixelbeat.core.model.SwingMode(),
            arrangement = com.trashmuppet.pixelbeat.core.model.Arrangement(
                patternChain = listOf("main"),
                loopBars = com.trashmuppet.pixelbeat.core.model.LoopBoundary()
            ),
            patterns = listOf(
                Pattern(
                    id = "main",
                    lengthSteps = 16,
                    tracks = listOf(
                        Track(id = "kick", kind = DrumKind.KICK, steps = List(16) { false })
                    )
                )
            )
        )

    @Test
    fun `listRecent cache miss rebuilds from inner repository`() = runBlocking {
        val dao = InMemoryDao()
        val innerCalls = AtomicInteger(0)
        val inner = object : ProjectRepository {
            override suspend fun load(id: String) = Result.Success(blankProject(id, "Project $id"))
            override suspend fun save(project: MBeatProject) = Result.Success(Unit)
            override suspend fun listRecent(limit: Int): Result<List<MBeatProject>> {
                innerCalls.incrementAndGet()
                return Result.Success(listOf(blankProject("a", "Alpha"), blankProject("b", "Bravo")))
            }
            override suspend fun delete(id: String) = Result.Success(Unit)
        }
        val dec = CachingProjectRepository(inner, dao)

        val first = dec.listRecent(8)
        assertTrue(first is Result.Success && first.value.map { it.id } == listOf("a", "b"))
        assertEquals("inner repo consulted once on cold list", 1, innerCalls.get())
        assertEquals("cache now holds both rows", 2, dao.all().size)

        // Second call should hit the cache, inner count must stay put.
        dec.listRecent(8)
        assertEquals("subsequent listRecent hits cache; inner not re-queried", 1, innerCalls.get())
    }

    @Test
    fun `save invalidates and refreshes the cached row`() = runBlocking {
        val dao = InMemoryDao()
        val inner = RecordingRepo()
        val dec = CachingProjectRepository(inner, dao)
        val original = blankProject("a", "Alpha", bpm = 120f)
        inner.save(original)
        dec.save(original)

        // Cache row inserted.
        val rowBefore = dao.getById("a") ?: fail("expected cached row a")
        assertEquals("Alpha", rowBefore.name)

        // Save new version → cache row updated.
        val renamed = original.copy(name = "Alpha Renamed", bpm = 140f)
        dec.save(renamed)
        val rowAfter = dao.getById("a") ?: fail("cache row lost after save")
        assertEquals("Alpha Renamed", rowAfter.name)
        assertEquals(140f, rowAfter.bpm, 0.0001f)
    }

    @Test
    fun `delete evicts the cached row`() = runBlocking {
        val dao = InMemoryDao()
        val inner = RecordingRepo()
        val dec = CachingProjectRepository(inner, dao)
        val p = blankProject("a", "Alpha")
        inner.save(p)
        dec.save(p)
        assertTrue(dao.getById("a") != null)

        dec.delete("a")
        assertTrue("cache row evicted", dao.getById("a") == null)
    }

    @Test
    fun `cache failure transparently falls back to inner repository`() = runBlocking {
        val dao = ThrowingDao()
        val inner = object : ProjectRepository {
            override suspend fun load(id: String) = Result.Success(blankProject(id, "Fallback $id"))
            override suspend fun save(project: MBeatProject) = Result.Success(Unit)
            override suspend fun listRecent(limit: Int) =
                Result.Success(listOf(blankProject("fallback-only", "InnerOnly")))
            override suspend fun delete(id: String) = Result.Success(Unit)
        }
        val dec = CachingProjectRepository(inner, dao)
        val out = dec.listRecent(8) as Result.Success
        assertEquals(listOf("fallback-only"), out.value.map { it.id })
    }

    // ------------------------------------------------------------------
    // Test doubles — purely JVM in-memory.
    // ------------------------------------------------------------------

    private class InMemoryDao : RecentProjectDao {
        private val rows = mutableMapOf<String, RecentProjectEntity>()
        override suspend fun getRecent(limit: Int) =
            rows.values.sortedByDescending { it.lastModifiedMs }.take(limit)
        override suspend fun getById(id: String) = rows[id]
        override suspend fun insert(entity: RecentProjectEntity) { rows[entity.id] = entity }
        override suspend fun insertAll(entities: List<RecentProjectEntity>) {
            entities.forEach { rows[it.id] = it }
        }
        override suspend fun delete(id: String) { rows.remove(id) }
        override suspend fun clear() { rows.clear() }
        override suspend fun count(): Int = rows.size
        fun all() = rows.values.toList()
    }

    private class ThrowingDao : RecentProjectDao {
        override suspend fun getRecent(limit: Int): List<RecentProjectEntity> =
            throw IllegalStateException("simulated DB corruption")
        override suspend fun getById(id: String): RecentProjectEntity? =
            throw IllegalStateException("simulated DB corruption")
        override suspend fun insert(entity: RecentProjectEntity) =
            throw IllegalStateException("simulated DB corruption")
        override suspend fun insertAll(entities: List<RecentProjectEntity>) =
            throw IllegalStateException("simulated DB corruption")
        override suspend fun delete(id: String) =
            throw IllegalStateException("simulated DB corruption")
        override suspend fun clear() = Unit
        override suspend fun count(): Int = throw IllegalStateException("simulated DB corruption")
    }

    private class RecordingRepo : ProjectRepository {
        var saves = 0
        var deletes = 0
        override suspend fun load(id: String) = Result.Success(blankProject(id, "Project $id"))
        override suspend fun save(project: MBeatProject): Result<Unit> { saves++; return Result.Success(Unit) }
        override suspend fun listRecent(limit: Int) = Result.Success(emptyList<MBeatProject>())
        override suspend fun delete(id: String): Result<Unit> { deletes++; return Result.Success(Unit) }
    }
}
