package com.trashmuppet.pixelbeat.storage.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for the rebuildable cache layer per `14_STORAGE.md`.
 *
 * Schema v1: a single `recent_projects` table holding metadata + JSON
 * blobs. The DB is **always rebuildable** — a wiped or missing file
 * simply drops `CachingProjectRepository.listRecent()` back to a
 * direct filesystem walk.
 *
 * `exportSchema = false` keeps the build simple; promote to `true`
 * once a second version lands in.
 */
@Database(
    entities = [RecentProjectEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StorageDatabase : RoomDatabase() {
    abstract fun recentProjectDao(): RecentProjectDao

    companion object {
        const val NAME = "pixelbeat_cache.db"

        fun build(context: Context): StorageDatabase =
            Room.databaseBuilder(context, StorageDatabase::class.java, NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
