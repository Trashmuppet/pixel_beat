package com.trashmuppet.pixelbeat.core.timeline

import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.model.ProjectSeed
import java.security.MessageDigest

/**
 * Canonical SHA-256 long-form hash of a project's *content*.
 *
 * Used in `TimelineCompiler` to seal determinism: identical input on
 * any machine / build must produce the same bytes. The hash is also
 * surfaced as `CompiledTimeline.projectHash` for golden-fixture tests.
 *
 * `seed` is intentionally NOT included — the seed is recorded
 * separately so transport can re-replay rolls deterministically if the
 * scene side ever consumes RNG.
 */
object ProjectHasher {

    fun contentHashOf(project: MBeatProject): Long {
        val canonical = canonicalString(project)
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
        // Take the leading 8 bytes as a long for compact in-memory equality.
        var h = 0L
        for (i in 0..7) h = (h shl 8) or (bytes[i].toLong() and 0xFF)
        return h
    }

    fun deriveSeed(project: MBeatProject): ProjectSeed {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(("seed:" + canonicalString(project)).toByteArray(Charsets.UTF_8))
        // Take the *trailing* 8 bytes so this hash differs from `contentHashOf`.
        var h = 0L
        for (i in 56..63) h = (h shl 8) or (bytes[i].toLong() and 0xFF)
        return ProjectSeed(h)
    }

    private fun canonicalString(project: MBeatProject): String =
        buildString {
            append(project.id).append('|')
            append(project.bpm.toBits().toString()).append('|')
            append(project.arrangement.patternChain.joinToString(",")).append('|')
            append(project.arrangement.loopBars.startBar).append(',')
                .append(project.arrangement.loopBars.endBar).append('|')
            project.patterns.forEach { p ->
                append(p.id).append('@').append(p.lengthSteps).append(':')
                p.tracks.forEach { t ->
                    append(t.id).append('(').append(t.kind.name).append(',')
                        .append(t.volumeDb.toBits()).append(',')
                        .append(t.panCb.toBits()).append(',')
                        .append(if (t.mute) "M" else "U").append(')').append('|')
                    t.steps.forEach { append(if (it) '1' else '0') }
                    append(';')
                }
                append("||")
            }
        }
}
