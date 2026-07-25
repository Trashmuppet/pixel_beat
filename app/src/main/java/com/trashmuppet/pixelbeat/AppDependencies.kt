package com.trashmuppet.pixelbeat

import android.content.Context
import com.trashmuppet.pixelbeat.audio.AudioEngine
import com.trashmuppet.pixelbeat.billing.GooglePlayPremiumManager
import com.trashmuppet.pixelbeat.core.common.AppDispatchers
import com.trashmuppet.pixelbeat.core.common.DefaultAppDispatchers
import com.trashmuppet.pixelbeat.core.export.AudioFramesSource
import com.trashmuppet.pixelbeat.core.export.CompatibilityExporter
import com.trashmuppet.pixelbeat.core.export.ExportPipeline
import com.trashmuppet.pixelbeat.core.timeline.RealtimeTransport
import com.trashmuppet.pixelbeat.core.timeline.TestRealtimeTransport
import com.trashmuppet.pixelbeat.feature.export.MediaExporter
import com.trashmuppet.pixelbeat.premium.PremiumManager
import com.trashmuppet.pixelbeat.scene.runtime.AnimationSystem
import com.trashmuppet.pixelbeat.scene.warehouse.WarehouseScene
import com.trashmuppet.pixelbeat.storage.CachingProjectRepository
import com.trashmuppet.pixelbeat.storage.ProjectRepository
import com.trashmuppet.pixelbeat.storage.StorageProjectRepository
import com.trashmuppet.pixelbeat.storage.cache.StorageDatabase

/**
 * Application-scoped dependency container (Phase 3 wiring).
 *
 * Phase 5 wires a real `CompatibilityExporter` over a production
 * `ExportPipeline` (real WavEncoder / GifEncoder / Mp4MediaCodecEncoder)
 * and a `DefaultAudioFramesSource` that drives the on-device
 * `AudioEngine` + `TimelineCompiler`. ADR-006 plans a code-generated
 * DI graph for Phase 6; this hand-rolled container stays until then.
 */
class AppDependencies(applicationContext: Context) {
    val dispatchers: AppDispatchers = DefaultAppDispatchers()
    private val rawRepository: ProjectRepository =
        StorageProjectRepository(applicationContext, dispatchers)
    /** File-system backed `.mbeat` repo wrapped with the rebuildable Room cache
     *  per `docs/14_STORAGE.md`. Feature modules see this interface only. */
    val projectRepository: ProjectRepository =
        CachingProjectRepository.wrap(rawRepository, StorageDatabase.build(applicationContext))
    val transport: RealtimeTransport = TestRealtimeTransport()

    /** Phase 5 audio source. Streams `renderOffline(float[s])` from the engine. */
    val audioFrames: AudioFramesSource = DefaultAudioFramesSource(AudioEngine.instance)

    /** Phase 5 scene driver. WarehouseScene wired per `09_SCENE_SYSTEM.md` reference impl. */
    private val scene: AnimationSystem = AnimationSystem(WarehouseScene())

    /** Phase 5 export orchestrator. */
    private val exportPipeline: ExportPipeline = ExportPipeline(audioFrames, scene)

    /** Phase 5 `MediaExporter` — backed by the real Wav/Gif/Mp4 encoders. */
    val exporter: MediaExporter = CompatibilityExporter(exportPipeline)

    /** Phase 7 billing manager. Cold-start cache delivered immediately; Play query async.
     *  Debug builds bypass Play Billing entirely — always returns Pro. */
    val premiumManager: PremiumManager = GooglePlayPremiumManager(
        applicationContext,
        debugBypass = BuildConfig.DEBUG
    )
}
