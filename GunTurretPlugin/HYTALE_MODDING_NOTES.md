# Hytale Modding Notes — Deacyde
> Reference for future mods. Written 2026.

---

## Java Plugin vs JSON Mod

| Approach | Pros | Cons |
|---|---|---|
| JSON Mod (.zip) | Simple, no compilation | Limited logic, no events, no commands |
| Java Plugin (.jar) | Full API access, commands, events | Needs JDK 25, Gradle build |

**Use Java plugins** for anything involving commands, custom AI/logic, event listeners, or asset registration.  
**Use JSON mods** for simple reskins, stat tweaks, new items/blocks with existing interactions.

---

## Java Plugin Quick Setup

Template: https://github.com/realBritakee/hytale-template-plugin

```
JAVA_HOME=/home/linuxbrew/.linuxbrew/Cellar/openjdk/25.0.2/libexec
export PATH=$JAVA_HOME/bin:$PATH
cd /home/deck/YourPlugin
./gradlew shadowJar
# Output: build/libs/YourPlugin-X.Y.Z.jar
cp build/libs/YourPlugin-X.Y.Z.jar ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
```

**gradle.properties** critical settings:
- `hytale_build=2026.02.19-1a311a592`  ← must match installed version (from HytaleServer.jar manifest)
- `java_version=25`
- `includes_pack=true`  ← lets you bundle JSON assets inside the jar
- Do NOT include `ServerVersion` in manifest.json — causes SEVERE log spam

**manifest.json** (no ServerVersion field!):
```json
{
  "Group": "Deacyde",
  "Name": "YourMod",
  "Version": "1.0.0",
  "Main": "dev.deacyde.yourmod.YourPlugin",
  "IncludesAssetPack": true
}
```

---

## Plugin Entry Point

```java
public class YourPlugin extends JavaPlugin {
    public YourPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new YourCommand());
        // this.getEventRegistry().registerListener(...)
    }
}
```

---

## Writing Commands

### Simple command (sync, no player needed)
```java
public class YourCommand extends CommandBase {
    public YourCommand() {
        super("cmd", "Description");
        this.setPermissionGroup(GameMode.Adventure); // everyone can use
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw("Hello!"));
    }
}
```

### Player command (needs Store + Ref + World)
```java
public class YourCommand extends AbstractPlayerCommand {
    public YourCommand() {
        super("cmd", "Description");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store,
                           Ref<EntityStore> playerRef, PlayerRef player, World world) {
        // Store<EntityStore> implements ComponentAccessor<EntityStore>
        // playerRef = the player entity ref
    }
}
```

### Give item to player
```java
ItemStack item = new ItemStack("Item_Id_Here", 1);
ItemUtils.throwItem(playerRef, item, 0.0f, store);   // drops at player's feet
```

---

## Asset Pack (JSON inside the jar)

Place files under `src/main/resources/` following the same path as a normal mod:
```
src/main/resources/
  manifest.json
  Server/
    Item/Items/Weapon/.../*.json
    ProjectileConfigs/.../*.json
    Models/.../*.json
```
These are loaded automatically when `IncludesAssetPack: true` in manifest.

---

## Deployable Turret — Correct JSON Format

The entire turret config is **inline** in the `SpawnDeployableAtHitLocation` interaction.
**`Type: "Turret"` is required** — `Type: "Aoe"` deploys a damage circle, NOT a turret.

```json
{
  "Type": "SpawnDeployableAtHitLocation",
  "Config": {
    "Type": "Turret",
    "Id": "YourTurretId",
    "Model": "Crossbow_Turret",
    "HitboxCollisionConfig": "HardCollision",
    "LiveDuration": 30,
    "DetectionRadius": 35,
    "TrackableRadius": 40,
    "RotationSpeed": 20,
    "ShotInterval": 0.08,
    "BurstCount": 10,
    "BurstCooldown": 1.5,
    "ProjectileDamage": 25,
    "CanShootOwner": false,
    "RespectTeams": true,
    "DoLineOfSightTest": false,
    "Stats": { "Health": { "Initial": 80, "Max": 80 } },
    "TargetOffset": { "X": 0, "Y": 1, "Z": 0 },
    "ProjectileSpawnOffsets": {
      "UP":    { "X": 0, "Y": 0.5, "Z": 0 },
      "DOWN":  { "X": 0, "Y": -0.5, "Z": 0 },
      "NORTH": { "X": 0, "Y": 0, "Z": 0.5 },
      "SOUTH": { "X": 0, "Y": 0, "Z": -0.5 },
      "EAST":  { "X": -0.5, "Y": 0, "Z": 0 },
      "WEST":  { "X": 0.5, "Y": 0, "Z": 0 }
    },
    "DeploySoundEventId": "SFX_Deployable_Totem_Heal_Spawn",
    "ProjectileHitLocalSoundEventId": "ARROW_FULLCHARGE_HIT",
    "ProjectileHitWorldSoundEventId": "ARROW_HIT_3P",
    "ProjectileConfig": {
      "Model": "Arrow_Crude",
      "Physics": {
        "Type": "Standard",
        "Gravity": 0.1,
        "TerminalVelocityAir": 100,
        "TerminalVelocityWater": 15,
        "RotationMode": "VelocityDamped",
        "Bounciness": 0.0,
        "SticksVertically": true
      },
      "LaunchForce": 50,
      "SpawnRotationOffset": { "Pitch": 0, "Yaw": 0, "Roll": 0 },
      "Interactions": {
        "ProjectileMiss": {
          "Interactions": [{ "Type": "Simple", "RunTime": 0 }]
        }
      }
    }
  }
}
```

