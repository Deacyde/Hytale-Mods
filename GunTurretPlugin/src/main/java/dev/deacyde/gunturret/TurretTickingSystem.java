package dev.deacyde.gunturret;

import com.hypixel.hytale.builtin.deployables.component.DeployableComponent;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ticking system that fires real projectiles from deployed Gun Turrets.
 *
 * The vanilla DeployableProjectileShooterComponent.spawnProjectile() is a no-op stub
 * in this game version — turret animation plays but no bullet spawns. This system
 * bypasses it and calls ProjectileModule.get().spawnProjectile() directly.
 */
public class TurretTickingSystem extends EntityTickingSystem<EntityStore> {

    private static final String TURRET_ID = "Gun_Turret";
    private static final double TURRET_RANGE = 40.0;
    private static final long FIRE_INTERVAL_MS = 500L;
    private static final float TURRET_DAMAGE = 6.0f; // arrow crossbow base damage

    private static final String[] PROJECTILE_CONFIG_IDS = {
        "Projectile_Config_Arrow_Crossbow",
        "Projectile_Config_Arrow_Base"
    };

    // Track last fire time per turret entity ref (keyed by identity hash)
    private final ConcurrentHashMap<Integer, Long> lastFireTimes = new ConcurrentHashMap<>();

    // Minimal query — UUIDComponent intentionally excluded because turret entities
    // may not have it (vanilla code null-checks it). Including it would prevent
    // our tick() from ever being called for deployed turrets.
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
        if (!TURRET_ID.equals(dc.getConfig().getId())) return;

        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) return;

        // Rate-limit shooting
        Ref<EntityStore> turretRef = chunk.getReferenceTo(index);
        int refId = System.identityHashCode(turretRef);
        long now = System.currentTimeMillis();
        if (now - lastFireTimes.getOrDefault(refId, 0L) < FIRE_INTERVAL_MS) return;

        Vector3d turretPos = transform.getPosition();

        // Find nearest NPC in range.
        // Pass commandBuffer (not store) — vanilla turret does the same for spatial queries.
        List<Ref<EntityStore>> nearby = TargetUtil.getAllEntitiesInSphere(turretPos, TURRET_RANGE, commandBuffer);
        Ref<EntityStore> target = findNearestNpc(nearby, turretRef, turretPos, store);
        if (target == null) return;

        ProjectileConfig cfg = resolveProjectileConfig();
        if (cfg == null) return;

        TransformComponent targetTransform = store.getComponent(target, TransformComponent.getComponentType());
        if (targetTransform == null) return;

        Vector3d targetPos = targetTransform.getPosition();

        // Spawn 3 blocks above the turret — high enough to arc over the player's head
        // and any nearby terrain. The player standing next to their own turret was being
        // hit when spawning lower, since the player is typically between turret and NPC.
        Vector3d spawnPos = new Vector3d(turretPos.getX(), turretPos.getY() + 3.0, turretPos.getZ());

        // Aim at NPC chest height
        // Use the player entity Ref (dc.getOwner()) as the projectile owner — exactly how
        // RPGCompanion's turret works. The turret deployable entity has zero combat stats,
        // so arrows spawned from it deal 0 damage. The player entity has proper stats.
        // Use the 5-arg spawnProjectile(Ref, commandBuffer, config, pos, vel) — no UUID needed.
        // Add slight spread to x/z like RPGCompanion does.
        Ref<EntityStore> ownerRef = dc.getOwner();
        if (ownerRef == null || !ownerRef.isValid()) return;

        double spreadX = (Math.random() - 0.5) * 0.3;
        double spreadZ = (Math.random() - 0.5) * 0.3;
        double dx = (targetPos.getX() + spreadX) - spawnPos.getX();
        double dy = (targetPos.getY() + 1.0) - spawnPos.getY();
        double dz = (targetPos.getZ() + spreadZ) - spawnPos.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.1) return;

        Vector3d velocity = new Vector3d(dx / dist, dy / dist, dz / dist);

        try {
            // Spawn visual arrow — may or may not physically collide with the target
            ProjectileModule.get().spawnProjectile(ownerRef, commandBuffer, cfg, spawnPos, velocity);

            // Apply damage directly, attributed to the player (ownerRef).
            // This bypasses projectile hit-detection which is unreliable for deployable turrets.
            // Same pattern used by RPGCompanion: DamageSystems.executeDamage(target, cb, Damage)
            DamageSystems.executeDamage(target, commandBuffer,
                new Damage(new Damage.EntitySource(ownerRef), DamageCause.PHYSICAL, TURRET_DAMAGE));

            lastFireTimes.put(refId, now);
        } catch (Exception ignored) {
        }
    }

    private Ref<EntityStore> findNearestNpc(
            List<Ref<EntityStore>> candidates,
            Ref<EntityStore> turretRef,
            Vector3d turretPos,
            Store<EntityStore> store) {

        Ref<EntityStore> nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Ref<EntityStore> candidate : candidates) {
            if (candidate == null || !candidate.isValid()) continue;
            if (candidate.equals(turretRef)) continue;
            // Must be an NPC; skip players and non-NPC entities
            if (store.getComponent(candidate, NPCEntity.getComponentType()) == null) continue;
            if (store.getComponent(candidate, Player.getComponentType()) != null) continue;

            TransformComponent tt = store.getComponent(candidate, TransformComponent.getComponentType());
            if (tt == null) continue;

            double d = turretPos.distanceTo(tt.getPosition());
            if (d < minDist) {
                minDist = d;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private ProjectileConfig resolveProjectileConfig() {
        for (String id : PROJECTILE_CONFIG_IDS) {
            ProjectileConfig cfg = ProjectileConfig.getAssetMap().getAsset(id);
            if (cfg != null) return cfg;
        }
        return null;
    }
}
