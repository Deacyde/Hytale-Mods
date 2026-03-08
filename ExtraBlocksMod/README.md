# Extra Blocks Mod — Hytale Asset Pack v1.0.0

Adds **4 new decorative blocks** to Hytale. All blocks appear in the creative inventory and can be placed and broken like normal blocks.

---

## Blocks

| Block ID | Name | Description |
|----------|------|-------------|
| `Glowstone` | Glowstone | Glowing light-emitting block, warm yellow glow |
| `Gas_Block` | Gas Block | Decorative green gas/crystal block, semi-transparent appearance |
| `Proximity_Mine` | Proximity Mine | Decorative mine block (no explosion mechanic — purely cosmetic) |
| `Magnet_Block` | Magnet Block | Decorative magnet block with distinct texture |

---

## How to Use

### In Creative Mode
1. Open the creative inventory
2. The blocks appear in the item list — search by name if needed
3. Place them in the world like any normal block
4. Break them normally to pick up

### With WorldEdit Wand
You can fill regions with these blocks using the WorldEdit Wand plugin:
```
/we set Glowstone         — fill selection with Glowstone
/we set Gas_Block         — fill with Gas Block
/we walls Glowstone       — line the walls of a room with Glowstone
```

---

## Installation

1. Copy `ExtraBlocks-1.0.0.zip` to:
   ```
   ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
   ```
2. Launch the world — blocks appear in the creative inventory automatically

> **Asset pack only** — no Java plugin needed. The `.zip` is loaded directly by the game.

---

## Notes

- Requires Hytale build `>= 2026.02.19`
- Blocks have full collision and can be placed/broken normally
- The Proximity Mine block has no explosion mechanic — it is purely a decorative prop
- `Gas_Block` was reused as the icon template for the SpawnBlock and WorldEdit Wand plugin items
