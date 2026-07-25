package com.trashmuppet.pixelbeat.storage.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rebuildable recent-projects cache entry per `14_STORAGE.md`.
 *
 * Schema v2 adds `scene_pack_id`. v1 had only `id`, `name`, `bpm`,
 * `last_modified_ms`, `project_json`. Migration v1→v2 is declared in
 * [StorageDatabase] as a default-null column addition so existing
 * rows survive the schema bump without data loss.
 *
 * Stores metadata (`name`, `bpm`, `lastModifiedMs`) plus a JSON blob
 * (`projectJson`) of the full `MBeatProject` so `load(id)` and
 * `listRecent()` can be answered without touching the filesystem.
 *
 * The Room DB is **never authoritative**: deleting the database file
 * produces a transparent cache miss and the next
 * `CachingProjectRepository.listRecent()` rebuilds it from
 * `filesDir/Projects/*.mbeat`.
 */
@Entity(tableName = "recent_projects")
data class RecentProjectEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "bpm") val bpm: Float,
    @ColumnInfo(name = "last_modified_ms") val lastModifiedMs: Long,
    @ColumnInfo(name = "project_json") val projectJson: String,
    /**
     * Phase 6+: tracks which `.mbscene` pack this project's
     * visualisation should boot into. Default-null for legacy rows
     * (MIGRATION_1_2 sets it to NULL).
     */
    @ColumnInfo(name = "scene_pack_id", defaultValue = "NULL")
    val scenePackId: String? = null
)
