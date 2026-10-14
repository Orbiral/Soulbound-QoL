package com.orbiral.soulboundqol;

import com.pixelcite.turnbattle.ArmorStats;
import com.pixelcite.turnbattle.BattleData;
import com.pixelcite.turnbattle.BattleManager;
import com.pixelcite.turnbattle.BattleState;
import com.pixelcite.turnbattle.BestiaryData;
import com.pixelcite.turnbattle.CharStats;
import com.pixelcite.turnbattle.EliteMobManager;
import com.pixelcite.turnbattle.HumanEnemies;
import com.pixelcite.turnbattle.MobLevelManager;
import com.pixelcite.turnbattle.CharacterRace;
import com.pixelcite.turnbattle.Net121;
import com.pixelcite.turnbattle.PartyMember;
import com.pixelcite.turnbattle.Recruit;
import com.pixelcite.turnbattle.RecruitData;
import com.pixelcite.turnbattle.SoulItem;
import com.pixelcite.turnbattle.SoulType;
import com.pixelcite.turnbattle.SoulWalk;
import com.pixelcite.turnbattle.TurnBattleMod;
import com.pixelcite.turnbattle.TurnBattleSounds;
import com.pixelcite.turnbattle.WeaponStats;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.class_124;
import net.minecraft.class_1297;
import net.minecraft.class_1303;
import net.minecraft.class_1308;
import net.minecraft.class_1309;
import net.minecraft.class_1324;
import net.minecraft.class_1510;
import net.minecraft.class_1542;
import net.minecraft.class_1545;
import net.minecraft.class_1560;
import net.minecraft.class_1588;
import net.minecraft.class_1613;
import net.minecraft.class_1627;
import net.minecraft.class_1657;
import net.minecraft.class_173;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_181;
import net.minecraft.class_1937;
import net.minecraft.class_2540;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_5134;
import net.minecraft.class_52;
import net.minecraft.class_5321;
import net.minecraft.class_8567;
import net.minecraft.class_9334;
import net.minecraft.server.MinecraftServer;

public class QoLHelper {

    private static final Set<UUID> DROPPED_SET = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Random RNG = new Random();

    /**
     * Resolves custom recruit index from targetType integer.
     * PartyScreen sends 20000 + ci, while legacy packets may send 1000 + ci.
     */
    public static int resolveRecruitTargetIndex(int targetType) {
        if (targetType >= 20000) {
            return targetType - 20000;
        }
        return targetType - 1000;
    }

    private static void addStatBonus(CharStats s, int statIndex, int amount) {
        if (s == null) return;
        switch (statIndex) {
            case 0 -> s.offense += amount;
            case 1 -> s.defense += amount;
            case 2 -> s.magic += amount;
            case 3 -> s.iq += amount;
            case 4 -> s.vitality += amount;
            case 5 -> s.luck += amount;
        }
    }

    /**
     * Calculates recruit effective stats including equipped weapon and armor stat bonuses.
     */
    public static CharStats getRecruitEffectiveStats(RecruitData.RecruitState rs) {
        if (rs == null || rs.stats == null) return new CharStats();
        WeaponStats ws = WeaponStats.fromStack(rs.equippedWeapon);
        boolean hasWeapon = ws != null;
        boolean hasArmor = rs.equippedArmor != null && (
            (rs.equippedArmor[0] != null && !rs.equippedArmor[0].method_7960()) ||
            (rs.equippedArmor[1] != null && !rs.equippedArmor[1].method_7960())
        );
        if (!hasWeapon && !hasArmor) {
            return rs.stats;
        }
        CharStats effective = rs.stats.copy();
        if (hasWeapon) {
            addStatBonus(effective, ws.bonusStat, ws.bonusAmt);
            if (ws.isQuirk) {
                addStatBonus(effective, ws.quirkStat, 10);
                addStatBonus(effective, ws.quirkMinus, -10);
            }
        }
        if (hasArmor) {
            for (int ai = 0; ai < 2; ++ai) {
                class_1799 piece = rs.equippedArmor[ai];
                if (piece == null || piece.method_7960()) continue;
                ArmorStats as = ArmorStats.fromStack(piece);
                if (as == null) continue;
                addStatBonus(effective, as.bonusStat, as.bonusAmt);
            }
        }
        return effective;
    }

