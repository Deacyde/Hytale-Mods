# Gun Turret — Damage Debugging Log

All attempts to get the turret to deal damage to NPCs. Pick up from here next session.

---

## Current State (as of 2026-03-07)

**What works:**
- `/turret` command gives the Gun Turret item ✅
- Turret deploys with correct model ✅
- Turret plays crossbow animation ✅
- Turret targets nearest NPC in range ✅
- Arrows visually leave the turret and fly ✅
- Multiple arrows fire continuously ✅

**What doesn't work:**
- Arrows do NOT deal damage to NPCs ❌
- Arrows appear to physically miss/pass through NPCs ❌

---

## Root Cause #1 — Vanilla stub (SOLVED)

`DeployableProjectileShooterComponent.spawnProjectile()` is a **no-op stub**:
```java
public void spawnProjectile(...) {
    commandBuffer.getExternalData().getWorld().execute(() -> { /* EMPTY - just returns */ });
}
```
The animation plays but nothing is ever spawned. Fixed by writing `TurretTickingSystem`
which calls `ProjectileModule.get().spawnProjectile()` directly.

---

## Attempt Log

### Attempt 1 — TurretTickingSystem with UUIDComponent in query
- **What:** Query included `UUIDComponent` alongside `DeployableComponent + TransformComponent`
- **Result:** No arrows fired at all — turret entities don't have `UUIDComponent`, so tick() never ran
- **Fix:** Removed `UUIDComponent` from query

### Attempt 2 — Wrong ComponentAccessor in getAllEntitiesInSphere
- **What:** Passed `store` to `TargetUtil.getAllEntitiesInSphere()` instead of `commandBuffer`
- **Result:** Target detection didn't work properly
- **Fix:** Pass `commandBuffer` (confirmed by vanilla `DeployableTurretConfig.tickAttackState()` source)

