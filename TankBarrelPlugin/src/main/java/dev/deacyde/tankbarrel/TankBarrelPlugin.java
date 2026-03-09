package dev.deacyde.tankbarrel;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TankBarrelPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Bundles action + playerRef so ticking system never needs cross-store lookup
    public static class PendingAction {
        public final TankBarrelCommand.Action action;
        public final PlayerRef playerRef;
        public PendingAction(TankBarrelCommand.Action action, PlayerRef playerRef) {
            this.action = action;
            this.playerRef = playerRef;
        }
    }

    // Cross-system communication: player UUID -> pending action from /barrel command
    public static final ConcurrentHashMap<UUID, PendingAction> pendingActions = new ConcurrentHashMap<>();

    public TankBarrelPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("[TankBarrel] Loaded v" + this.getManifest().getVersion());
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new TankBarrelCommand());
        this.getEntityStoreRegistry().registerSystem(new TankBarrelTickingSystem());
        LOGGER.atInfo().log("[TankBarrel] Setup complete. Use /barrel to get the Tank Barrel.");
    }
}