    public static RecruitData.RecruitState resolveRecruitState(class_3222 player, BattleState state, UUID slot) {
        if (player == null || state == null || slot == null || player.method_5682() == null) {
            return null;
        }
        RecruitData rd = RecruitData.get(player.method_5682());
        if (rd == null) return null;
        RecruitData.PlayerRecruits pr = rd.getOrCreate(state.playerUuid);
        if (pr == null) return null;
        boolean isSlot2 = slot.equals(BattleState.PARTY_MEMBER_UUID_2);
        int slotCode = isSlot2 ? pr.secondActiveRecruit : pr.activeRecruit;
        if (slotCode >= 1000) {
            int ci = slotCode - 1000;
            if (ci >= 0 && ci < pr.customRecruits.size()) {
                return pr.customRecruits.get(ci).state;
            }
        } else if (slotCode >= 0 && slotCode < Recruit.values().length) {
            return pr.recruits[slotCode];
        } else if (!isSlot2) {
            Recruit activeRecruit = BattleManager.getActiveRecruitEnum(player.method_5682(), state.playerUuid);
            if (activeRecruit != null) {
                return pr.get(activeRecruit);
            }
        }
        return null;
    }

    /**
     * Calculates the multiplicative armor damage reduction factor for a party slot.
     */
    public static float getPartyArmorFactor(class_3222 player, BattleState state, UUID slot, BattleData.PlayerProgress progD) {
        if (slot == null) return 1.0f;
        if (slot.equals(BattleState.PARTY_MEMBER_UUID) && progD != null && progD.partyMember != PartyMember.NONE) {
            float factor = 1.0f;
            if (progD.equippedArmor != null) {
                for (class_1799 ap : progD.equippedArmor) {
                    if (ap == null || ap.method_7960()) continue;
                    ArmorStats.Tier tier = ArmorStats.tierOf(ap);
                    if (tier != null) factor *= tier.factor();
                }
            }
            return factor;
        }
        RecruitData.RecruitState rs = resolveRecruitState(player, state, slot);
        if (rs != null && rs.equippedArmor != null) {
            float factor = 1.0f;
            for (int i = 0; i < 2; ++i) {
                class_1799 piece = rs.equippedArmor[i];
                if (piece == null || piece.method_7960()) continue;
                ArmorStats.Tier tier = ArmorStats.tierOf(piece);
                if (tier != null) factor *= tier.factor();
            }
            return factor;
        }
        return 1.0f;
    }

    /**
     * Fixes mob damage calculation to prevent quadratic double-scaling.
     */
    public static float getFixedEnemyDamage(class_1309 enemy, int playerLevel) {
        if (enemy == null) {
            return 2.5f * (0.3f + (float)playerLevel * 0.04f);
        }
        class_1324 atk = enemy.method_5996(class_5134.field_23721);
        float base = atk != null && atk.method_6194() > 0.0 ? (float)atk.method_6194() : Math.max(1.5f, enemy.method_6063() / 8.0f);
        int mobLevel = MobLevelManager.getMobLevel(enemy);
        if (mobLevel > 0) {
            return base;
        }
        int effectiveLevel = Math.max(mobLevel, playerLevel);
        return base * (0.3f + (float)effectiveLevel * 0.04f);
    }

    /**
     * Persists party member HP at battle end for both Slot 1 and Slot 2.
     */
    public static void savePartyMemberHpEndBattle(MinecraftServer server, BattleState state, UUID uUID, boolean restoreParty) {
        if (server == null || state == null || uUID == null) return;
        RecruitData rd = RecruitData.get(server);
        if (rd == null) return;
        RecruitData.PlayerRecruits pr = rd.getOrCreate(uUID);
        if (pr == null) return;

        // Slot 1
        float sHp1 = state.participantHp.getOrDefault(BattleState.PARTY_MEMBER_UUID, Float.valueOf(-1.0f)).floatValue();
        if (sHp1 >= 0.0f) {
            if (pr.activeRecruit >= 1000) {
                int ci = pr.activeRecruit - 1000;
                if (ci >= 0 && ci < pr.customRecruits.size()) {
                    RecruitData.CustomRecruitState cr = pr.customRecruits.get(ci);
                    float crMax = RecruitData.maxHp(cr.state);
                    cr.state.hp = restoreParty ? crMax : Math.max(1.0f, sHp1);
                }
            } else if (pr.activeRecruit >= 0 && pr.activeRecruit < pr.recruits.length) {
                RecruitData.RecruitState rs = pr.recruits[pr.activeRecruit];
                float rMax = RecruitData.maxHp(rs);
                rs.hp = restoreParty ? rMax : Math.max(1.0f, sHp1);
            }
        }

        // Slot 2
        float sHp2 = state.participantHp.getOrDefault(BattleState.PARTY_MEMBER_UUID_2, Float.valueOf(-1.0f)).floatValue();
        if (sHp2 >= 0.0f) {
            if (pr.secondActiveRecruit >= 1000) {
                int ci2 = pr.secondActiveRecruit - 1000;
                if (ci2 >= 0 && ci2 < pr.customRecruits.size()) {
                    RecruitData.CustomRecruitState cr2 = pr.customRecruits.get(ci2);
                    float crMax2 = RecruitData.maxHp(cr2.state);
                    cr2.state.hp = restoreParty ? crMax2 : Math.max(1.0f, sHp2);
                }
            } else if (pr.secondActiveRecruit >= 0 && pr.secondActiveRecruit < pr.recruits.length) {
                RecruitData.RecruitState rs2 = pr.recruits[pr.secondActiveRecruit];
                float rMax2 = RecruitData.maxHp(rs2);
                rs2.hp = restoreParty ? rMax2 : Math.max(1.0f, sHp2);
            }
        }

        rd.method_80();
    }

