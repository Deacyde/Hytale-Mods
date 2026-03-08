package dev.deacyde.worldedit;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Handles wand clicks (BreakBlockEvent) and sets pos1/pos2 via toggle:
 *  - Click 1: sets pos1, nextClickIsPos2 = true
 *  - Click 2: sets pos2, nextClickIsPos2 = false
 *  - Click 3: sets pos1 again (clears pos2), nextClickIsPos2 = true
 *  ...
 * This avoids needing to detect Primary vs Secondary interaction type, which
 * is inaccessible from within BreakBlockEvent.
 */
public class WandBreakBlockSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public WandBreakBlockSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> buffer, BreakBlockEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || !item.getItemId().endsWith(WorldEditPlugin.WAND_ITEM_ID)) return;

        Vector3i target = event.getTargetBlock();
        if (target == null) return;

        PlayerRef player = chunk.getComponent(index, PlayerRef.getComponentType());
        if (player == null) return;

        WandSession session = WorldEditPlugin.get().getSession(player.getUuid());

        // Debounce: suppress repeated fires from holding the mouse button.
        // Only process if the target block changed OR 500ms has passed since last trigger.
        long now = System.currentTimeMillis();
        boolean sameTarget = target.equals(session.lastToggleTarget);
        boolean inCooldown = (now - session.lastToggleMsEpoch) < 500L;
        if (sameTarget && inCooldown) {
            event.setCancelled(true);
            return;
        }
        session.lastToggleTarget = new Vector3i(target.x, target.y, target.z);
        session.lastToggleMsEpoch = now;

        // Toggle: first click = pos1, second = pos2, third = pos1, etc.
        MouseButtonType btn;
        if (!session.nextClickIsPos2) {
            btn = MouseButtonType.Left;          // pos1
            session.nextClickIsPos2 = true;
            // NOTE: do NOT clear pos2 here — that would break copy/paste after setting both positions
        } else {
            btn = MouseButtonType.Right;         // pos2
            session.nextClickIsPos2 = false;
        }

        LOGGER.atInfo().log("[WorldEdit] Wand click: uuid=" + player.getUuid()
            + " target=" + target + " setting=" + (btn == MouseButtonType.Left ? "pos1" : "pos2"));

        WorldEditPlugin.get().setPositionFromClick(session, player, target, btn);
        event.setCancelled(true);
    }
}
