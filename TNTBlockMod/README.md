# TNT Block Mod — Hytale Asset Pack v1.0.1

Adds **two throwable explosive weapons** to Hytale — a TNT Block for targeted demolition and a Nuke Block for massive area destruction.

---

## Items

| Item ID | Name | Quality | Description |
|---------|------|---------|-------------|
| `TNT_Block` | TNT Block | Uncommon (red) | Throwable explosive — moderate radius |
| `Nuke_Block` | Nuke Block | Epic (yellow) | Throwable nuke — massive explosion radius |

---

## How to Use

### Get the Items
Both items appear in the **creative inventory** automatically after loading the mod.

### Throw to Explode
1. Select the item in your hotbar
2. **Throw it** — hold and release / drop from hand forward
3. The block flies in a physics arc and **explodes on impact** with terrain or entities

### TNT Block
- Moderate explosion radius — good for precision demolition
- Useful for: clearing rooms, mining veins, removing structures
- Has a visual explosion effect on detonation

### Nuke Block
- **Massive explosion** — approximately 10× the radius of TNT
- Levels entire hills and large structures in one throw
- Deals heavy damage to all nearby entities
- **Use with extreme caution** — very destructive and hard to contain

---

## Explosion Comparison

| Item | Radius | Best For |
|------|--------|----------|
| TNT Block | Moderate | Targeted demolition, mining |
| Nuke Block | Massive (10× TNT) | Clearing large areas, terrain shaping |

---

## Installation

1. Copy `TNTBlock-1.0.1.zip` to:
   ```
   ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
   ```
2. Launch the world — both items appear in the creative inventory automatically

> **Asset pack only** — no Java plugin required. Load the `.zip` directly.

---

## Notes

- Requires Hytale build `>= 2026.02.19`
- This supersedes the original `NukeMod` — use this version instead (it includes both TNT and Nuke)
- Explosions use Hytale's built-in destructible terrain system
- The Nuke Block is the same item as in `NukeMod` but with refined explosion config