    /**
     * Drops authentic vanilla XP when enemies are defeated.
     */
    public static void onEnemyDefeated(class_1309 entity, class_3218 world, class_3222 killer) {
        if (world == null || entity == null) {
            return;
        }
        if (entity instanceof class_1510) {
            return;
        }
        int xp = 0;
        try {
            xp = entity.method_59923(world, killer);
        } catch (Throwable ignored) {}
        if (xp <= 0) {
            if (entity instanceof class_1588) {
                xp = 5;
            } else {
                xp = Math.max(1, (int)(entity.method_6063() * 0.25f));
            }
        }
        int mobLevel = 1;
        try {
            mobLevel = MobLevelManager.getMobLevel(entity);
        } catch (Throwable ignored) {}
        if (mobLevel > 1) {
            float levelScale = 1.0f + (float)mobLevel * 0.08f;
            xp = Math.max(xp, Math.round((float)xp * levelScale));
        }
        int eliteTier = EliteMobManager.getTier(entity.method_5667());
        if (eliteTier >= 0 && eliteTier < EliteMobManager.EXP_MULT.length) {
            xp = Math.round((float)xp * EliteMobManager.EXP_MULT[eliteTier]);
        }
        if (xp > 0) {
            class_1303.method_31493(world, entity.method_19538(), xp);
        }
    }

    /**
     * Handles scaled loot dropping, extra attempts, and authentic XP when an enemy dies.
     */
    public static void dropBattleLoot(class_1309 entity, class_3218 world, boolean killed, class_3222 killer) {
        if (entity == null || world == null || !killed) {
            return;
        }

        if (!DROPPED_SET.add(entity.method_5667())) {
            return;
        }

        onEnemyDefeated(entity, world, killer);

        try {
            var m = HumanEnemies.class.getDeclaredMethod("onDefeated", class_1309.class, class_3218.class, class_3222.class);
            m.setAccessible(true);
            m.invoke(null, entity, world, killer);
        } catch (Throwable ignored) {}

        int mobLevel = 1;
        try {
            mobLevel = MobLevelManager.getMobLevel(entity);
        } catch (Throwable ignored) {}
        if (mobLevel <= 0) {
            mobLevel = 1;
        }

        int eliteTier = -1;
        try {
            eliteTier = EliteMobManager.getTier(entity.method_5667());
        } catch (Throwable ignored) {}

        class_5321 lootTableKey = entity.method_5989();
        class_2960 lootTableId = lootTableKey != null ? lootTableKey.method_29177() : null;
        class_52 table = null;
        if (lootTableId != null) {
            table = world.method_8503().method_58576().method_58295(lootTableKey);
        }

        List<class_1799> finalDrops = new ArrayList<>();

        if (table != null && table != class_52.field_948) {
            class_8567.class_8568 params = new class_8567.class_8568(world)
                    .method_51874(class_181.field_1226, (class_1297)entity)
                    .method_51874(class_181.field_24424, entity.method_19538())
                    .method_51874(class_181.field_1231, killer != null ? world.method_48963().method_48802((class_1657)killer) : world.method_48963().method_48830())
                    .method_51877(class_181.field_1233, (class_1657)killer)
                    .method_51877(class_181.field_1230, (class_1297)killer);
            class_8567 paramSet = params.method_51875(class_173.field_1173);

            ObjectArrayList drops = table.method_51878(paramSet);

            int extraRerolls = (mobLevel / 15) + (eliteTier >= 0 ? eliteTier + 1 : 0);
            while ((drops == null || drops.isEmpty()) && extraRerolls > 0) {
                extraRerolls--;
                drops = table.method_51878(paramSet);
            }

            if (drops != null) {
                for (Object o : drops) {
                    if (o instanceof class_1799) {
                        class_1799 st = (class_1799)o;
                        if (!st.method_7960()) {
                            finalDrops.add(st);
                        }
                    }
                }
            }
        }

        if (finalDrops.isEmpty() && mobLevel >= 15) {
            if (entity instanceof class_1560) {
                finalDrops.add(new class_1799(class_1802.field_8634, 1));
            } else if (entity instanceof class_1613) {
                finalDrops.add(new class_1799(class_1802.field_8606, 1));
            } else if (entity instanceof class_1627) {
                if (RNG.nextFloat() < (0.05f + (float)mobLevel * 0.003f)) {
                    finalDrops.add(new class_1799(class_1802.field_8087, 1));
                } else {
                    finalDrops.add(new class_1799(class_1802.field_8606, 2));
                }
            } else if (entity instanceof class_1545) {
                finalDrops.add(new class_1799(class_1802.field_8894, 1));
            }
        }

        int bonusCount = (mobLevel / 15) + (eliteTier >= 0 ? eliteTier + 1 : 0);
        if (bonusCount > 0) {
            for (class_1799 st : finalDrops) {
                if (st.method_7914() > 1) {
                    int extra = (bonusCount / 2) + RNG.nextInt(bonusCount / 2 + 1);
                    extra = Math.max(1, extra);
                    st.method_7939(extra);
                    if (st.method_7947() > st.method_7914()) {
                        st.method_7933(st.method_7914());
                    }
                }
            }
        }

        for (class_1799 drop : finalDrops) {
            if (drop.method_7960()) continue;
            class_1542 itemEntity = new class_1542((class_1937)world, entity.method_23317(), entity.method_23318(), entity.method_23321(), drop);
            itemEntity.method_6982(10);
            world.method_8649((class_1297)itemEntity);
        }
    }

