package dev.deacyde.worldedit;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.MouseButtonType;

/**
 * Handles DamageBlockEvent (left-click on a block) via the ECS EntityEventSystem.
 * Fires per player entity — we can get the PlayerRef directly.
 * When the player holds the WorldEdit Wand and left-clicks a block, sets Pos1.
 */
public class WandBlockDamageSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public WandBlockDamageSystem() {
        super(DamageBlockEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Match all player entities
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> buffer, DamageBlockEvent event) {
        ItemStack item = event.getItemInHand();
        LOGGER.atInfo().log("[WorldEdit] DamageBlockEvent: item="
            + (item != null ? item.getItemId() : "null")
            + " target=" + event.getTargetBlock());

        if (item == null || !item.getItemId().endsWith(WorldEditPlugin.WAND_ITEM_ID)) return;

        Vector3i target = event.getTargetBlock();
        if (target == null) return;

        PlayerRef player = chunk.getComponent(index, PlayerRef.getComponentType());
        if (player == null) return;

        WandSession session = WorldEditPlugin.get().getSession(player.getUuid());
        WorldEditPlugin.get().setPositionFromClick(session, player, target, MouseButtonType.Left);
        event.setCancelled(true);
    }
}
