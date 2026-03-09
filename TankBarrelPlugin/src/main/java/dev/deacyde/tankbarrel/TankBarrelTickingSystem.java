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
import com.hypixel.hytale.math.vector.Vector3f;
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
    private final ConcurrentHashMap<Long, TankBarrelState> barrelStates = new ConcurrentHashMap<>();

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

        // Pick the first pending action — works for single-player.
        UUID firstKey = TankBarrelPlugin.pendingActions.keys().nextElement();
        TankBarrelPlugin.PendingAction pending = TankBarrelPlugin.pendingActions.remove(firstKey);
        if (pending == null) return;

        PlayerRef ownerPlayerRef = pending.playerRef;

        // Use the PLAYER's entity ref as projectile owner (same as GunTurret uses dc.getOwner()).
        // playerRef.getReference() gives the valid player entity ref in the entity store.
        Ref<EntityStore> playerEntityRef = ownerPlayerRef.getReference();
        if (playerEntityRef == null || !playerEntityRef.isValid()) return;

        TransformComponent barrelTransform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (barrelTransform == null) return;

        // Key state by barrel position (stable across ticks, unlike Ref identity hash)
        Vector3d pos = barrelTransform.getPosition();
        long barrelKey = (long)(pos.getX() * 100) * 1000000L + (long)(pos.getY() * 100) * 1000L + (long)(pos.getZ() * 100);
        TankBarrelState state = barrelStates.computeIfAbsent(barrelKey, k -> new TankBarrelState());

        LOGGER.atInfo().log("[TankBarrel] Processing " + pending.action + " — tnt=" + state.tntAmmo + " nuke=" + state.nukeAmmo);

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
                fireShell(playerEntityRef, ownerPlayerRef, barrelTransform, state, commandBuffer, false);
                break;
            case FIRE_NUKE:
                fireShell(playerEntityRef, ownerPlayerRef, barrelTransform, state, commandBuffer, true);
                break;
            case SHOW_AMMO:
                ownerPlayerRef.sendMessage(Message.raw("§e[Barrel] Ammo: §fTNT=" + state.tntAmmo + " §cNuke=" + state.nukeAmmo));
                break;
        }
    }

    private void fireShell(Ref<EntityStore> playerEntityRef, PlayerRef ownerPlayerRef,
                           TransformComponent barrelTransform, TankBarrelState state,
                           CommandBuffer<EntityStore> commandBuffer,
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

        Vector3d barrelPos = barrelTransform.getPosition();
        Vector3d spawnPos = new Vector3d(barrelPos.getX(), barrelPos.getY() + 0.5, barrelPos.getZ());
        Vector3d velocity = computeFireDirection(ownerPlayerRef);

        try {
            ProjectileModule.get().spawnProjectile(playerEntityRef, commandBuffer, cfg, spawnPos, velocity);
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

    // Fires in the direction the player is looking using their head yaw/pitch
    private Vector3d computeFireDirection(PlayerRef playerRef) {
        Vector3f headRot = playerRef.getHeadRotation();
        if (headRot == null) {
            return new Vector3d(0, 1, 0);
        }
        // Hytale: yaw=0 faces north (-Z), yaw=90 faces east (+X)
        // Look vector: negate the Z component vs Minecraft convention
        double yawRad = Math.toRadians(headRot.getYaw());
        double pitchRad = Math.toRadians(headRot.getPitch());
        double dx = Math.sin(yawRad) * Math.cos(pitchRad);
        double dy = -Math.sin(pitchRad);
        double dz = -Math.cos(yawRad) * Math.cos(pitchRad);
        return new Vector3d(dx, dy, dz);
    }
}
