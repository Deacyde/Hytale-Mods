# Spawn Block Plugin — Hytale Server Plugin v1.0.0

A Hytale server plugin that adds a **placeable mob spawner block**. Configure the mob type, spawn rate, and cap via command, then place the block — it continuously spawns mobs nearby on a timer.

---

## Quick Start

```
/spawnblock give                 — get the Spawn Block item
/spawnblock Goblin_Scrapper      — configure to spawn goblins
Place the block in the world     — spawning starts immediately
```

---

## Commands

### Give Yourself the Block
```
/spawnblock give
```

### Configure the Next Block Placed
```
/spawnblock <mobType> [rateSeconds] [maxMobs] [radius]
```

**Parameters:**

| Parameter | Default | Min | Max | Description |
|-----------|---------|-----|-----|-------------|
| `mobType` | `Goblin_Scrapper` | — | — | NPC role ID (see list below) |
| `rateSeconds` | `30` | 1 | 300 | Spawn interval in game seconds |
| `maxMobs` | `5` | 1 | 50 | Max mobs alive within radius before pausing spawns |
| `radius` | `10` | 1 | 100 | Block radius to count existing mobs |

### Examples
```
/spawnblock Goblin_Scrapper                   # default settings
/spawnblock Goblin_Scrapper 30 5 10           # every 30s, max 5, within 10 blocks
/spawnblock Trork_Warrior 15 8 20             # Trork every 15s, max 8, 20 block radius
/spawnblock Emberwulf 60 3 15                 # Emberwulf every 60s, max 3
/spawnblock Skeleton_Knight 10 10 25          # skeleton knights, fast rate, high cap
/spawnblock Zombie 20 5 15                    # standard zombies
/spawnblock Goblin_Duke 120 1 30              # boss goblin every 2 minutes, max 1
```

---

## How to Use

1. **Configure** the mob type and settings with `/spawnblock <mobType> [options]`
2. **Get the block** with `/spawnblock give` (or give another, each stores its own settings)
3. **Place the block** anywhere — it starts spawning immediately on the timer
4. **Break the block** to stop spawning

> **Important:** The config from `/spawnblock` applies to the **next block you place**. Configure first, then place. Multiple blocks can be active simultaneously with different configs.

---

## Mob ID Reference

Use these IDs exactly as shown (case-sensitive) with `/spawnblock <mobId>`.

### Goblins (Zone 1 enemies)
| Mob ID | Description |
|--------|-------------|
| `Goblin_Scrapper` | Basic melee goblin |
| `Goblin_Miner` | Goblin with pickaxe |
| `Goblin_Lobber` | Ranged goblin (throws things) |
| `Goblin_Ogre` | Large tank goblin |
| `Goblin_Scavenger` | Sneaky scavenger goblin |
| `Goblin_Scavenger_Sword` | Sword-wielding scavenger |
| `Goblin_Scavenger_Battleaxe` | Axe-wielding scavenger |
| `Goblin_Thief` | Fast goblin thief |
| `Goblin_Hermit` | Reclusive hermit goblin |
| `Goblin_Duke` | Boss goblin (multiple phases) |

### Skeletons (Zone 2 enemies)
| Mob ID | Description |
|--------|-------------|
| `Skeleton` | Basic skeleton |
| `Skeleton_Fighter` | Sword-wielding skeleton |
| `Skeleton_Knight` | Armored skeleton knight |
| `Skeleton_Archer` | Skeleton archer |
| `Skeleton_Mage` | Magic-casting skeleton |
| `Skeleton_Scout` | Fast scouting skeleton |
| `Skeleton_Ranger` | Skeleton ranger |
| `Skeleton_Archmage` | Powerful skeleton mage |
| `Skeleton_Frost_Knight` | Ice skeleton knight |
| `Skeleton_Frost_Archer` | Ice skeleton archer |
| `Skeleton_Frost_Mage` | Ice skeleton mage |
| `Skeleton_Burnt_Knight` | Fire skeleton knight |
| `Skeleton_Burnt_Archer` | Fire skeleton archer |
| `Skeleton_Sand_Guard` | Desert skeleton guard |
| `Skeleton_Sand_Archer` | Desert skeleton archer |
| `Skeleton_Pirate_Captain` | Pirate skeleton captain |
| `Skeleton_Pirate_Gunner` | Pirate skeleton gunner |

