package com.trashmuppet.pixelbeat.storage.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for the rebuildable cache layer per `14_STORAGE.md`.
 *
 * Schema v1 → v2 adds `recent_projects.scene_pack_id` (nullable TEXT,
 * default null) so the Project browser can deep-link a project's
 * visualizer to a `.mbscene` pack. The DB is **always rebuildable**
 * — a wiped or missing file simply drops
 * `CachingProjectRepository.listRecent()` back to a direct
 * filesystem walk.
 *
 * `exportSchema = true` enables Room to emit `1.json` / `2.json` in
 * the gradle KSP `room.schemaLocation` directory so the migration
 * test in [StorageDatabaseMigrationTest] can validate v1 → v2 with
 * `MigrationTestHelper`. Migration is still **additive + safe** even
 * on devices where `MIGRATION_1_2` fails to run — see
 * `fallbackToDestructiveMigration()` guardrail.
 */
@Database(
    entities = [RecentProjectEntity::class],
    version = 2,
    exportSchema = true
)
abstract class StorageDatabase : RoomDatabase() {
    abstract fun recentProjectDao(): RecentProjectDao

    companion object {
        const val NAME = "pixelbeat_cache.db"

        /**
         * Phase 6+ migration v1 → v2. Performs an additive
         * `ALTER TABLE` to introduce the `scene_pack_id` column with
         * default `NULL`. Existing rows survive intact.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE recent_projects " +
                        "ADD COLUMN scene_pack_id TEXT DEFAULT NULL"
                )
            }
        }

        fun build(context: Context): StorageDatabase =
            Room.databaseBuilder(context, StorageDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                // The DB is rebuildable off the .mbeat filesystem source of
                // truth, so destructive migration is a last-resort guardrail
                // — never the default path. Survivors win either way.
                .fallbackToDestructiveMigration()
                .build()
    }
}
