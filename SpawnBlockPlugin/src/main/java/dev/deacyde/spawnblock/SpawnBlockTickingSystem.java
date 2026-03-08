// SpawnBlock v2 — ChunkStore ticking system (pattern from Landmark-0.0.5)
package dev.deacyde.spawnblock;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.time.Instant;

public class SpawnBlockTickingSystem extends EntityTickingSystem<ChunkStore> {

    private static final Query<ChunkStore> QUERY = Query.and(
        BlockSection.getComponentType(),
        ChunkSection.getComponentType()
    );

    @Override
    public Query<ChunkStore> getQuery() {
        return QUERY;
    }

    @Override
    public void tick(float delta, int index, ArchetypeChunk<ChunkStore> chunk,
                     Store<ChunkStore> store, CommandBuffer<ChunkStore> buffer) {

        BlockSection blockSection = (BlockSection) chunk.getComponent(index, BlockSection.getComponentType());
        if (blockSection == null || blockSection.getTickingBlocksCountCopy() == 0) return;

        ChunkSection chunkSection = (ChunkSection) chunk.getComponent(index, ChunkSection.getComponentType());
        if (chunkSection == null) return;

        BlockComponentChunk blockComponentChunk = (BlockComponentChunk) buffer.getComponent(
            chunkSection.getChunkColumnReference(), BlockComponentChunk.getComponentType()
        );
        if (blockComponentChunk == null) return;

        ChunkStore chunkStore = (ChunkStore) buffer.getExternalData();
        World world = chunkStore.getWorld();
        WorldTimeResource timeRes = (WorldTimeResource) world.getEntityStore().getStore()
            .getResource(WorldTimeResource.getResourceType());

        blockSection.forEachTicking(blockComponentChunk, buffer, chunkSection.getY(),
            (blockChunk, buf, x, y, z, idx) -> {
                Ref<ChunkStore> blockRef = blockChunk.getEntityReference(ChunkUtil.indexBlockInColumn(x, y, z));
                if (blockRef == null) return BlockTickStrategy.IGNORED;

                SpawnBlockComponent comp = (SpawnBlockComponent) buf.getComponent(
                    blockRef, SpawnBlockPlugin.get().getSpawnBlockComponent()
                );
                if (comp == null) return BlockTickStrategy.IGNORED;

                int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkSection.getX(), x);
                int worldY = ChunkUtil.worldCoordFromLocalCoord(chunkSection.getY(), y);
                int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkSection.getZ(), z);

                comp.onTick(worldX, worldY, worldZ, world);

                Instant nextTick = comp.getNextScheduledTick(timeRes);
                if (nextTick != null) {
                    blockSection.scheduleTick(ChunkUtil.indexBlock(x, y, z), nextTick);
                }
                return BlockTickStrategy.SLEEP;
            }
        );
    }
}
