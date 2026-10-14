<div align="center">

# Soulbound QoL
### *The Complete Balance, Immersion & Quality-of-Life Expansion for Soulbound*

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](#)
[![Fabric Loader](https://img.shields.io/badge/Loader-Fabric-blue.svg)](#)
[![Requires Soulbound](https://img.shields.io/badge/Requires-Soulbound%20(Turn%20Battle)-red.svg)](#)
[![GitHub Repository](https://img.shields.io/badge/GitHub-Orbiral%2FSoulbound--QoL-blue.svg)](https://github.com/Orbiral/Soulbound-QoL)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/Orbiral/Soulbound-QoL/blob/main/LICENSE)

---

</div>

> **Soulbound QoL** is a comprehensive, client-and-server Fabric addon for the **Soulbound (Turn Battle)** mod (1.21.1).
> 
> It resolves punishing late-game damage scaling bugs, empowers your party members with real armor and weapon combat benefits, adds true soul swapping without losing your active traits, connects RPG victories to vanilla Minecraft experience and enchanting, and introduces dynamic Light and Dark World companion skins for a seamless Undertale/Deltarune RPG experience in Minecraft.

---

## Key Features

### Balanced Combat & Fair Progression
* **Fair Late-Game Mob Scaling**: In the original mod, mob leveling double-multiplied damage, resulting in massive damage spikes (400+ raw damage) that constantly slammed against upstream's 50% max HP damage ceiling—forcing artificial 2-hit knockouts regardless of vitality or defense. Soulbound QoL restores clean, linear scaling so your defensive stats and shields actually protect you.
* **Companions Benefit from Gear**: Equipping armor and weapons on companions or custom vessels used to be purely cosmetic in battle. Now, **armor defense reduces incoming damage** and **weapon attack bonuses boost party damage output**!
* **Persistent Companion Health**: Companions no longer magically heal to 100% HP immediately after surviving a tough battle. Damage carries over between encounters, making out-of-combat healing items and strategy meaningful.
* **Vanilla Experience (XP) on Victory**: Defeating enemies in turn-based combat now drops authentic vanilla Minecraft experience orbs scaled to enemy level and elite status. Connect your RPG victories directly to vanilla enchanting, anvil repairs, and Mending!
* **Rock-Solid Stat Resets**: Resetting stats on custom recruits with a Netherite Ingot no longer causes network desyncs or crashes.

---

### Soul Mastery: True Swapping & Extraction
* **True Soul Swapping**: Using a physical Soul item from your inventory while another soul is active **no longer deletes your current soul**! Your active soul is safely extracted and refunded back into your inventory.
* **Dual-Mode Soul Extraction**:
  * **Press Alt**: Standard Soul Walk spirit projection (flight mode).
  * **Hold Shift + Press Alt**: Safely rips out your physical soul into an inventory item, returning your character to vanilla Minecraft mechanics.
* **Anti-Item-Wipe Protection**: Stops upstream's login routine from clearing physical soul items out of your inventory upon joining the world.

---

### Customization & Companion Skins
* **Dynamic Light & Dark World Companion Skins**: Custom vessels automatically swap skins when traveling between the Light World and Dark World. To set this up:
  1. Open `.minecraft/config/soulbound-recruit-skins/` (created automatically on first launch).
  2. Save your standard Minecraft skin file named after your custom vessel: `<vessel_name>.png` (e.g. `vessel.png`).
  3. Save the Dark World outfit variant as `<vessel_name>_dark.png` (or `<vessel_name>-dark.png`).
  * When your party steps into the Dark World, Soulbound QoL automatically detects the active companions by name and swaps their models in real time without needing to re-import or restart. (See the setup guide below for advanced options).
* **Dark Fountain Party Transitions**: As an added visual touch, the Dark Fountain dive transition only displays the companions currently active in your party.

---

## Original Mod Bugfixes at a Glance

Soulbound QoL fixes several critical bugs present in upstream **Soulbound 2.0.2 – 2.0.7**:

| Issue | Upstream Behavior | Soulbound QoL Fix |
| :--- | :--- | :--- |
| **Mob Damage Scaling** | Quadratic double-scaling caused massive damage spikes hitting the 50% max HP clamp every turn (guaranteed 2-hit KO). | Clean linear scaling derived from base mob attributes. |
| **Companion Equipment** | Equipped armor defense and weapon attack were 100% ignored in combat calculations. | Injects armor defense points and weapon attack values directly into battle formulas. |
| **Multi-Mob Battle Loot** | Defeating non-primary enemies in group battles yielded zero items; only the final mob dropped loot. | Slain extra enemies roll and drop their complete loot tables immediately upon defeat. |
| **Vanilla EXP Drops** | Defeating mobs in RPG battles awarded 0 vanilla Minecraft experience points. | Spawns authentic XP orbs scaled to enemy level and elite tier upon victory. |
| **Companion Health Reset** | Companion HP instantly reset to full between encounters, ignoring battle damage. | Persists companion HP across battles in world save data. |
| **Custom Recruit Stat Reset** | Using Netherite Ingots to reset custom vessels desynced or crashed with array index errors. | Correctly parses custom vessel IDs (`>= 1000` / `20000`) and safely refunds points. |

> [!NOTE]
> For a full technical post-mortem with decompiled bytecode and mathematical formulas, check out **[`BUGS_AND_FIXES.md`](./BUGS_AND_FIXES.md)**.

---

## Custom Companion Skins Setup Guide

Want your custom party members to have unique skins that automatically transform when entering the Dark World? Follow these simple steps:

### Step 1: Locate the Skins Folder
Navigate to the `soulbound-recruit-skins` folder inside your Minecraft configuration directory:

* **Windows**: `%appdata%\.minecraft\config\soulbound-recruit-skins\`
* **macOS**: `~/Library/Application Support/minecraft/config/soulbound-recruit-skins/`
* **Linux**: `~/.minecraft/config/soulbound-recruit-skins/`

*(This folder is created automatically on startup by Soulbound QoL).*

### Step 2: Add Your PNG Skins
Name your standard Minecraft skin files (`64x64` PNG format) after the in-game name of your custom vessel:

* **Light World Form**: `<vessel_name>.png`
* **Dark World Form**: `<vessel_name>_dark.png` *(also supports `<name>-dark.png`, `<name>_darkworld.png`, or `<name>_dw.png`)*

#### Example Directory Layout
```
soulbound-recruit-skins/
├── vessel.png           <-- Light World skin for vessel named "Vessel"
├── vessel_dark.png      <-- Dark World armor/outfit for vessel named "Vessel"
├── companion.png        <-- Light World skin for vessel named "Companion"
└── companion_dark.png   <-- Dark World outfit for vessel named "Companion"
```

> [!TIP]
> Skin names are case-insensitive across all operating systems (Windows, macOS, and Linux). If a companion does not have a `_dark` file, the mod will gracefully fall back to their standard Light World skin in the Dark World.

### Advanced: Custom File Paths (`recruit_skin_paths.json`)
If you keep your skins in an external folder outside `.minecraft`, you can create a `recruit_skin_paths.json` file inside the `soulbound-recruit-skins` directory with exact paths (supports Windows paths, macOS/Linux `~` home directory, and environment variables):
```json
{
  "vessel": "/path/to/vessel_light.png",
  "vessel_dark": "/path/to/vessel_dark.png"
}
```

---

## Installation & Requirements

### Requirements
* **Minecraft**: `1.21.1`
* **Fabric Loader**: `>= 0.15.0`
* **Fabric API**: compatible with `1.21.1`
* **Soulbound (Turn Battle)**: `2.0.7` *(Base Mod)*
* **Supported OS**: Windows, macOS, and Linux

### How to Install
1. Make sure you have **Fabric Loader**, **Fabric API**, and **Soulbound (Turn Battle)** installed.
2. Download `soulbound-qol-1.0.0.jar`.
3. Drop the `.jar` file into your Minecraft instance's `mods/` folder.
4. Launch the game and enjoy a balanced, polished RPG experience!

---

<details>
<summary><b>Building from Source (Developers)</b></summary>

### Prerequisites
* JDK 21+

### Build Commands

* **Linux / macOS**:
  ```bash
  ./build.sh
  ```
  *(To automatically install to your local profile: `./build.sh --install`)*

* **Windows**:
  ```cmd
  build.bat
  ```
  *(To automatically install to your local profile: `build.bat --install`)*
</details>

---

## Compatibility & Philosophy

Soulbound QoL is designed as a **clean, non-invasive Fabric addon**. It modifies upstream mod behavior at runtime using Sponge Powered Mixin, without redistributing or altering any closed-source assets from the original author.

### License
Released under the [MIT License](LICENSE).