### Attempt 3 — Arrow spawns inside turret hitbox
- **What:** Arrow spawned at turret position `y + 1.5` with no horizontal offset
- **Result:** Arrow immediately hit the turret's own collision box, only 1 arrow ever fired, turret became disabled
- **Fix:** Moved spawn to `y + 3.0` (matches RPGCompanion's exact value)

### Attempt 4 — Arrow hits player character
- **What:** Arrow spawned 2 blocks horizontally offset toward target but still at `y + 1.5`
- **Owner:** `ownerUUID` (player UUID) + `turretRef` — 6-arg spawnProjectile
- **Result:** Arrow hit the player standing next to the turret (player body not excluded from own projectile collision when entity ref differs from owner UUID)
- **Fix:** Changed to `y + 3.0` spawn height to arc over player's head

### Attempt 5 — turretRef-only owner, no UUID
- **What:** `ProjectileModule.get().spawnProjectile(turretRef, commandBuffer, cfg, pos, vel)` — 5-arg, no UUID
- **Spawn:** `y + 3.0`, targeting correct
- **Result:** Arrows fly, target correctly, multiple fire — but ZERO damage to NPCs
- **Hypothesis:** Turret deployable entity has no combat stats; arrows do 0 damage when shooter has no stats

### Attempt 6 — ownerUUID restored (6-arg)
- **What:** `spawnProjectile(ownerUUID, turretRef, commandBuffer, cfg, pos, vel)` with `y + 3.0`
- **Result:** Arrows fly, target correctly — still no damage to NPCs
- **Hypothesis:** The 6-arg version may require the UUID and entity ref to match the same entity

### Attempt 7 — dc.getOwner() Ref, 5-arg (matching RPGCompanion pattern)
- **What:** `spawnProjectile(dc.getOwner(), commandBuffer, cfg, pos, vel)`
- **Source:** RPGCompanion uses `spawnProjectile(ref, commandBuffer, asset, pos, vel)` where ref = companion (living entity)
- **Result:** Arrows fly, target correctly — still no damage
- **Hypothesis:** Maybe NPCs spawned with `/npc spawn` are test dummies with no health/combat

### Attempt 8 — DamageSystems.executeDamage() direct damage
- **What:** Still spawn visual arrow, PLUS apply damage directly:
  ```java
  DamageSystems.executeDamage(target, commandBuffer,
      new Damage(new Damage.EntitySource(ownerRef), DamageCause.PHYSICAL, 6.0f));
  ```
- **Source:** Exact pattern from `CompanionAbilityExecutor.applyDamage()` in RPGCompanion
- **Result:** Arrows fire, still no visible damage or NPC death
- **Status:** UNKNOWN — either DamageSystems doesn't work on this NPC type, or `/npc spawn` entities are invincible

---

## New Attempts to Try (2026-03-07)

### Attempt 9 — Log everything, remove silent catch
**What to do:** Change `catch (Exception ignored)` to log the exception, and add prints at every decision point.
```java
System.out.println("[GunTurret] tick() - turret found, owner=" + ownerRef + " valid=" + (ownerRef != null && ownerRef.isValid()));
System.out.println("[GunTurret] target found: " + (target != null));
System.out.println("[GunTurret] Firing at target, applying damage " + TURRET_DAMAGE);
// change catch to:
} catch (Exception e) {
    System.err.println("[GunTurret] Exception in fire: " + e);
}
```
**Why:** We have no visibility into what's failing. The silent catch may be hiding a crash.

---

### Attempt 10 — Check EntityStatMap on target before damage
**Critical discovery from RPGCompanion's `PetCombatDamageSystem`:**
Before calling `DamageSystems.executeDamage()`, it checks:
```java
EntityStatMap statMap = store.getComponent(target, EntityStatMap.getComponentType());
// If statMap is null or has no "Health" entry → entity CANNOT take damage
int healthIndex = EntityStatType.getAssetMap().getIndex("Health");
EntityStatValue healthVal = (statMap != null && healthIndex >= 0) ? statMap.get(healthIndex) : null;
System.out.println("[GunTurret] Target health stat: " + (healthVal != null ? healthVal.get() : "NO HEALTH COMPONENT"));
```
**Imports needed:**
```java
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
```
**Why this matters:** `/npc spawn` likely spawns quest/dialogue NPCs with no health component. They have `NPCEntity` but no `EntityStatMap`. `DamageSystems.executeDamage()` fires but has nothing to reduce. The NPC is effectively invincible.

**Fix:** Add `EntityStatMap` check to `findNearestNpc()` — only target entities that have health:
```java
EntityStatMap statMap = store.getComponent(candidate, EntityStatMap.getComponentType());
if (statMap == null) continue; // skip entities with no health
```

---

### Attempt 11 — Use /mob spawn for testing instead of /npc spawn
**What:** Spawn a hostile mob using the game's mob command instead of NPC command.
**Why:** Hostile mobs (skeletons, zombies, etc.) have `EntityStatMap` with Health by definition. `/npc spawn` creates dialogue/quest NPCs. Try:
```
/mob spawn Skeleton
/mob spawn Zombie
```
The turret's `findNearestNpc()` checks for `NPCEntity` component — verify hostile mobs also have this, or broaden the filter:
```java
// Instead of requiring NPCEntity, require EntityStatMap with Health > 0
// and exclude players. This targets anything damageable that isn't the player.
```

---

### Attempt 12 — Broaden target filter to anything with health
**What:** Change `findNearestNpc()` to target any entity with `EntityStatMap` and health > 0, instead of requiring `NPCEntity`:
```java
private Ref<EntityStore> findNearestNpc(...) {
    for (Ref<EntityStore> candidate : candidates) {
        if (candidate == null || !candidate.isValid()) continue;
        if (candidate.equals(turretRef)) continue;
        if (store.getComponent(candidate, Player.getComponentType()) != null) continue; // skip players
        
        EntityStatMap statMap = store.getComponent(candidate, EntityStatMap.getComponentType());
        if (statMap == null) continue; // skip entities with no health

        int healthIndex = EntityStatType.getAssetMap().getIndex("Health");
        if (healthIndex < 0) continue;
        EntityStatValue health = statMap.get(healthIndex);
        if (health == null || health.get() <= 0) continue; // skip dead/no-health entities
        // ... distance check
    }
}
```
**Why:** This is more reliable than `NPCEntity` presence. Targets anything alive with health stats.

---

### Attempt 13 — Try MAGIC damage type
**What:** Change `DamageCause.PHYSICAL` to `DamageCause.MAGIC`
**Why:** Some entity types may be immune or unregistered for physical damage but not magic.
```java
new Damage(new Damage.EntitySource(ownerRef), DamageCause.MAGIC, TURRET_DAMAGE)
```

---

### Attempt 14 — Use massive damage amount to confirm if any damage lands
**What:** Change `TURRET_DAMAGE = 6.0f` to `TURRET_DAMAGE = 1000.0f`
**Why:** If damage IS being applied but the NPC has a huge health pool, nothing visible happens. 1000 damage would kill anything. If the NPC still doesn't die, damage is definitely not landing.

---

### Attempt 15 — Wrap DamageSystems in world.execute()
**What:** The vanilla stub wrapped everything in `world.execute()`. Maybe DamageSystems also needs main-thread execution:
```java
commandBuffer.getExternalData().getWorld().execute(() -> {
    try {
        DamageSystems.executeDamage(target, commandBuffer,
            new Damage(new Damage.EntitySource(ownerRef), DamageCause.PHYSICAL, TURRET_DAMAGE));
    } catch (Exception e) {
        System.err.println("[GunTurret] Damage exception: " + e);
    }
});
```
**Why:** ECS ticking systems may run on a worker thread. Some APIs require the main world thread.

---

### Attempt 16 — Verify dc.getOwner() is the correct method name
**From decompile:** `deployableComponent.getOwner()` is confirmed in `DeployableTurretConfig.java` line 241:
```java
return deployableComponent == null || this.canShootOwner || !ref2.equals(deployableComponent.getOwner());
```
So `getOwner()` is correct. But verify it returns the **player** Ref and not some other entity.
Log: `System.out.println("[GunTurret] owner ref: " + dc.getOwner());`

---

### Attempt 17 — Use EntityStatMap query to find targets instead of TargetUtil
**What:** Instead of `TargetUtil.getAllEntitiesInSphere()`, manually scan the chunk store for entities with `EntityStatMap` and `TransformComponent` in range.
**Why:** `TargetUtil` might filter to specific entity types. Scanning by `EntityStatMap` presence guarantees we find damageable entities.
**Risk:** More expensive per tick — only worth trying if TargetUtil is excluding valid targets.

---

### Attempt 18 — Check if DamageSystems.executeDamage needs group context
**From RPGCompanion `PetAssistOnPunchSystem`:**
```java
public SystemGroup<EntityStore> getGroup() {
    return DamageModule.get().getFilterDamageGroup();
}
```
The damage event systems register themselves in the `filterDamageGroup`. Systems in this group intercept and modify damage events. `DamageSystems.executeDamage()` called from OUTSIDE this group (e.g., from an EntityTickingSystem) may not work because no damage event listeners are registered to process it.

**Fix:** Register `TurretTickingSystem` with the damage group? Or use a different dispatch mechanism. Needs research.

---

## Priority Order for Next Session

1. **Attempt 9** (add logging) — do this FIRST, confirms what's actually running
2. **Attempt 11** (try `/mob spawn Skeleton`) — quick in-game test, no code change needed
3. **Attempt 10 + 12** (EntityStatMap filter) — likely the real fix for NPC targeting
4. **Attempt 14** (1000f damage) — quick sanity check
5. **Attempt 15** (world.execute wrapper) — if damage still doesn't land after above
6. **Attempt 18** (damage group context) — deeper investigation if all else fails


1. **Are `/npc spawn` entities actually damageable?**
   - Test: manually hit an `/npc spawn` entity with a weapon — does it die?
   - If not → use `/mob spawn <hostile_mob>` instead for testing
   - The turret targets anything with `NPCEntity` component; hostile mobs should also qualify

2. **Is `DamageSystems.executeDamage()` being called?**
   - Add a log line before the call: `System.out.println("[GunTurret] Applying damage to target");`
   - Check server console to confirm it runs

3. **Does `dc.getOwner()` return a valid player Ref?**
   - Log: `System.out.println("[GunTurret] ownerRef valid: " + (ownerRef != null && ownerRef.isValid()));`
   - If ownerRef is always null/invalid, the `return` guard skips the whole fire block

4. **Check if DamageSystems requires a different import path**
   - Verify: `com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems`
   - In RPGCompanion decompile: `/tmp/rpg_decompiled/sources/com/dwarfpet/abilities/CompanionAbilityExecutor.java` line 537

5. **Try a mob that has health in its archetype**
   - Check if the target entity has a health component:
     ```java
     // Before applying damage, log the target's archetype
     System.out.println("[GunTurret] Target archetype: " + store.getArchetype(target));
     ```

---

## Confirmed Working APIs

```java
// Spawn projectile (visual) — 5-arg, living entity as owner
ProjectileModule.get().spawnProjectile(Ref ownerRef, CommandBuffer cb, ProjectileConfig cfg, Vector3d pos, Vector3d vel);

// Apply direct damage — bypasses projectile collision
DamageSystems.executeDamage(Ref target, CommandBuffer cb,
    new Damage(new Damage.EntitySource(Ref attacker), DamageCause.PHYSICAL, float amount));

// Spatial query — use commandBuffer not store
TargetUtil.getAllEntitiesInSphere(Vector3d center, double radius, CommandBuffer cb);

// Turret ID check
dc.getConfig().getId()  // returns "Gun_Turret" for our turret

// Turret owner
dc.getOwner()       // Ref<EntityStore> — player who placed the turret
dc.getOwnerUUID()   // UUID — player's UUID
```

---

## Current TurretTickingSystem.java Logic

```
tick() called for every entity matching [DeployableComponent, TransformComponent]
  → filter to "Gun_Turret" only
  → rate-limit to FIRE_INTERVAL_MS (500ms)
  → TargetUtil.getAllEntitiesInSphere(turretPos, 40.0, commandBuffer)
  → findNearestNpc() — filters for NPCEntity, excludes Player
  → spawn visual arrow from (turretPos.y + 3.0) aimed at (targetPos.y + 1.0)
  → DamageSystems.executeDamage(target, commandBuffer, Damage(playerSource, PHYSICAL, 6.0f))
```

---

## Files

| File | Purpose |
|------|---------|
| `src/main/java/dev/deacyde/gunturret/TurretTickingSystem.java` | Core logic — targeting + shooting |
| `src/main/java/dev/deacyde/gunturret/GunTurretPlugin.java` | Plugin entry, registers system |
| `src/main/java/dev/deacyde/gunturret/GunTurretCommand.java` | `/turret` command |
| `/home/deck/GunTurretMod/` | Asset pack source (ZIP contents) |
| `HYTALE_MODDING_NOTES.md` | General Hytale modding reference |

---

## Build & Deploy

```bash
export JAVA_HOME=/home/linuxbrew/.linuxbrew/Cellar/openjdk/25.0.2/libexec
export PATH=$JAVA_HOME/bin:$PATH
cd /home/deck/GunTurretPlugin
./gradlew shadowJar
cp build/libs/GunTurretPlugin-1.0.0.jar ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/

# Repack asset ZIP (run from GunTurretMod dir):
cd /home/deck/GunTurretMod
zip -r /tmp/GunTurret-1.0.0.zip manifest.json Server/ Common/
cp /tmp/GunTurret-1.0.0.zip ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
```

## Decompiled Sources (for reference)

```
/tmp/rpg_decompiled/sources/com/dwarfpet/abilities/CompanionAbilityExecutor.java
  → fireTurretProjectile() at line 1700
  → applyDamage() at line 535

/tmp/hytale_turret/sources/com/hypixel/hytale/builtin/deployables/config/DeployableTurretConfig.java
  → tickAttackState() — confirms TargetUtil.getAllEntitiesInSphere(pos, radius, commandBuffer)
  → spawnProjectile() call at line 224 — THIS IS THE STUB, does nothing
```