    public static void onBeforePromote(BattleState state, class_3218 world) {
        if (state == null || world == null || state.isVirtualEnemy || state.isPvp) {
            return;
        }
        int oldPrimaryId = state.primaryEnemyId;
        class_1297 oldEnt = world.method_8469(oldPrimaryId);
        class_3222 player = world.method_8503().method_3760().method_14602(state.playerUuid);
        if (oldEnt instanceof class_1309) {
            class_1309 dyingLe = (class_1309)oldEnt;
            if (player != null) {
                try {
                    BestiaryData.get(world.method_8503()).recordDefeat(state.playerUuid, dyingLe.method_5864());
                } catch (Throwable ignored) {}
            }
            dropBattleLoot(dyingLe, world, true, player);
            if (dyingLe instanceof class_1308) {
                BattleManager.unfreezeMob((class_1308)dyingLe);
            }
        }
        if (oldEnt != null && !(oldEnt instanceof class_1657)) {
            try {
                var m = BattleManager.class.getDeclaredMethod("sendFate", BattleState.class, class_3218.class, int.class, class_1297.class, boolean.class);
                m.setAccessible(true);
                m.invoke(null, state, world, oldPrimaryId, oldEnt, false);
            } catch (Throwable ignored) {}
            oldEnt.method_31472();
        }
        state.enemyHpMap.put(oldPrimaryId, 0.0f);
    }

    public static void onDeadExtraDiscarded(BattleState state, class_3218 world, class_1297 entity) {
        if (state == null || world == null || entity == null || state.isVirtualEnemy || state.isPvp) {
            return;
        }
        if (entity instanceof class_1309) {
            class_1309 deadLe = (class_1309)entity;
            class_3222 player = world.method_8503().method_3760().method_14602(state.playerUuid);
            if (player != null) {
                try {
                    BestiaryData.get(world.method_8503()).recordDefeat(state.playerUuid, deadLe.method_5864());
                } catch (Throwable ignored) {}
            }
            dropBattleLoot(deadLe, world, true, player);
            if (deadLe instanceof class_1308) {
                BattleManager.unfreezeMob((class_1308)deadLe);
            }
        }
    }

