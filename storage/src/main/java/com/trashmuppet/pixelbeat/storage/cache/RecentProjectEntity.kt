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
     * visualisation should boot into. Nullable so legacy rows from
     * schema v1 survive the additive MIGRATION_1_2 with NULL.
     *
     * Note: Room 2.6.x + KSP 2.0.21 has a regression where
     * `defaultValue = "NULL"` on a nullable Kotlin property
     * invalidates the entity class during annotation processing,
     * surfacing as `ksp [MissingType]` on the @Database element. The
     * column is null-defaulted by virtue of being `String? = null`
     * — Room emits `DEFAULT NULL` automatically for new columns of
     * this shape.
     */
    @ColumnInfo(name = "scene_pack_id")
    val scenePackId: String? = null
)
