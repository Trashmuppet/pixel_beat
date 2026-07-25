package com.trashmuppet.pixelbeat.storage.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for `recent_projects` (per `14_STORAGE.md`).
 *
 * Read paths return rows ordered by `last_modified_ms DESC` so the
 * blurbs match `StorageProjectRepository.listRecent()` semantics.
 */
@Dao
interface RecentProjectDao {

    @Query("SELECT * FROM recent_projects ORDER BY last_modified_ms DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<RecentProjectEntity>

    @Query("SELECT * FROM recent_projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecentProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecentProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<RecentProjectEntity>)

    @Query("DELETE FROM recent_projects WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM recent_projects")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM recent_projects")
    suspend fun count(): Int
}