    public static float getPartyDefense(class_3222 player, BattleState state, UUID slot, BattleData.PlayerProgress progD) {
        if (slot == null) return 0.0f;
        if (slot.equals(BattleState.PARTY_MEMBER_UUID) && progD != null && progD.partyMember != PartyMember.NONE) {
            int baseStat = progD.partyStats[progD.partyMember.ordinal()].defense;
            int weaponBonus = 0;
            if (progD.partyEquippedWeapon != null && !progD.partyEquippedWeapon.method_7960()) {
                WeaponStats ws = WeaponStats.fromStack(progD.partyEquippedWeapon);
                if (ws != null) weaponBonus += ws.netStatBonus(1);
            }
            if (progD.equippedArmor != null) {
                for (class_1799 ap : progD.equippedArmor) {
                    if (ap == null || ap.method_7960()) continue;
                    ArmorStats as = ArmorStats.fromStack(ap);
                    if (as != null && as.bonusStat == 1) weaponBonus += as.bonusAmt;
                }
            }
            return (float)(baseStat + weaponBonus);
        }
        RecruitData.RecruitState rs = resolveRecruitState(player, state, slot);
        if (rs != null) {
            CharStats eff = getRecruitEffectiveStats(rs);
            return eff != null ? eff.defenseReduction() : (float)rs.stats.defense;
        }
        return 0.0f;
    }

    public static float getPartyDodge(class_3222 player, BattleState state, UUID slot, BattleData.PlayerProgress progD) {
        if (slot == null) return 0.0f;
        if (slot.equals(BattleState.PARTY_MEMBER_UUID) && progD != null && progD.partyMember != PartyMember.NONE) {
            int baseStat = progD.partyStats[progD.partyMember.ordinal()].magic;
            return Math.min((float)baseStat * 0.01f, 0.2f);
        }
        RecruitData.RecruitState rs = resolveRecruitState(player, state, slot);
        if (rs != null) {
            CharStats eff = getRecruitEffectiveStats(rs);
            return eff != null ? eff.dodgeChance() : 0.0f;
        }
        return 0.0f;
    }

    public static float applyPartyArmorAndDef(class_3222 player, BattleState state, UUID slot, BattleData.PlayerProgress progD, float baseDmg, float shieldReduction, float partyDef) {
        float armorFactor = getPartyArmorFactor(player, state, slot, progD);
        float afterShieldAndArmor = baseDmg * (1.0f - shieldReduction) * armorFactor;
        return Math.max(0.0f, afterShieldAndArmor - partyDef);
    }

    public static float applyDirectPartyDamage(class_3222 player, BattleState state, UUID slot, float rdmg) {
        if (player == null || state == null || slot == null) return Math.max(0.0f, rdmg - 5.0f);
        BattleData data = BattleData.get(player.method_5682());
        BattleData.PlayerProgress progD = data != null ? data.getOrCreate(state.playerUuid) : null;
        float def = getPartyDefense(player, state, slot, progD);
        float armorFactor = getPartyArmorFactor(player, state, slot, progD);
        float effectiveDef = def > 0.0f ? def : 5.0f;
        return Math.max(0.0f, rdmg * armorFactor - effectiveDef);
    }

    public static void onVirtualBattleWon(MinecraftServer server, UUID playerUuid, int expGained) {
        if (server == null || playerUuid == null) return;
        class_3222 player = server.method_3760().method_14602(playerUuid);
        if (player != null && player.method_37908() instanceof class_3218) {
            class_3218 world = (class_3218)player.method_37908();
            int vxp = Math.max(5, Math.min(100, Math.round((float)expGained * 0.15f)));
            class_1303.method_31493(world, player.method_19538(), vxp);
        }
    }

    public static void onLiveCombatOutcome(class_1309 entity, class_3222 player, String outcome) {
        if (entity == null || player == null) return;
        if ("mercy".equals(outcome) || "win".equals(outcome)) {
            class_1937 w = entity.method_37908();
            if (w instanceof class_3218) {
                onEnemyDefeated(entity, (class_3218)w, player);
            }
        }
    }

