package com.trashmuppet.pixelbeat.core.model

import kotlinx.serialization.Serializable

/**
 * The authoritative project document for Monochrome Beat.
 *
 * Per `14_STORAGE.md` this is the **source of truth**. Room and any other
 * cache layer are rebuildable from this document; the document is never
 * derived from them.
 *
 * Persisted as versioned UTF-8 JSON (.mbeat). Bumping `schema` requires a
 * defined migration per `14_STORAGE.md` ("Explicit migrations").
 */
@Serializable
data class MBeatProject(
    val version: Int = MBEAT_CURRENT_VERSION,
    val schema: String = MBEAT_SCHEMA,
    val projectId: String,
    val name: String,
    val bpm: Int,
    val swing: Double = 0.0,
    val tracks: List<Track>
) {
    companion object {
        const val MBEAT_CURRENT_VERSION = 1
        const val MBEAT_SCHEMA = "mbeat-v1"
    }
}

@Serializable
data class Track(
    val id: String,
    val steps: List<Int>
)
