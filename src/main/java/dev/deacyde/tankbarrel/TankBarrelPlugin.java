package dev.deacyde.tankbarrel;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TankBarrelPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Cross-system communication: player UUID -> pending action from /barrel command
    public static final ConcurrentHashMap<UUID, TankBarrelCommand.Action> pendingActions = new ConcurrentHashMap<>();

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
