package dev.deacyde.tankbarrel;

import com.hypixel.hytale.builtin.deployables.component.DeployableComponent;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TankBarrelTickingSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String BARREL_ID = "Gun_Turret";
    private static final int LOAD_AMOUNT = 5;
    private static final int MAX_AMMO = 20;
    private static final long TNT_COOLDOWN_MS = 3000L;
    private static final long NUKE_COOLDOWN_MS = 8000L;
    private static final String TNT_SHELL_CONFIG = "Projectile_Config_Tank_Barrel_TNT_Shell";
    private static final String NUKE_SHELL_CONFIG = "Projectile_Config_Tank_Barrel_Nuke_Shell";

    // Per-barrel state keyed by identity hash of the barrel's Ref
    private final ConcurrentHashMap<Integer, TankBarrelState> barrelStates = new ConcurrentHashMap<>();

    private static final Query<EntityStore> QUERY = Archetype.of(new ComponentType[]{
        DeployableComponent.getComponentType(),
        TransformComponent.getComponentType()
    });

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void tick(float deltaTime, int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        DeployableComponent dc = chunk.getComponent(index, DeployableComponent.getComponentType());
        if (dc == null || dc.getConfig() == null) return;
        if (!BARREL_ID.equals(dc.getConfig().getId())) return;

        // Skip fast if nothing pending
        if (TankBarrelPlugin.pendingActions.isEmpty()) return;

        Ref<EntityStore> ownerRef = dc.getOwner();
        if (ownerRef == null || !ownerRef.isValid()) return;

        // Pick the first pending action — works for single-player.
        // PlayerRef is stored in PendingAction so no cross-store lookup needed.
        UUID firstKey = TankBarrelPlugin.pendingActions.keys().nextElement();
        TankBarrelPlugin.PendingAction pending = TankBarrelPlugin.pendingActions.remove(firstKey);
        if (pending == null) return;

        PlayerRef ownerPlayerRef = pending.playerRef;

        Ref<EntityStore> barrelRef = chunk.getReferenceTo(index);
        int barrelKey = System.identityHashCode(barrelRef);
        TankBarrelState state = barrelStates.computeIfAbsent(barrelKey, k -> new TankBarrelState());

        TransformComponent barrelTransform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (barrelTransform == null) return;

        switch (pending.action) {
            case LOAD_TNT:
                state.tntAmmo = Math.min(state.tntAmmo + LOAD_AMOUNT, MAX_AMMO);
                ownerPlayerRef.sendMessage(Message.raw("§a[Barrel] Loaded TNT shells: " + state.tntAmmo + "/" + MAX_AMMO));
                break;
            case LOAD_NUKE:
                state.nukeAmmo = Math.min(state.nukeAmmo + LOAD_AMOUNT, MAX_AMMO);
                ownerPlayerRef.sendMessage(Message.raw("§a[Barrel] Loaded Nuke shells: " + state.nukeAmmo + "/" + MAX_AMMO));
                break;
            case FIRE_TNT:
                fireShell(ownerRef, ownerPlayerRef, barrelTransform, state, store, commandBuffer, false);
                break;
            case FIRE_NUKE:
                fireShell(ownerRef, ownerPlayerRef, barrelTransform, state, store, commandBuffer, true);
                break;
            case SHOW_AMMO:
                ownerPlayerRef.sendMessage(Message.raw("§e[Barrel] Ammo: §fTNT=" + state.tntAmmo + " §cNuke=" + state.nukeAmmo));
                break;
        }
    }

    private void fireShell(Ref<EntityStore> ownerRef, PlayerRef ownerPlayerRef,
                           TransformComponent barrelTransform, TankBarrelState state,
                           Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                           boolean nuke) {
        long now = System.currentTimeMillis();

        if (nuke) {
            if (state.nukeAmmo <= 0) {
                ownerPlayerRef.sendMessage(Message.raw("§c[Barrel] No Nuke ammo! Use /barrel load nuke"));
                return;
            }
            if (now - state.lastNukeFireMs < NUKE_COOLDOWN_MS) {
                long remaining = (NUKE_COOLDOWN_MS - (now - state.lastNukeFireMs)) / 1000;
                ownerPlayerRef.sendMessage(Message.raw("§c[Barrel] Nuke shell on cooldown: " + remaining + "s left"));
                return;
            }
        } else {
            if (state.tntAmmo <= 0) {
                ownerPlayerRef.sendMessage(Message.raw("§c[Barrel] No TNT ammo! Use /barrel load tnt"));
                return;
            }
            if (now - state.lastTntFireMs < TNT_COOLDOWN_MS) {
                long remaining = (TNT_COOLDOWN_MS - (now - state.lastTntFireMs)) / 1000;
                ownerPlayerRef.sendMessage(Message.raw("§c[Barrel] TNT shell on cooldown: " + remaining + "s left"));
                return;
            }
        }

        String configId = nuke ? NUKE_SHELL_CONFIG : TNT_SHELL_CONFIG;
        ProjectileConfig cfg = ProjectileConfig.getAssetMap().getAsset(configId);
        if (cfg == null) {
            ownerPlayerRef.sendMessage(Message.raw("§c[Barrel] Shell config not found: " + configId));
            return;
        }

        // Fire away from owner: direction = barrel pos - player pos, elevated 30°
        Vector3d barrelPos = barrelTransform.getPosition();
        Vector3d spawnPos = new Vector3d(barrelPos.getX(), barrelPos.getY() + 0.5, barrelPos.getZ());

        Vector3d velocity = computeFireDirection(ownerRef, barrelPos, store);

        try {
            ProjectileModule.get().spawnProjectile(ownerRef, commandBuffer, cfg, spawnPos, velocity);
            if (nuke) {
                state.nukeAmmo--;
                state.lastNukeFireMs = now;
                ownerPlayerRef.sendMessage(Message.raw("§c[Barrel] BOOM! Nuke shell fired! (" + state.nukeAmmo + " remaining)"));
            } else {
                state.tntAmmo--;
                state.lastTntFireMs = now;
                ownerPlayerRef.sendMessage(Message.raw("§e[Barrel] TNT shell fired! (" + state.tntAmmo + " remaining)"));
            }
        } catch (Exception e) {
            ownerPlayerRef.sendMessage(Message.raw("§c[Barrel] Fire failed: " + e.getMessage()));
        }
    }

    // Fires away from the player at 30° elevation (player stands behind barrel, barrel faces away)
    private Vector3d computeFireDirection(Ref<EntityStore> ownerRef, Vector3d barrelPos, Store<EntityStore> store) {
        TransformComponent playerTransform = store.getComponent(ownerRef, TransformComponent.getComponentType());
        if (playerTransform != null) {
            Vector3d playerPos = playerTransform.getPosition();
            double dx = barrelPos.getX() - playerPos.getX();
            double dz = barrelPos.getZ() - playerPos.getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            if (horiz > 0.1) {
                double elevation = Math.tan(Math.toRadians(30));
                double len = Math.sqrt(dx * dx + dz * dz + elevation * elevation * horiz * horiz);
                return new Vector3d(dx / horiz, elevation, dz / horiz);
            }
        }
        // Fallback: fire straight up
        return new Vector3d(0, 1, 0);
    }
}
