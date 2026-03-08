# Gun Turret Plugin — Hytale Server Plugin v1.0.0

A Hytale server plugin that adds a **deployable auto-targeting gun turret**. Throw the item to deploy it anywhere — the turret scans for nearby hostile mobs and fires projectiles automatically.

---

## Quick Start

```
/turret           — get the Gun Turret item
Throw the item    — hold and release / drop from hand
                  — turret deploys and starts shooting automatically
```

---

## How to Use

### Step 1 — Get the Turret
```
/turret
```
You receive a **Gun Turret** item (Rare quality, level 40).

### Step 2 — Deploy It
With the turret item in hand, **throw it** — toss it forward or drop from hand. The turret entity spawns at the landing point and begins scanning for targets immediately.

### Step 3 — Let It Work
The turret operates fully automatically:
- Scans a **40-block radius** every 500ms
- Picks the **nearest hostile mob** as its target
- Fires a bullet projectile every **500ms** (2 shots/second)
- Deals **6 damage** per hit
- Ignores players — only targets hostile NPCs

### Multiple Turrets
You can deploy as many turrets as you want. Run `/turret` multiple times to get more items, then throw each one. All turrets operate independently.

---

## Stats

| Property | Value |
|----------|-------|
| Command | `/turret` |
| Deploy Method | Throw the item |
| Scan Range | 40 blocks |
| Fire Rate | Every 500ms (2/second) |
| Damage per Shot | 6 |
| Target Type | Hostile mobs only |
| Player Damage | None (ignores players) |

---

## What It Targets

The turret targets any hostile NPC within range, including:
- All **Goblin** variants (Scrapper, Miner, Lobber, Ogre, Duke, etc.)
- All **Skeleton** variants (Fighter, Knight, Archer, Mage, etc.)
- All **Trork** variants (Brawler, Warrior, Chieftain, etc.)
- All **Kweebec** combat variants (Razorleaf, Sproutling, etc.)
- **Zombies** (standard, Burnt, Frost, Sand, Aberrant)
- **Emberwulf**, **Spider**, **Spider_Cave**

> The turret checks if the entity is flagged as hostile. Passive animals (Deer, Cow, Rabbit, etc.) are not targeted.

---

## Pair With Asset Pack

**Both files are required** for a fully functional turret:

| File | Purpose |
|------|---------|
| `GunTurretPlugin-1.0.0.jar` | Java plugin — scanning, targeting, shooting logic |
| `GunTurret-1.0.0.zip` | Asset pack — turret model, bullet model, item definition |

The asset pack alone will deploy the turret model but **won't fire** — the vanilla `DeployableProjectileShooterComponent.spawnProjectile()` is a no-op stub in this Hytale build. The Java plugin overrides it with real projectile spawning.

---

## Installation

Copy **both** files to `UserData/Mods/`:
```
~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
├── GunTurretPlugin-1.0.0.jar
└── GunTurret-1.0.0.zip
```
Launch the world, then run `/turret`.

### Build from Source
```bash
export JAVA_HOME=/home/linuxbrew/.linuxbrew/Cellar/openjdk/25.0.2/libexec
export PATH=$JAVA_HOME/bin:$PATH
cd /home/deck/GunTurretPlugin
./gradlew shadowJar
cp build/libs/GunTurretPlugin-1.0.0.jar ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
```

---

## Technical Notes

- Requires Hytale build `>= 2026.02.19`
- The turret is a server-side entity — it persists in the world until the server restarts or you reload the world
- Bullets use the `Gun_Turret_Bullet` projectile model from the asset pack
- The scanning loop uses `World.getEntitiesInRadius()` to find targets on each tick
