package dev.deacyde.spawnblock;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class SpawnBlockPlacedSystem extends RefSystem<ChunkStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public Query<ChunkStore> getQuery() {
        // Single-component query — fire whenever SpawnBlockData is present
        return SpawnBlockPlugin.get().getSpawnBlockComponent();
    }

    @Override
    public void onEntityAdded(Ref<ChunkStore> blockRef, AddReason reason,
                              Store<ChunkStore> store, CommandBuffer<ChunkStore> buffer) {

        LOGGER.atInfo().log("[SpawnBlock] onEntityAdded FIRED — reason=" + reason);

        SpawnBlockComponent comp = (SpawnBlockComponent) buffer.getComponent(
            blockRef, SpawnBlockPlugin.get().getSpawnBlockComponent()
        );
        if (comp == null) {
            LOGGER.atWarning().log("[SpawnBlock] SpawnBlockComponent is null after add — skipping");
            return;
        }

        // Apply pending config from /spawnblock command
        SpawnBlockRegistry registry = SpawnBlockPlugin.get().getRegistry();
        if (registry.hasPending()) {
            registry.applyPending(comp);
            LOGGER.atInfo().log("[SpawnBlock] Config applied: mob=" + comp.getMobType()
                + " rate=" + comp.getSpawnRate() + "s max=" + comp.getMaxMobs()
                + " radius=" + comp.getSpawnRadius());
        } else {
            LOGGER.atInfo().log("[SpawnBlock] Block placed with defaults (run /spawnblock <mob> first)");
        }

        // === SCHEDULE THE FIRST TICK (exact Landmark pattern) ===
        // Get WorldTimeResource from the world's EntityStore
        ChunkStore chunkStore = (ChunkStore) buffer.getExternalData();
        WorldTimeResource timeRes = (WorldTimeResource) chunkStore.getWorld()
            .getEntityStore().getStore()
            .getResource(WorldTimeResource.getResourceType());

        // Get block local coordinates from BlockStateInfo
        BlockModule.BlockStateInfo blockStateInfo = (BlockModule.BlockStateInfo) buffer.getComponent(
            blockRef, BlockModule.BlockStateInfo.getComponentType()
        );
        if (blockStateInfo == null) {
            LOGGER.atWarning().log("[SpawnBlock] BlockStateInfo is null — cannot schedule tick");
            return;
        }

        int localX = ChunkUtil.xFromBlockInColumn(blockStateInfo.getIndex());
        int localY = ChunkUtil.yFromBlockInColumn(blockStateInfo.getIndex());
        int localZ = ChunkUtil.zFromBlockInColumn(blockStateInfo.getIndex());

        // Get BlockChunk (holds BlockSection[] for the column) and schedule tick
        BlockChunk blockChunk = (BlockChunk) buffer.getComponent(
            blockStateInfo.getChunkRef(), BlockChunk.getComponentType()
        );
        if (blockChunk == null) {
            LOGGER.atWarning().log("[SpawnBlock] BlockChunk is null — cannot schedule tick");
            return;
        }

        BlockSection blockSection = blockChunk.getSectionAtBlockY(localY);
        if (blockSection == null) {
            LOGGER.atWarning().log("[SpawnBlock] BlockSection is null — cannot schedule tick");
            return;
        }

        blockSection.scheduleTick(ChunkUtil.indexBlock(localX, localY, localZ),
            comp.getNextScheduledTick(timeRes));

        LOGGER.atInfo().log("[SpawnBlock] First tick scheduled for block at local "
            + localX + "," + localY + "," + localZ);
    }

    @Override
    public void onEntityRemove(Ref<ChunkStore> blockRef, RemoveReason reason,
                               Store<ChunkStore> store, CommandBuffer<ChunkStore> buffer) {
        if (reason == RemoveReason.UNLOAD) return;
        LOGGER.atInfo().log("[SpawnBlock] Block removed");
    }
}
