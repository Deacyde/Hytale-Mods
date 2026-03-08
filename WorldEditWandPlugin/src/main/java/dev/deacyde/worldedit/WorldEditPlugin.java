package dev.deacyde.worldedit;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorldEditPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static WorldEditPlugin instance;

    static final String WAND_ITEM_ID = "WE_Wand";
    static final int MAX_BLOCKS = 100_000;

    private final Map<UUID, WandSession> sessions = new ConcurrentHashMap<>();

    public WorldEditPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("[WorldEdit] Plugin loaded v" + this.getManifest().getVersion());
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new WandCommand(this));

        // ECS system approach: DamageBlockEvent fires per block damage in EntityStore ECS
        this.getEntityStoreRegistry().registerSystem(new WandBlockDamageSystem());

        LOGGER.atInfo().log("[WorldEdit] Setup complete. Use /we wand to get the wand.");
    }

    @Override
    protected void start() {
        super.start();
        // After all modules are loaded, register on Universe's root event registry.
        // InteractionModule dispatches PlayerMouseButtonEvent on its own registry which
        // bubbles UP to Universe's registry — but NOT sideways to our plugin's registry.
        // Registering on Universe.get().getEventRegistry() catches events from all modules.
        try {
            Universe.get().getEventRegistry().register(PlayerMouseButtonEvent.class, this::onMouseButton);
            LOGGER.atInfo().log("[WorldEdit] Registered PlayerMouseButtonEvent on Universe registry.");
        } catch (Throwable t) {
            LOGGER.atWarning().log("[WorldEdit] Failed to register MouseButtonEvent on Universe: " + t);
        }
        try {
            Universe.get().getEventRegistry().registerGlobal(PlayerInteractEvent.class, this::onPlayerInteract);
            LOGGER.atInfo().log("[WorldEdit] Registered PlayerInteractEvent on Universe registry.");
        } catch (Throwable t) {
            LOGGER.atWarning().log("[WorldEdit] Failed to register PlayerInteractEvent on Universe: " + t);
        }
    }

    public static WorldEditPlugin get() {
        return instance;
    }

    public WandSession getSession(UUID playerUuid) {
        return sessions.computeIfAbsent(playerUuid, k -> new WandSession());
    }

    /** Handles PlayerMouseButtonEvent from Universe's root event registry. */
    private void onMouseButton(PlayerMouseButtonEvent event) {
        if (event.getMouseButton().state != MouseButtonState.Pressed) return;

        Item item = event.getItemInHand();
        LOGGER.atInfo().log("[WorldEdit] MouseButton: item="
            + (item != null ? item.getId() : "null")
            + " btn=" + event.getMouseButton().mouseButtonType
            + " target=" + event.getTargetBlock());

        if (item == null || !item.getId().endsWith(WAND_ITEM_ID)) return;

        Vector3i target = event.getTargetBlock();
        if (target == null) return;

        PlayerRef player = event.getPlayerRefComponent();
        if (player == null) return;

        WandSession session = getSession(player.getUuid());
        setPositionFromClick(session, player, target, event.getMouseButton().mouseButtonType);
        event.setCancelled(true);
    }

    /** Handles PlayerInteractEvent from Universe's root event registry (backup). */
    private void onPlayerInteract(PlayerInteractEvent event) {
        var item = event.getItemInHand();
        LOGGER.atInfo().log("[WorldEdit] PlayerInteractEvent: item="
            + (item != null ? item.getItemId() : "null")
            + " action=" + event.getActionType()
            + " target=" + event.getTargetBlock());
    }

    void setPositionFromClick(WandSession session, PlayerRef player, Vector3i target, MouseButtonType btn) {
        if (btn == MouseButtonType.Left) {
            session.pos1 = new Vector3i(target.x, target.y, target.z);
            player.sendMessage(Message.raw(
                "§a[WE] Pos1 set to §f(" + target.x + ", " + target.y + ", " + target.z + ")"
                + (session.hasSelection() ? " §7— §e" + session.getTotalBlocks() + " blocks" : "")
            ));
        } else if (btn == MouseButtonType.Right) {
            session.pos2 = new Vector3i(target.x, target.y, target.z);
            player.sendMessage(Message.raw(
                "§b[WE] Pos2 set to §f(" + target.x + ", " + target.y + ", " + target.z + ")"
                + (session.hasSelection() ? " §7— §e" + session.getTotalBlocks() + " blocks" : "")
            ));
        }
    }
}
