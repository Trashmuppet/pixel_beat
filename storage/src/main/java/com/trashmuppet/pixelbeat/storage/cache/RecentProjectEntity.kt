package com.trashmuppet.pixelbeat.storage.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rebuildable recent-projects cache entry per `14_STORAGE.md`.
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
    @ColumnInfo(name = "project_json") val projectJson: String
)
