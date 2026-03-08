package dev.deacyde.worldedit;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseMotionEvent;
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
    static final int MAX_BLOCKS = 500_000;
    /** Skip saving undo buffer for selections larger than this — too slow to pre-read. */
    static final int UNDO_SKIP_THRESHOLD = 50_000;

    private final Map<UUID, WandSession> sessions = new ConcurrentHashMap<>();
    @SuppressWarnings("unused")
    private EventRegistration<?, ?> mouseButtonRegistration;
    @SuppressWarnings("unused")
    private EventRegistration<?, ?> mouseMotionRegistration;

    public WorldEditPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("[WorldEdit] Plugin loaded v" + this.getManifest().getVersion());
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new WandCommand(this));

        // BreakBlockEvent fires via ComponentAccessor.invoke(Ref, Event) — entity-specific dispatch
        // that properly reaches plugin EntityEventSystems. Handles both left-click (pos1) and
        // right-click (pos2) by inspecting the active InteractionChain type.
        this.getEntityStoreRegistry().registerSystem(new WandBreakBlockSystem());

        LOGGER.atInfo().log("[WorldEdit] Setup complete. Use /we wand to get the wand.");
    }

    @Override
    protected void start() {
        super.start();
        try {
            var bus = HytaleServer.get().getEventBus();
            // Verify class loader identity — both must use the server's classloader
            try {
                Class<?> serverClass = com.hypixel.hytale.server.core.plugin.PluginManager.class
                    .getClassLoader()
                    .loadClass("com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent");
                boolean sameClass = (serverClass == PlayerMouseButtonEvent.class);
                LOGGER.atInfo().log("[WorldEdit] ClassLoader check: serverClass=" + serverClass.getClassLoader()
                    + " ourClass=" + PlayerMouseButtonEvent.class.getClassLoader()
                    + " same=" + sameClass);
            } catch (Throwable cl) {
                LOGGER.atWarning().log("[WorldEdit] ClassLoader check failed: " + cl);
            }
            // Store registrations as fields to prevent GC
            mouseButtonRegistration = bus.register(PlayerMouseButtonEvent.class, this::onMouseButton);
            mouseMotionRegistration = bus.register(PlayerMouseMotionEvent.class, this::onMouseMotion);
            LOGGER.atInfo().log("[WorldEdit] Registered on EventBus. Known events: "
                + bus.getRegisteredEventClassNames());
        } catch (Throwable t) {
            LOGGER.atWarning().log("[WorldEdit] Failed to register on EventBus: " + t);
        }
    }

    public static WorldEditPlugin get() {
        return instance;
    }

    public WandSession getSession(UUID playerUuid) {
        return sessions.computeIfAbsent(playerUuid, k -> new WandSession());
    }

    /** Diagnostic: fires on any mouse movement — confirms EventBus works for our plugin. */
    private void onMouseMotion(PlayerMouseMotionEvent event) {
        // Only log once to avoid spam — remove after confirming events fire
    }

    /** Handles PlayerMouseButtonEvent from the EventBus. */
    private void onMouseButton(PlayerMouseButtonEvent event) {
        // Unconditional log — if this never appears, the handler is never called
        LOGGER.atInfo().log("[WorldEdit] onMouseButton FIRED state=" + event.getMouseButton().state
            + " btn=" + event.getMouseButton().mouseButtonType
            + " item=" + (event.getItemInHand() != null ? event.getItemInHand().getId() : "null"));

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