    /**
     * Dual-Mode Soul Extraction:
     * Extracts active soul into a physical Soul item in the player's inventory.
     */
    public static boolean extractSoulToItem(class_3222 p) {
        if (p == null) return false;
        MinecraftServer server = p.method_5682();
        if (server == null || SoulWalk.isOut(p.method_5667())) return false;
        BattleData data = BattleData.get(server);
        BattleData.PlayerProgress prog = data.getOrCreate(p.method_5667());
        if (prog.soulRemoved || prog.soulType == null || prog.playerRace == CharacterRace.MONSTER) return false;
        if (BattleManager.isInBattle(p.method_5667())) return false;
        SoulItem soulItem = TurnBattleMod.SOUL_ITEMS.get(prog.soulType);
        if (p.method_31548().method_7376() == -1 || soulItem == null) {
            if (soulItem != null) {
                p.method_7353((class_2561)class_2561.method_43470("No room to hold your SOUL.").method_27692(class_124.field_1080), true);
            }
            return false;
        }
        prog.soulRemoved = true;
        data.method_80();
        class_1799 soulStack = new class_1799(soulItem);
        String vname = prog.vesselName != null && !prog.vesselName.isEmpty() ? prog.vesselName : p.method_5477().getString();
        soulStack.method_57379(class_9334.field_49631, class_2561.method_43470(vname + "'s SOUL").method_27692(class_124.field_1068));
        p.method_31548().method_7394(soulStack);
        Net121.sendToClient(p, TurnBattleMod.SOUL_REMOVED, PacketByteBufs.empty());
        return true;
    }

    /**
     * Sends current soul information packet to the client.
     */
    public static void sendSoulInfo(class_3222 player, BattleData.PlayerProgress prog) {
        if (player == null || prog == null) return;
        class_2540 buf = PacketByteBufs.create();
        buf.method_52964(prog.soulType != null);
        if (prog.soulType != null) {
            buf.method_53002(prog.soulType.ordinal());
        }
        buf.method_52964(prog.freeRollUsed);
        Net121.sendToClient(player, TurnBattleMod.SOUL_INFO, buf);
    }

    /**
     * True Soul Swapping:
     * When absorbing a soul, preserves and refunds the previously active soul as an item
     * rather than destroying it.
     */
    public static boolean handleSoulItemUse(class_3222 sp, SoulItem soulItem, class_1799 stack) {
        if (sp == null || soulItem == null || stack == null) return false;
        MinecraftServer server = sp.method_5682();
        if (server == null) return false;
        BattleData data = BattleData.get(server);
        BattleData.PlayerProgress prog = data.getOrCreate(sp.method_5667());
        if (prog.playerRace == CharacterRace.MONSTER) {
            return false; // let default monster absorption run
        }
        SoulType targetSoulType = soulItem.getSoulType();
        if (targetSoulType == SoulType.MONSTER) {
            sp.method_7353((class_2561)class_2561.method_43470("This soul belongs to a Monster. You cannot absorb it."), true);
            return true;
        }
        boolean isOwnSoul = stack.method_57826(class_9334.field_49631)
                && prog.vesselName != null
                && stack.method_7964().getString().equals(prog.vesselName + "'s SOUL")
                && targetSoulType == prog.soulType;

        sp.method_5783(TurnBattleSounds.SND_GRAB, 1.0f, 1.0f);

        if (isOwnSoul) {
            prog.soulRemoved = false;
        } else {
            SoulType oldSoul = prog.soulType;
            boolean hadActiveSoul = !prog.soulRemoved && oldSoul != null && oldSoul != targetSoulType;

            prog.soulType = targetSoulType;
            prog.soulRemoved = false;
            Net121.sendToClient(sp, TurnBattleMod.ESTABLISHING_CONNECTION, PacketByteBufs.empty());

            // True Soul Swapping: Refund/preserve previous active soul as an item
            if (hadActiveSoul) {
                SoulItem oldItem = TurnBattleMod.SOUL_ITEMS.get(oldSoul);
                if (oldItem != null) {
                    class_1799 oldStack = new class_1799(oldItem);
                    String vname = prog.vesselName != null && !prog.vesselName.isEmpty() ? prog.vesselName : sp.method_5477().getString();
                    oldStack.method_57379(class_9334.field_49631, class_2561.method_43470(vname + "'s SOUL").method_27692(class_124.field_1068));
                    if (!sp.method_31548().method_7394(oldStack)) {
                        sp.method_7328(oldStack, false);
                    }
                }
            }
        }

        data.method_80();
        stack.method_7934(1);
        sp.field_7498.method_7623();
        Net121.sendToClient(sp, TurnBattleMod.SOUL_RESTORED, PacketByteBufs.empty());
        sendSoulInfo(sp, prog);
        TurnBattleMod.sendPartyInfo(sp);
        return true;
    }

    public static final Set<UUID> SOUL_RIPPING_PLAYERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static boolean isSoulRipping(UUID who) {
        if (who == null) return false;
        return SOUL_RIPPING_PLAYERS.contains(who);
    }

    public static boolean isSoulRipping(class_1297 entity) {
        if (entity == null) return false;
        return isSoulRipping(entity.method_5667());
    }
}