**Turret stat fields** (from `DeployableTurretConfig`):
- `TrackableRadius` — max range to keep tracking a locked target
- `DetectionRadius` — range to initially detect/acquire targets
- `RotationSpeed` — degrees/sec to rotate toward target
- `ShotInterval` — seconds between shots in a burst
- `BurstCount` — shots fired per burst
- `BurstCooldown` — seconds between bursts
- `DeployDelay` — seconds after placement before turret activates
- `ProjectileDamage` — damage per bullet
- `RespectTeams: true` — only shoots hostile mobs, not players/allies
- `CanShootOwner: false` — won't shoot the player who placed it
- `DoLineOfSightTest` — raycast before shooting (more realistic but slower)
- `PreferOwnerTarget` — prioritize whatever the owner is targeting

---

## Block Interaction System (JSON)

### Continuous trigger (fire while player stands inside)
```json
"Material": "Empty",
"Collision": { ... }          ← NOT "CollisionEnter"
```

### One-shot trigger on collision (Proximity Mine style)
```json
"Material": "Solid",
"CollisionEnter": { ... }
```

### RootInteraction format (correct)
```json
{ "Cooldown": { "Id": "UniqueId", "Cooldown": 0.5 }, "Interactions": ["EffectName"] }
```
**Not** `"Serial": [...]` — that causes NullPointerException crash.

### Interaction Effect format
```json
{ "Type": "Serial", "Interactions": [{ "Type": "Explode", "Parent": "Explode_Generic", "Config": {...} }] }
```

### Speed/slow block (walking through)
```json
"Material": "Empty",
"Collision": {
  "Interactions": [{ "Type": "MovementSettings", "HorizontalSpeedMultiplier": 0.4 }]
}
```

### ApplyEffect with particles
```json
{
  "Type": "ApplyEffect",
  "EffectId": { "Id": "poison", "Duration": 3.0, "Strength": 1 },
  "Effects": {
    "Particles": [{ "SystemId": "Impact_Poison", "TargetEntityPart": "Entity" }]
  }
}
```

**Confirmed valid particle SystemIds**: `Explosion_Big`, `Explosion_Medium`, `Impact_Poison`

---

## ⚠️ Critical: DeployableProjectileShooterComponent is a STUB

Discovered 2026-03-07 by decompiling `HytaleServer.jar`.

```java
// This is a NO-OP in the current game version — the lambda body is empty!
public void spawnProjectile(Ref ref, CommandBuffer cmd, ProjectileConfig cfg,
                            UUID owner, Vector3d pos, Vector3d vel) {
    commandBuffer.getExternalData().getWorld().execute(() -> { /* EMPTY */ });
}
```

The turret ANIMATION (crossbow pull/release) still plays because `DeployableTurretConfig.tickAttackState()`
calls `playAnimation()` separately. But no projectile entity is ever spawned.

**The fix: use `ProjectileModule.get().spawnProjectile()` directly from a Java ticking system.**
This is how RPGCompanion's DwarfPet turret works.

---

## Turret Java Shooting System

The only working approach for real turret bullets in this game version:

### 1. Register a ticking system in `setup()`
```java
getEntityStoreRegistry().registerSystem(new TurretTickingSystem());
```

