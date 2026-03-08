package dev.deacyde.gunturret;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class GunTurretPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public GunTurretPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("[GunTurret] Loaded v" + this.getManifest().getVersion());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("[GunTurret] Registering /turret command");
        this.getCommandRegistry().registerCommand(new GunTurretCommand());

        LOGGER.atInfo().log("[GunTurret] Registering turret ticking system");
        this.getEntityStoreRegistry().registerSystem(new TurretTickingSystem());
    }
}
