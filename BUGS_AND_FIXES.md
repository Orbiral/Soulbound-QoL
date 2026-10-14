<div align="center">

# Soulbound Bug Analysis & Technical Fixes
### *A Decompiled Deep-Dive into Upstream Soulbound (Turn Battle) 2.0.2 – 2.0.7*

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](#)
[![Target Mod](https://img.shields.io/badge/Target-turnbattle%202.0.2--2.0.7-red.svg)](#)
[![Addon](https://img.shields.io/badge/Addon-Soulbound%20QoL-blue.svg)](#)
[![Status](https://img.shields.io/badge/Status-6%20Bugs%20Patched-brightgreen.svg)](#)

---

</div>

> This document provides a comprehensive technical breakdown of the critical bugs present in the original **Soulbound (Turn Battle)** mod (versions 2.0.2 through 2.0.7).
> 
> Each section details the player-facing issue, breaks down the underlying root cause from the original decompiled bytecode, and explains the non-invasive Mixin fixes implemented in **Soulbound QoL**.

---

## Quick Reference: Upstream Bugs Patched

| Bug | System Affected | Original Mod Flaw | Soulbound QoL Resolution |
| :--- | :--- | :--- | :--- |
| **[1. Mob Damage Scaling](#1-quadratic-mob-damage-double-scaling-high-level-damage-spikes)** | Combat & Balance | Level multiplier applied twice, causing 400+ raw damage spikes that hit the 50% max HP clamp every hit (2-hit KO). | Baseline attribute extraction with linear, balanced damage scaling. |
| **[2. Companion Equipment](#2-companion-armor-defense--weapon-attack-omission)** | Party Stats & Combat | Equipped armor and weapons were 100% cosmetic in RPG calculations; defense and attack bonuses were ignored. | Injects armor defense points and weapon attack bonuses directly into recruit battle calculations. |
| **[3. Custom Recruit Stat Resets](#3-companion--custom-recruit-stat-reset-desync--failure)** | Party GUI & Networking | Resetting custom recruit stats with Netherite Ingots crashed the server or devoured ingots without refunding points. | Custom recruit index resolver (`>= 1000` / `20000`) and dirty state synchronization. |
| **[4. Multi-Mob Battle Loot](#4-multi-enemy-battle-drop-loss-non-primary-enemies-dropping-nothing)** | Battle Rewards | Killing non-primary enemies in multi-mob encounters discarded them with zero item drops; only the final mob dropped loot. | Slain extra enemies roll and drop their complete loot tables immediately upon defeat. |
| **[5. Vanilla Experience Omission](#5-complete-omission-of-vanilla-minecraft-experience-exp)** | Progression & Enchanting | Defeating enemies in RPG encounters awarded zero vanilla Minecraft experience orbs. | Authentic vanilla Minecraft XP orbs spawned on victory, scaled to mob level and elite tier. |
| **[6. Companion Health Resets](#6-companion-health-reset-to-100-between-encounters)** | Party Persistence | Companion HP was hard-reset to 100% at the start of every encounter, making battle damage ephemeral. | Persistent recruit HP tracking across battles stored in world save data. |

---

## 1. Quadratic Mob Damage Double-Scaling (High-Level Damage Spikes)

### The In-Game Problem
When encountering enemies at levels above 30–40 (such as Level 41 Blazes or Level 50 Zombies), mobs dealt absurd, game-breaking damage (115 to 400+ raw damage per hit).

The upstream developer attempted to patch over these damage spikes with a safety clamp capping single-hit damage to at most **50% of maximum HP** (`Math.min(targetMaxHp * 0.5f, damage)`). However, because the quadratic calculation produced numbers so far above player defense values, **every single attack reliably hit the 50% damage ceiling**. This locked combat into an inevitable, artificial **2-hit knockout** regardless of the player's vitality, shields, or defense stats, rendering defensive progression useless at high levels.

### Under the Hood: Decompiled Analysis
In the original mod, mob leveling modifies attack damage in two completely independent locations:

1. **At Entity World Spawn (`MobLevelManager.applyMobLevel`)**:
   When a mob spawns into the world, the mod scales its base attribute:
   ```java
   EntityAttributeInstance attr = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
   double scaled = baseDamage * (1.0 + (double)level * 0.08);
   attr.setBaseValue(scaled);
   ```

2. **In Battle Damage Calculation (`BattleManager.getEnemyDamage`)**:
   When that same mob attacks during a turn-based battle, `BattleManager` calculated damage as:
   ```java
   float baseDamage = (float)enemy.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
   int mobLevel = MobLevelManager.getMobLevel(enemy);
   float levelMultiplier = 1.0f + (float)mobLevel * 0.08f;
   float finalDamage = baseDamage * levelMultiplier; // Level multiplier applied a second time!
   ```

Because `getAttributeValue()` had **already** been multiplied by `(1 + 0.08 * level)` at entity spawn, multiplying it by `(1 + 0.08 * level)` a second time produced quadratic scaling:
$$\text{Damage}_{\text{upstream}} \propto \text{Base} \times (1 + 0.08 \times \text{Level})^2 \approx O(\text{Level}^2)$$

For a Level 45 Blaze with a base damage of 6:
* Intended linear scaling: $6 \times (1 + 0.08 \times 45) = 27.6\text{ damage}$.
* Upstream quadratic calculation: $6 \times (4.6) \times (4.6) = 126.96\text{ raw damage}$ (reaching 400+ with criticals and high-tier elite modifiers).

3. **The 50% Max HP Band-Aid Clamp (`BattleManager.applySoulHitDamage`)**:
   In `applySoulHitDamage`, upstream applied:
   ```java
   // Player hit:
   damage = Math.min(player.getMaxHealth() * 0.5f, damage);

   // Party recruit hit:
   damage = Math.min(recruitMaxHp * 0.5f, damage);
   ```
   Because 120–400+ raw damage easily exceeded 50% max HP even after `CharStats.afterDefense`, the band-aid clamp was hit on virtually every attack, locking combat into a guaranteed 2-hit kill.

### How Soulbound QoL Fixes It
`soulbound-qol` intercepts `BattleManager.getEnemyDamage` at `@At("HEAD")`, retrieves the unscaled baseline attribute for the entity type, and computes a clean linear scaling formula:

```java
@Inject(method = "getEnemyDamage", at = @At("HEAD"), cancellable = true, remap = false)
private static void onGetEnemyDamage(LivingEntity enemy, int playerLevel, CallbackInfoReturnable<Float> cir) {
    if (enemy == null) return;
    int mobLevel = MobLevelManager.getMobLevel(enemy);
    if (mobLevel > 0) {
        cir.setReturnValue(QoLHelper.getFixedEnemyDamage(enemy, playerLevel));
    }
}
```

$$\text{Damage}_{\text{fixed}} = \text{UnscaledVanillaBase} \times (1.0 + 0.075 \times \text{MobLevel})$$

---

## 2. Companion Armor Defense & Weapon Attack Omission

### The In-Game Problem
Equipping armor (Helmets, Chestplates, Leggings, Boots) and weapons on party companions or custom vessels changed their visual models on-screen, but provided **zero benefit** to damage dealt and zero damage reduction in battle. A companion wearing full Netherite Armor took the exact same damage as one wearing nothing.

### Under the Hood: Decompiled Analysis
Two critical oversights existed in upstream's recruit combat calculation:

1. **Stat Resolution (`BattleManager.getRecruitEffectiveStats`)**:
   ```java
   public static CharStats getRecruitEffectiveStats(RecruitData.RecruitState rs) {
       CharStats s = new CharStats();
       s.copyFrom(rs.stats);
       // rs.equippedWeapon and rs.equippedArmor were completely ignored!
       return s;
   }
   ```
   Upstream copied only the raw stat points allocated by the player, completely omitting the `WeaponStats` and `ArmorStats` stored on the recruit's equipment.

2. **Incoming Damage Mitigation (`BattleManager.applySoulHitDamage`)**:
   When an enemy attacked a companion in battle, upstream applied incoming damage directly to `state.participantHp`:
   ```java
   state.participantHp.put(slot, Float.valueOf(oldHp - rawDamage));
   ```
   It never calculated armor defense points or protection factors for party members.

### How Soulbound QoL Fixes It
1. **Stat Injection**: `BattleManagerMixin` intercepts `getRecruitEffectiveStats`, adding `WeaponStats.attack()` and `magic()` to offense/magic, and summing `ArmorStats.defense()` from all equipped armor slots into `CharStats.defense`.
2. **Armor Mitigation in Battle**: `BattleManagerMixin` redirects the `participantHp.put` call in `applySoulHitDamage` to run `QoLHelper.getPartyArmorFactor(...)`, reducing incoming damage according to the companion's equipped armor defense points before applying HP reduction.

---

## 3. Companion & Custom Recruit Stat Reset Desync / Failure

### The In-Game Problem
Attempting to reset stat points for companions or custom recruits using a Netherite Ingot in the Party menu either:
1. Crashed the server with an `IndexOutOfBoundsException`, or
2. Consumed the Netherite Ingot without resetting stats or refunding points.

### Under the Hood: Decompiled Analysis
In upstream `TurnBattleMod.java`, the network receiver for `turnbattle:stat_reset` assumed recruit indices always mapped to the standard recruit enum (`Recruit.values()`):

```java
int rawTargetType = buf.readInt();
if (rawTargetType >= 1) {
    int seen = 0;
    for (Recruit r : Recruit.values()) {
        if (!pr.recruits[r.ordinal()].unlocked || ++seen != rawTargetType) continue;
        matched = r;
        break;
    }
}
```

When resetting a custom recruit, the client GUI sent `1000 + ci` (or `20000 + ci` in later updates). Upstream did not correctly handle target IDs $\ge 1000$, resulting in failed lookups or `IndexOutOfBoundsException` when indexing `pr.customRecruits`.

### How Soulbound QoL Fixes It
`SoulboundQoL` replaces the upstream `STAT_RESET` receiver on `ServerLifecycleEvents.SERVER_STARTING` with a robust handler:

```java
public static int resolveRecruitTargetIndex(int targetType) {
    if (targetType >= 20000) {
        return targetType - 20000;
    }
    return targetType - 1000;
}
```

If `rawTargetType >= 1000`, the handler resolves the index against `pr.customRecruits`, resets `stats.reset()`, calls `rd.markDirty()`, and immediately broadcasts `TurnBattleMod.sendRecruitInfo(player)` to refund the points on the client GUI.

---

## 4. Multi-Enemy Battle Drop Loss (Non-Primary Enemies Dropping Nothing)

### The In-Game Problem
When fighting multiple enemies in a single battle (e.g., a group of 3 Blazes or 3 Skeletons), defeating the first two enemies yielded **no items and no drops**. Only the very last enemy slain dropped loot upon battle termination.

### Under the Hood: Decompiled Analysis
In upstream `BattleManager`:
When a non-primary enemy was defeated during an encounter, `BattleManager.promoteNextEnemy` promoted the next enemy in line and discarded the dead enemy using:

```java
BattleManager.discardEnemy(entity);
```

`discardEnemy` called `entity.discard()` directly without triggering loot table generation or mob death drop events. Drops were only invoked in `endBattle()` for the single currently active entity.

### How Soulbound QoL Fixes It
`soulbound-qol` intercepts `promoteNextEnemy`:

```java
@Redirect(
    method = "promoteNextEnemy",
    at = @At(value = "INVOKE", target = "Lcom/pixelcite/turnbattle/BattleManager;discardEnemy(Lnet/minecraft/entity/Entity;)V"),
    remap = false
)
private static void onDiscardEnemyInPromote(Entity entity, BattleState state, ServerWorld world) {
    QoLHelper.onDeadExtraDiscarded(state, world, entity);
    if (entity != null && !(entity instanceof PlayerEntity)) {
        entity.discard();
    }
}
```

`onDeadExtraDiscarded` extracts the defeated mob's loot table and generates drop rolls immediately before the entity is discarded, guaranteeing that all enemies slain during multi-mob battles drop their rightful loot.

---

## 5. Complete Omission of Vanilla Minecraft Experience (EXP)

### The In-Game Problem
Defeating enemies in turn-based battles awarded internal mod stat points but **zero vanilla Minecraft experience points**. Players could fight dozens of high-level battles and remain at Level 0 in vanilla, leaving enchanting, anvil repairs, and Mending unusable without grinding outside the mod.

### Under the Hood: Decompiled Analysis
Because `BattleManager` concludes encounters by manually discarding entities or bypassing the vanilla death loop (`LivingEntity.dropXp`), vanilla Minecraft's `ExperienceOrbEntity` spawning logic was never invoked.

### How Soulbound QoL Fixes It
Whenever an enemy is defeated in battle, `soulbound-qol` calculates authentic vanilla experience scaled to the mob's level and elite status:

```java
int xp = Math.max(1, (int)(mobLevel * 2.5f));
int eliteTier = EliteMobManager.getTier(entity.getUuid());
if (eliteTier >= 0 && eliteTier < EliteMobManager.EXP_MULT.length) {
    xp = Math.round((float)xp * EliteMobManager.EXP_MULT[eliteTier]);
}
if (xp > 0) {
    ExperienceOrbEntity.spawn(world, entity.getPos(), xp);
}
```

Defeating enemies now spawns real Minecraft experience orbs.

---

## 6. Companion Health Reset to 100% Between Encounters

### The In-Game Problem
If a companion took heavy damage and survived a battle with 1 HP, the next battle started with them immediately back at full health. Companion damage was ephemeral, making healing items outside of active combat meaningless.

### Under the Hood: Decompiled Analysis
In upstream `BattleManager.startBattle(...)`:

```java
for (int i = 0; i < party.size(); i++) {
    party.get(i).currentHp = party.get(i).maxHp; // Forced reset to max HP on battle start!
}
```

Upstream never read or persisted the recruit's current HP field (`RecruitState.hp`) across battle sessions.

### How Soulbound QoL Fixes It
`BattleManagerMixin` injects into `BattleManager.endBattle`:
* If the battle ended in victory or mercy, the companion's remaining HP from `state.participantHp` is saved to their persistent `RecruitState` in world data.
* Subsequent battles read the saved HP value instead of resetting to `maxHp`.
* If a battle ends in a wipe or retreat, party health is safely restored.
