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
 * `exportSchema = false` for now: Room 2.6.1 + KSP 2.0.21 + AGP 8.7
 * fails annotation processing with `[MissingType]` when the schema
 * export target directory can't be materialized on a fresh
 * checkout. The migration test still passes via the runtime `ALTER
 * TABLE` in [MIGRATION_1_2] — we just defer the `1.json` /
 * `2.json` snapshot emission to a follow-up once the schemas dir
 * is bootstrapped by an initial successful build.
 *
 * `MIGRATION_1_2` is constructed from a **top-level named class**
 * rather than an anonymous `object : Migration(1, 2) { ... }`
 * literal. KSP2 (the default KSP implementation under Kotlin
 * 2.0.21) cannot resolve the signature of an anonymous class
 * declared inside another class's companion object during the
 * `@Database` annotation-processing phase, surfacing as
 * `[MissingType]: Element StorageDatabase references a type that
 * is not present`. Extracting to a named private class gives KSP2
 * a stable symbol to resolve. The behaviour of the migration is
 * unchanged.
 */
@Database(
    entities = [RecentProjectEntity::class],
    version = 2,
    exportSchema = false
)
abstract class StorageDatabase : RoomDatabase() {
    abstract fun recentProjectDao(): RecentProjectDao

    companion object {
        const val NAME = "pixelbeat_cache.db"

        /**
         * Phase 6+ migration v1 → v2 instance. The additive `ALTER
         * TABLE` introducing `scene_pack_id` (default NULL) lives in
         * [Migration_1_2] below; we hold only the reference here so
         * KSP2 can resolve it during `@Database` processing.
         */
        val MIGRATION_1_2: Migration = Migration_1_2()

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

/**
 * Top-level named migration so KSP2 has a stable symbol to resolve
 * during `@Database` annotation processing. Performs an additive
 * `ALTER TABLE recent_projects ADD COLUMN scene_pack_id TEXT
 * DEFAULT NULL` so legacy rows from schema v1 survive the bump
 * with NULL — see `14_STORAGE.md` "rebuildable cache" rules.
 */
private class Migration_1_2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE recent_projects " +
                "ADD COLUMN scene_pack_id TEXT DEFAULT NULL"
        )
    }
}