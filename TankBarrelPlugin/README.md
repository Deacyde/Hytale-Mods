# TankBarrelPlugin

A deployable tank barrel for Hytale. Throw it to plant it, then load and fire TNT or Nuke shells.

## Install

Copy `TankBarrelPlugin-1.0.0.jar` to:
```
~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
```

## Commands

| Command | Description |
|---------|-------------|
| `/barrel` or `/barrel give` | Get a Tank Barrel item (auto-faces your direction) |
| `/barrel give [n\|e\|s\|w]` | Get a barrel facing a specific direction |
| `/barrel load tnt` | Load 5 TNT shells into nearest barrel |
| `/barrel load nuke` | Load 5 Nuke shells into nearest barrel |
| `/barrel fire tnt` | Fire a TNT shell (3s fuse, radius 5 explosion) |
| `/barrel fire nuke` | Fire a Nuke shell (4s fuse, radius 50 explosion) |
| `/barrel ammo` | Check current ammo count |

## Usage

1. `/barrel give` — barrel item lands in your inventory
2. Throw the item to deploy the barrel (it sticks in the ground)
3. `/barrel load tnt` or `/barrel load nuke` — loads 5 shells
4. Aim at a target, then `/barrel fire tnt` or `/barrel fire nuke`
5. Shell fires in your look direction, sticks in terrain, then explodes

## Notes

- Barrel always fires in **your look direction**, not the barrel's facing
- Barrel facing (`n/e/s/w`) is cosmetic — choose for aesthetics
- TNT: block-break radius 5, entity damage radius 10
- Nuke: block-break radius 50, entity damage radius 60 — causes significant lag

## Build

```bash
export JAVA_HOME=/home/linuxbrew/.linuxbrew/Cellar/openjdk/25.0.2/libexec
export PATH=$JAVA_HOME/bin:$PATH
cd TankBarrelPlugin
./gradlew shadowJar -q
cp build/libs/TankBarrelPlugin-1.0.0.jar ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/
```
