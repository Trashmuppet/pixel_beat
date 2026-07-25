package com.trashmuppet.pixelbeat.scene.api

/**
 * Registry of all available scene packs.
 *
 * Uses [Class.forName] to instantiate [WarehouseScene] without a direct
 * dependency on [:scene-warehouse], keeping [:scene-api] decoupled.
 *
 * Pro packs ([requiresPro] = true) throw [UnsupportedOperationException]
 * from their [ScenePack.sceneFactory] until the implementation modules
 * ship in a future phase.
 */
object ScenePackRegistry {
    val all: List<ScenePack> = listOf(
        ScenePack(
            packId = "warehouse",
            displayName = "Warehouse",
            description = "The classic monochrome environment.",
            requiresPro = false,
            sceneFactory = {
                Class.forName("com.trashmuppet.pixelbeat.scene.warehouse.WarehouseScene")
                    .getDeclaredConstructor()
                    .newInstance() as Scene
            }
        ),
        ScenePack(
            packId = "neon",
            displayName = "Neon City",
            description = "Premium lights and visualizers.",
            requiresPro = true,
            // Reflective instantiation keeps [:scene-api] decoupled from
            // [:scene-neon]. Pro-gating via ProGate composable ensures
            // this factory is only invoked when the user is entitled.
            sceneFactory = {
                Class.forName("com.trashmuppet.pixelbeat.scene.neon.NeonScene")
                    .getDeclaredConstructor()
                    .newInstance() as Scene
            }
        ),
        ScenePack(
            packId = "void",
            displayName = "The Void",
            description = "Minimal deep space isolation.",
            requiresPro = true,
            // Reflective instantiation keeps [:scene-api] decoupled from
            // [:scene-void]. Pro-gating via ProGate composable ensures
            // this factory is only invoked when the user is entitled.
            sceneFactory = {
                Class.forName("com.trashmuppet.pixelbeat.scene.darkvoid.VoidScene")
                    .getDeclaredConstructor()
                    .newInstance() as Scene
            }
        )
    )
}
