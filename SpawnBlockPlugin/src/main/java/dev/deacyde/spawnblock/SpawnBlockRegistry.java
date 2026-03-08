package dev.deacyde.spawnblock;

/** Holds the most-recently configured pending spawn settings set via /spawnblock. */
public class SpawnBlockRegistry {

    private String pendingMobType;
    private int pendingRate;
    private int pendingMax;
    private int pendingRadius;
    private boolean hasPending = false;

    public void storePending(String mobType, int rateSecs, int maxMobs, int radius) {
        this.pendingMobType = mobType;
        this.pendingRate = rateSecs;
        this.pendingMax = maxMobs;
        this.pendingRadius = radius;
        this.hasPending = true;
    }

    public boolean hasPending() { return hasPending; }

    public void applyPending(SpawnBlockComponent comp) {
        if (!hasPending) return;
        comp.setMobType(pendingMobType);
        comp.setSpawnRate(pendingRate);
        comp.setMaxMobs(pendingMax);
        comp.setSpawnRadius(pendingRadius);
        hasPending = false;
    }
}

