# WorldEdit Wand Plugin — Hytale Server Plugin v1.0.0

A Hytale server plugin that adds **WorldEdit-style block editing tools** — select two corners with a wand, then fill, copy, paste, or undo large regions instantly.

---

## Quick Start

```
/we wand          — get the wand item
Left-click block  — set Pos1 (first corner)
Right-click block — set Pos2 (second corner)
/we set Rock_Stone — fill selection with stone
/we undo          — undo last operation
```

---

## All Commands

| Command | Description |
|---------|-------------|
| `/we wand` | Give yourself the WorldEdit Wand |
| `/we pos1` | Set Pos1 at your current feet position |
| `/we pos2` | Set Pos2 at your current feet position |
| `/we set <blockId>` | Fill the entire selection with a block |
| `/we walls <blockId>` | Fill only the 4 side walls (not floor/ceiling) |
| `/we clear` | Fill selection with air (delete all blocks) |
| `/we copy` | Copy selection to clipboard (origin = Pos1 corner) |
| `/we paste` | Paste clipboard at your current feet position |
| `/we undo` | Undo the last fill or paste operation |
| `/we size` | Show selection dimensions and total block count |

---

## Wand Controls

With the **WorldEdit Wand** item in hand:
- **Left Mouse Button** → Sets **Pos1** (first corner of selection)
- **Right Mouse Button** → Sets **Pos2** (second corner of selection)

Each click shows a confirmation message with coordinates and total blocks selected.

> You can also set positions with `/we pos1` and `/we pos2` commands if clicking doesn't work.

---

## Example Workflows

### Build a stone room (hollow box)
```
1. /we wand                         — get the wand
2. Left-click one corner            — Pos1 set to (10, 64, 10)
3. Right-click opposite corner      — Pos2 set to (20, 70, 20) — 1331 blocks
4. /we walls Rock_Stone             — fills the 4 side walls only
5. /we set Rock_Stone               — (alternative: fill everything solid)
6. /we undo                         — restores if you made a mistake
```

### Clear land and build a platform
```
1. Select a flat area with the wand
2. /we clear                        — removes all blocks in selection
3. /we pos1                         — set floor level with pos1
4. Walk up one block, /we pos2      — thin 1-block-tall selection
5. /we set Soil_Dirt                — lay a dirt floor
```

### Copy and paste a structure
```
1. Select a structure with the wand
2. /we copy                         — copies blocks (origin at Pos1)
3. Walk to new destination
4. /we paste                        — pastes with origin at your feet
5. /we undo                         — undo paste if placed wrong
```

---

## Block IDs Reference

Block IDs are **case-sensitive**. Use underscores, no spaces. Full list in `Assets.zip/Server/BlockTypeList/`.

### Rock & Stone
| Block ID | Description |
|----------|-------------|
| `Rock_Stone` | Standard grey stone |
| `Rock_Stone_Mossy` | Mossy stone |
| `Rock_Marble` | White marble |
| `Rock_Sandstone` | Tan sandstone |
| `Rock_Sandstone_Red` | Red sandstone |
| `Rock_Sandstone_White` | White sandstone |
| `Rock_Shale` | Dark layered shale |
| `Rock_Slate` | Smooth dark slate |
| `Rock_Basalt` | Dark volcanic basalt |
| `Rock_Quartzite` | White/grey quartzite |
| `Rock_Calcite` | Light calcite |
| `Rock_Chalk` | White chalk |
| `Rock_Bedrock` | Unbreakable bedrock |

### Soil & Dirt
| Block ID | Description |
|----------|-------------|
| `Soil_Dirt` | Standard brown dirt |
| `Soil_Grass` | Grass-covered dirt |
| `Soil_Grass_Full` | Dense full grass |
| `Soil_Grass_Dry` | Dry/arid grass |
| `Soil_Grass_Cold` | Cold tundra grass |
| `Soil_Gravel` | Grey gravel |
| `Soil_Gravel_Sand` | Sandy gravel |
| `Soil_Pebbles` | Small pebbles |

### Wood
| Block ID | Description |
|----------|-------------|
| `Wood_Oak_Trunk` | Oak log/trunk |
| `Wood_Pine_Trunk` | Pine log/trunk |
| `Wood_Birch_Trunk` | Birch log |
| `Wood_Fir_Trunk` | Fir log |
| `Wood_Redwood_Trunk` | Redwood log |
| `Wood_Oak_Trunk_Full` | Full oak log (all sides) |
| `Wood_Crystal_Trunk` | Glowing crystal wood |
| `Wood_Petrified_Trunk` | Stone-like petrified wood |
| `Wood_Bamboo_Trunk` | Bamboo stalk |

> Each tree type also has: `_Trunk_Full`, `_Branch_Short`, `_Branch_Long`, `_Branch_Corner`, `_Roots`

### Ores (decorative fills)
| Block ID | Description |
|----------|-------------|
| `Ore_Iron_Stone` | Iron ore in stone |
| `Ore_Gold_Stone` | Gold ore in stone |
| `Ore_Cobalt_Stone` | Cobalt ore |
| `Ore_Mithril_Stone` | Mithril ore |
| `Ore_Adamantite_Stone` | Adamantite ore |
| `Ore_Silver_Stone` | Silver ore |

---

## Limits

- Maximum **100,000 blocks** per operation (prevents server lag)
- Only **one undo level** is stored — undo immediately after each operation
- Pos2 and Pos1 can be set in any order — the plugin calculates min/max automatically

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Unknown block ID: Rock_Stone` | Block IDs are case-sensitive — `Rock_Stone` not `rock_stone` or `Stone` |
| Wand clicks don't set positions | Use `/we pos1` and `/we pos2` commands instead |
| Selection filled wrong area | Use `/we undo` immediately — only one undo level saved |
| `/we set` changed nothing | Check `/we size` to confirm you have a valid selection first |

---

## Installation

1. Copy `WorldEditWandPlugin-1.0.0.jar` to:
   ```
   ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
   ```
2. Launch the world
3. Run `/we wand` to get started

### Build from Source
```bash
export JAVA_HOME=/home/linuxbrew/.linuxbrew/Cellar/openjdk/25.0.2/libexec
export PATH=$JAVA_HOME/bin:$PATH
cd /home/deck/WorldEditWandPlugin
./gradlew shadowJar
cp build/libs/WorldEditWandPlugin-1.0.0.jar ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
```

---

## Technical Notes

- Requires Hytale build `>= 2026.02.19`
- Copy stores blocks relative to the Pos1 corner; Paste places Pos1 at your feet
- `/we clear` sets blocks to `BlockType.EMPTY_KEY` — the game's internal air/empty constant
- The undo buffer stores a full snapshot of the pre-operation region — large operations use more RAM temporarily
