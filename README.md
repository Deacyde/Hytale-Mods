# Deacyde's Mod Collection

Personal collection of game mods — split between **Hytale** (custom server plugins + asset packs) and **Minecraft Forge** mods.

---

## Hytale Mods

Hytale mods require build `>= 2026.02.19`. Copy all files to:
```
~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
```

### Server Plugins (Java `.jar`)

| Plugin | Files Needed | Command | Description |
|--------|-------------|---------|-------------|
| [GunTurretPlugin](GunTurretPlugin/README.md) | `GunTurretPlugin-1.0.0.jar` + `GunTurret-1.0.0.zip` | `/turret` | Deploy auto-targeting turrets that shoot nearby mobs |
| [SpawnBlockPlugin](SpawnBlockPlugin/README.md) | `SpawnBlockPlugin-1.0.0.jar` | `/spawnblock` | Placeable block that continuously spawns configured mobs |
| [WorldEditWandPlugin](WorldEditWandPlugin/README.md) | `WorldEditWandPlugin-1.0.0.jar` | `/we` | WorldEdit-style wand for fill, copy, paste, undo |
| [TankBarrelPlugin](TankBarrelPlugin/README.md) | `TankBarrelPlugin-1.0.0.jar` | `/barrel` | Deployable tank barrel — fires TNT and Nuke shells in any direction |

### Asset Packs (`.zip` only — no plugin needed)

| Mod | File | Description |
|-----|------|-------------|
| [ExtraBlocksMod](ExtraBlocksMod/README.md) | `ExtraBlocks-1.0.0.zip` | Adds Glowstone, Gas Block, Proximity Mine, Magnet Block |
| [TNTBlockMod](TNTBlockMod/README.md) | `TNTBlock-1.0.1.zip` | Throwable TNT Block + Nuke Block — throw to explode |
| [GunTurretMod](GunTurretMod/README.md) | `GunTurret-1.0.0.zip` | Asset pack for gun turret (required alongside GunTurretPlugin) |
| [NukeMod](NukeMod/README.md) | *(see TNTBlockMod)* | Original nuke-only version — superseded by TNTBlockMod |

---

## Hytale Quick Command Reference

```
/turret
  → Get a Gun Turret item, then throw it to deploy

/spawnblock give
  → Get a Spawn Block item
/spawnblock <mobId> [rateSeconds] [maxMobs] [radius]
  → Configure next placed block (configure BEFORE placing)
  → Example: /spawnblock Goblin_Scrapper 30 5 10
  → Example: /spawnblock Skeleton_Knight 15 8 20
  → Example: /spawnblock Trork_Warrior 60 3 15

/we wand               → Get WorldEdit Wand item
/we pos1               → Set pos1 at feet (or left-click with wand)
/we pos2               → Set pos2 at feet (or right-click with wand)
/we set <blockId>      → Fill selection with block
/we walls <blockId>    → Fill only the 4 side walls
/we clear              → Delete all blocks in selection (fill with air)
/we copy               → Copy selection to clipboard
/we paste              → Paste clipboard at current position
/we undo               → Undo last fill/paste
/we size               → Show selection dimensions and block count

/barrel                → Get Tank Barrel (auto-faces your direction)
/barrel give [n|e|s|w] → Get barrel facing a specific direction
/barrel load tnt       → Load 5 TNT shells into nearest barrel
/barrel load nuke      → Load 5 Nuke shells into nearest barrel
/barrel fire tnt       → Fire TNT shell (3s fuse, radius 5 explosion)
/barrel fire nuke      → Fire Nuke shell (4s fuse, MASSIVE explosion)
/barrel ammo           → Check ammo count
```

---

## Common Block IDs (for `/we set`)

```
Rock_Stone          Rock_Marble         Rock_Sandstone      Rock_Basalt
Rock_Slate          Rock_Quartzite      Rock_Bedrock        Rock_Chalk
Soil_Dirt           Soil_Grass          Soil_Gravel         Soil_Pebbles
Wood_Oak_Trunk      Wood_Birch_Trunk    Wood_Fir_Trunk      Wood_Redwood_Trunk
Wood_Crystal_Trunk  Wood_Bamboo_Trunk   Wood_Petrified_Trunk
Ore_Iron_Stone      Ore_Gold_Stone      Ore_Cobalt_Stone    Ore_Mithril_Stone
Glowstone           Gas_Block
```

## Common Mob IDs (for `/spawnblock`)

```
Goblin_Scrapper     Goblin_Ogre         Goblin_Duke
Skeleton_Knight     Skeleton_Archer     Skeleton_Frost_Knight
Trork_Warrior       Trork_Chieftain     Trork_Shaman
Kweebec_Razorleaf   Zombie              Zombie_Burnt
Bear_Grizzly        Wolf_Black          Emberwulf           Spider
```

---

## Hytale Build & Deploy

```bash
export JAVA_HOME=/home/linuxbrew/.linuxbrew/Cellar/openjdk/25.0.2/libexec
export PATH=$JAVA_HOME/bin:$PATH

# Build a plugin
cd /home/deck/<PluginFolder>
./gradlew shadowJar -q
cp build/libs/*.jar ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/

# Watch logs (replace world name as needed)
tail -f ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Saves/"New World 4"/logs/latest.log
```

---

## Minecraft Forge Mods

Minecraft 1.21.1 / Forge 61.1.0 mods.

| Mod | Description |
|-----|-------------|
| [Gamemode-Sign-Mod](Gamemode-Sign-Mod/README.md) | 7 glowing admin signs — toggle gamemode, time, weather, heal, fly, give items |
| [UFO-Mod](UFO-Mod/README.md) | Rideable UFO vehicle with hover/fly modes and weapons |
| [Turret-Mod](Turret-Mod/README.md) | 17 automated defense turrets (Arrow, TNT, Lightning, Laser, and more) |
| [Schem-Builder-Mod](Schem-Builder-Mod/README.md) | Schematic copy-paste tool with holographic preview and WorldEdit-style wand |
| [Minecraft-Mod---Conveyor-Belts](Minecraft-Mod---Conveyor-Belts/README.md) | Full conveyor belt automation system (straight, corner, slope, splitter, merger) |
