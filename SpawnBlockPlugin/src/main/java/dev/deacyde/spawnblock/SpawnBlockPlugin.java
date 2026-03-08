package dev.deacyde.spawnblock;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.modules.item.ItemModule;

import javax.annotation.Nonnull;

public class SpawnBlockPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static SpawnBlockPlugin instance;

    private ComponentType<ChunkStore, SpawnBlockComponent> spawnBlockComponent;
    private final SpawnBlockRegistry registry = new SpawnBlockRegistry();

    public SpawnBlockPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("[SpawnBlock] Loaded v2 — " + this.getManifest().getVersion());
    }

    @Override
    protected void setup() {
        // Register the ChunkStore component — links JSON key "SpawnBlockData" to SpawnBlockComponent
        this.spawnBlockComponent = this.getChunkStoreRegistry()
            .registerComponent(SpawnBlockComponent.class, "SpawnBlockData", SpawnBlockComponent.CODEC);
        LOGGER.atInfo().log("[SpawnBlock] Registered ChunkStore component SpawnBlockData");

        // Placement system: fires when a block with SpawnBlockData is placed or broken
        this.getChunkStoreRegistry().registerSystem(new SpawnBlockPlacedSystem());

        // Ticking system: ticks all SpawnBlock blocks every N seconds
        this.getChunkStoreRegistry().registerSystem(new SpawnBlockTickingSystem());

        // Command: /spawnblock <mob> [rate] [max] [radius]
        this.getCommandRegistry().registerCommand(new SpawnBlockCommand());

        LOGGER.atInfo().log("[SpawnBlock] Setup complete — place a Spawn_Block to start spawning mobs");
    }

    @Override
    protected void start() {
        super.start();
        // Diagnostic: check item registry AFTER ItemModule is fully enabled
        for (String id : new String[]{"Block_Spawn_Block","Deacyde:Block_Spawn_Block","Deacyde.SpawnBlock:Block_Spawn_Block"}) {
            LOGGER.atInfo().log("[SpawnBlock] ItemModule.exists(\"" + id + "\") = " + ItemModule.exists(id));
        }
    }

    public static SpawnBlockPlugin get() { return instance; }
    /** @deprecated Use get() */
    public static SpawnBlockPlugin getInstance() { return instance; }

    public ComponentType<ChunkStore, SpawnBlockComponent> getSpawnBlockComponent() {
        return spawnBlockComponent;
    }

    public SpawnBlockRegistry getRegistry() { return registry; }
}