### Trorks (Zone 3 enemies)
| Mob ID | Description |
|--------|-------------|
| `Trork_Brawler` | Basic melee trork |
| `Trork_Warrior` | Armed trork warrior |
| `Trork_Guard` | Defensive trork guard |
| `Trork_Hunter` | Trork hunter |
| `Trork_Mauler` | Heavy trork fighter |
| `Trork_Sentry` | Trork sentry |
| `Trork_Shaman` | Trork spellcaster |
| `Trork_Chieftain` | Trork boss/leader |
| `Trork_Doctor_Witch` | Trork witch doctor |

### Kweebec (Zone 4 enemies)
| Mob ID | Description |
|--------|-------------|
| `Kweebec_Razorleaf` | Combat kweebec |
| `Kweebec_Sproutling` | Small kweebec |
| `Kweebec_Sapling` | Kweebec sapling |
| `Kweebec_Elder` | Kweebec elder |

### Zombies
| Mob ID | Description |
|--------|-------------|
| `Zombie` | Standard zombie |
| `Zombie_Burnt` | Fire zombie |
| `Zombie_Frost` | Ice zombie |
| `Zombie_Sand` | Desert zombie |
| `Zombie_Aberrant` | Mutated zombie |
| `Zombie_Aberrant_Big` | Large mutated zombie |

### Wildlife — Passive
| Mob ID | Description |
|--------|-------------|
| `Antelope` | Antelope |
| `Bison` | Bison |
| `Deer_Stag` | Male deer |
| `Deer_Doe` | Female deer |
| `Bear_Grizzly` | Grizzly bear |
| `Bear_Polar` | Polar bear |
| `Wolf_Black` | Black wolf |
| `Wolf_White` | White wolf |
| `Horse` | Horse |
| `Pig` | Pig |
| `Cow` | Cow |
| `Chicken` | Chicken |
| `Rabbit` | Rabbit |
| `Fox` | Fox |
| `Spider` | Spider |
| `Spider_Cave` | Cave spider |
| `Emberwulf` | Fire wolf creature |

---

## Timing Note

Hytale uses **accelerated game time** — `rateSeconds` is measured in **game seconds**, not real seconds. The world clock runs faster than real time.

| Rate Setting | Approx. Real-World Interval |
|-------------|----------------------------|
| 10 game seconds | ~3 real seconds |
| 30 game seconds | ~8 real seconds |
| 60 game seconds | ~16 real seconds |
| 120 game seconds | ~32 real seconds |

> For most uses, 15–30 game seconds gives a comfortable spawn rate. Use `maxMobs` to prevent overcrowding.

---

## Installation

1. Copy `SpawnBlockPlugin-1.0.0.jar` to:
   ```
   ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
   ```
2. Launch the world
3. Run `/spawnblock give` to get a block

### Build from Source
```bash
export JAVA_HOME=/home/linuxbrew/.linuxbrew/Cellar/openjdk/25.0.2/libexec
export PATH=$JAVA_HOME/bin:$PATH
cd /home/deck/SpawnBlockPlugin
./gradlew shadowJar
cp build/libs/SpawnBlockPlugin-1.0.0.jar ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
```

---

## Technical Notes

- Requires Hytale build `>= 2026.02.19`
- Mob IDs are NPC role filenames from `Assets.zip/Server/NPC/Roles/` (without `.json`)
- Each placed block stores its own mob type, rate, cap, and radius independently
- Breaking the block stops its spawn loop
- The spawn tick runs on the Hytale server game loop (accelerated time)
