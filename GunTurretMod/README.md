# Gun Turret Mod — Hytale Asset Pack v1.0.0

The **asset pack** component of the Gun Turret system. Provides the turret model, bullet model, item definition, and projectile configs. Must be paired with `GunTurretPlugin-1.0.0.jar` for the turret to actually fire.

---

## Contents

| Asset ID | Type | Description |
|----------|------|-------------|
| `Weapon_Deployable_Gun_Turret` | Item | The throwable turret item (Rare quality, Level 40) |
| `Gun_Turret` | Entity model | The deployed turret structure |
| `Gun_Turret_Bullet` | Projectile model | Bullet fired by the turret |
| `Projectile_Config_Gun_Turret_Deploy` | Config | Deploy throw arc and physics config |

---

## How to Use

**This asset pack alone does not fire bullets.** The vanilla `DeployableProjectileShooterComponent.spawnProjectile()` method is a no-op stub in the current Hytale build — the model deploys, but no shooting occurs without the Java plugin.

**For a fully working turret, install both:**

| File | Role |
|------|------|
| `GunTurret-1.0.0.zip` | This file — model, item, projectile assets |
| `GunTurretPlugin-1.0.0.jar` | Java plugin — target scanning and bullet spawning |

### With Both Files Installed
1. Copy both files to `UserData/Mods/`
2. Launch the world
3. Run `/turret` to get the item
4. **Throw the item** — the turret deploys and auto-fires at nearby mobs

See [GunTurretPlugin/README.md](../GunTurretPlugin/README.md) for full usage and stats.

---

## Installation

```
~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
├── GunTurret-1.0.0.zip         ← this file
└── GunTurretPlugin-1.0.0.jar   ← also required
```

---

## Notes

- Requires Hytale build `>= 2026.02.19`
- Asset pack loads as a `.zip` — no compilation needed
- If you load only the zip without the plugin, you can still throw and deploy the turret model, but it won't shoot