### 2. TurretTickingSystem pattern
```java
public class TurretTickingSystem extends EntityTickingSystem<EntityStore> {

    private static final Query<EntityStore> QUERY = Archetype.of(new ComponentType[]{
        DeployableComponent.getComponentType(),
        TransformComponent.getComponentType(),
        UUIDComponent.getComponentType()
    });

    @Override public Query<EntityStore> getQuery() { return QUERY; }

    @Override
    public void tick(float dt, int i, ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
        DeployableComponent dc = chunk.getComponent(i, DeployableComponent.getComponentType());
        if (dc == null || !"Gun_Turret".equals(dc.getConfig().getId())) return;

        // ... find nearest NPC via TargetUtil.getAllEntitiesInSphere(pos, range, store)
        // ... calculate normalized direction vector to target

        ProjectileConfig cfg = ProjectileConfig.getAssetMap().getAsset("Projectile_Config_Arrow_Crossbow");
        ProjectileModule.get().spawnProjectile(ownerUUID, turretRef, cmd, cfg, spawnPos, velocity);
    }
}
```

### 3. Key APIs for turret shooting
| API | Notes |
|---|---|
| `ProjectileModule.get().spawnProjectile(UUID, Ref, CommandBuffer, ProjectileConfig, Vector3d pos, Vector3d vel)` | Real projectile spawn — has actual implementation |
| `ProjectileConfig.getAssetMap().getAsset("Projectile_Config_Arrow_Crossbow")` | Load any registered projectile config by string ID |
| `TargetUtil.getAllEntitiesInSphere(Vector3d, double, ComponentAccessor)` | Find all entities within radius — returns `List<Ref<EntityStore>>` |
| `DeployableConfig.getId()` | Returns the `"Id"` field from your JSON config |
| `store.getArchetype(ref).contains(NPCEntity.getComponentType())` | Check if entity is an NPC |
| `store.getArchetype(ref).contains(Player.getComponentType())` | Check if entity is a player (to exclude) |
| `getEntityStoreRegistry().registerSystem(system)` | Register a ticking system from `setup()` |

### 4. Velocity parameter
Pass a **unit vector (normalized direction)** — `LaunchForce` in the config scales it automatically.
```java
double dx = targetX - spawnX,  dy = targetY + 1.0 - spawnY,  dz = targetZ - spawnZ;
double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
Vector3d velocity = new Vector3d(dx/dist, dy/dist, dz/dist);  // unit vector
```

---

## Key API Classes (HytaleServer.jar)

| Class | Purpose |
|---|---|
| `com.hypixel.hytale.server.core.plugin.JavaPlugin` | Plugin base class |
| `com.hypixel.hytale.server.core.plugin.JavaPluginInit` | Passed to constructor |
| `com.hypixel.hytale.server.core.command.system.basecommands.CommandBase` | Simple command |
| `com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand` | Player command |
| `com.hypixel.hytale.server.core.entity.ItemUtils` | `throwItem()`, `dropItem()` |
| `com.hypixel.hytale.server.core.inventory.ItemStack` | `new ItemStack(id, count)` |
| `com.hypixel.hytale.builtin.deployables.config.DeployableTurretConfig` | Turret logic/tick |
| `com.hypixel.hytale.builtin.deployables.DeployablesUtils` | `spawnDeployable()` |
| `com.hypixel.hytale.server.core.modules.projectile.ProjectileModule` | `get().spawnProjectile()` — real projectile spawning |
| `com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig` | Projectile asset — load via `getAssetMap().getAsset(id)` |
| `com.hypixel.hytale.component.system.tick.EntityTickingSystem` | Base for custom per-entity ticking logic |
| `com.hypixel.hytale.component.Archetype` | Build entity queries: `Archetype.of(ComponentType[])` |
| `com.hypixel.hytale.server.core.util.TargetUtil` | Spatial queries — `getAllEntitiesInSphere()` |
| `com.hypixel.hytale.component.Store` | Implements `ComponentAccessor`, used in player commands |
| `com.hypixel.hytale.server.core.Message` | `Message.raw("text")`, `Message.translation("key")` |

---

## Modding Contest Info

**"New Worlds" Contest** — $100K prize pool, 65 winners
- 3 categories: WorldGen V2, NPCs, Experiences
- 1st: $10K | 2nd: $7.5K | 3rd: $2.5K | 4th–10th: $1K each per category
- Java is allowed for advanced logic
- Top performers may be recruited by Hypixel Studios

---

## File Locations

| Path | Purpose |
|---|---|
| `~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/` | Install mods/plugins here |
| `~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/install/release/package/game/latest/Server/HytaleServer.jar` | Server JAR (compile target) |
| `~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/install/release/package/game/latest/Assets.zip` | Vanilla assets (reference JSON) |
| `/home/deck/GunTurretPlugin/` | Java plugin source |
| `/home/deck/GunTurretMod/` | Old JSON-only turret mod (superseded) |
| `/home/deck/ExtraBlocksMod/` | Extra Blocks mod (Gas Block, Speed Block, Proxy Mine) |
