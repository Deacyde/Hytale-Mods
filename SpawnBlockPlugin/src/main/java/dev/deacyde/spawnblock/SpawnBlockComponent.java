package dev.deacyde.spawnblock;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class SpawnBlockComponent implements Component<ChunkStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<SpawnBlockComponent> CODEC = BuilderCodec
        .builder(SpawnBlockComponent.class, SpawnBlockComponent::new)
        .append(new KeyedCodec<>("MobType", Codec.STRING, true),
                SpawnBlockComponent::setMobType, SpawnBlockComponent::getMobType).add()
        .append(new KeyedCodec<>("SpawnRate", Codec.INTEGER, true),
                SpawnBlockComponent::setSpawnRate, SpawnBlockComponent::getSpawnRate).add()
        .append(new KeyedCodec<>("MaxMobs", Codec.INTEGER, true),
                SpawnBlockComponent::setMaxMobs, SpawnBlockComponent::getMaxMobs).add()
        .append(new KeyedCodec<>("SpawnRadius", Codec.INTEGER, true),
                SpawnBlockComponent::setSpawnRadius, SpawnBlockComponent::getSpawnRadius).add()
        .build();

    private String mobType = "Goblin_Scrapper";
    private int spawnRate = 30;  // game seconds between spawns (Landmark uses 30)
    private int maxMobs = 5;
    private int spawnRadius = 10;

    public SpawnBlockComponent() {}

    public void onTick(int x, int y, int z, World world) {
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            LOGGER.atWarning().log("[SpawnBlock] NPCPlugin not available");
            return;
        }

        world.execute(() -> {
            try {
                var entityStore = world.getEntityStore().getStore();
                Vector3d blockPos = new Vector3d(x, y, z);

                // Count existing NPCs within spawnRadius — enforce mob cap
                List<Ref<EntityStore>> nearby = TargetUtil.getAllEntitiesInSphere(
                    blockPos, spawnRadius, entityStore);
                long npcCount = nearby.stream()
                    .filter(r -> r != null && r.isValid())
                    .filter(r -> entityStore.getComponent(r, NPCEntity.getComponentType()) != null)
                    .filter(r -> entityStore.getComponent(r, PlayerRef.getComponentType()) == null)
                    .count();

                if (npcCount >= maxMobs) {
                    LOGGER.atInfo().log("[SpawnBlock] Cap reached (" + npcCount + "/" + maxMobs + ") at " + x + "," + y + "," + z);
                    return;
                }

                double ox = (Math.random() * 2 - 1) * spawnRadius;
                double oz = (Math.random() * 2 - 1) * spawnRadius;
                Vector3d spawnPos = new Vector3d(x + ox, y, z + oz);
                var result = npcPlugin.spawnNPC(entityStore, mobType, null, spawnPos, new Vector3f(0f, 0f, 0f));
                if (result != null && result.first() != null && result.first().isValid()) {
                    LOGGER.atInfo().log("[SpawnBlock] Spawned " + mobType + " (" + (npcCount + 1) + "/" + maxMobs + ") at " + x + "," + y + "," + z);
                } else {
                    LOGGER.atWarning().log("[SpawnBlock] spawnNPC returned null/invalid for role: " + mobType);
                }
            } catch (Exception e) {
                LOGGER.atSevere().log("[SpawnBlock] Error during mob spawn: " + e.getMessage());
            }
        });
    }

    public Instant getNextScheduledTick(WorldTimeResource timeResource) {
        return timeResource.getGameTime().plus(spawnRate, ChronoUnit.SECONDS);
    }

    @Override
    public Component<ChunkStore> clone() {
        SpawnBlockComponent c = new SpawnBlockComponent();
        c.mobType = this.mobType;
        c.spawnRate = this.spawnRate;
        c.maxMobs = this.maxMobs;
        c.spawnRadius = this.spawnRadius;
        return c;
    }

    public String getMobType() { return mobType; }
    public void setMobType(String mobType) { this.mobType = mobType; }

    public int getSpawnRate() { return spawnRate; }
    public void setSpawnRate(int spawnRate) { this.spawnRate = Math.max(1, spawnRate); }

    public int getMaxMobs() { return maxMobs; }
    public void setMaxMobs(int maxMobs) { this.maxMobs = Math.max(1, maxMobs); }

    public int getSpawnRadius() { return spawnRadius; }
    public void setSpawnRadius(int spawnRadius) { this.spawnRadius = Math.max(1, spawnRadius); }
}
