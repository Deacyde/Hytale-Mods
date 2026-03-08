package dev.deacyde.worldedit;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.MouseButtonType;

/**
 * Detects right-click (UseBlock interaction) when the WorldEdit Wand is held and sets Pos2.
 * Cancels the use-block action to prevent default behavior.
 */
public class WandUseBlockSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public WandUseBlockSystem() {
        super(UseBlockEvent.Pre.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> buffer, UseBlockEvent.Pre event) {
        InteractionContext ctx = event.getContext();
        if (ctx == null) return;

        ItemStack item = ctx.getHeldItem();
        LOGGER.atInfo().log("[WorldEdit] UseBlockEvent.Pre: item="
            + (item != null ? item.getItemId() : "null")
            + " target=" + event.getTargetBlock());

        if (item == null || !item.getItemId().endsWith(WorldEditPlugin.WAND_ITEM_ID)) return;

        Vector3i target = event.getTargetBlock();
        if (target == null) return;

        PlayerRef player = chunk.getComponent(index, PlayerRef.getComponentType());
        if (player == null) return;

        WandSession session = WorldEditPlugin.get().getSession(player.getUuid());
        WorldEditPlugin.get().setPositionFromClick(session, player, target, MouseButtonType.Right);
        event.setCancelled(true);
    }
}
