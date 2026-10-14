package com.orbiral.soulboundqol.mixin;

import com.pixelcite.turnbattle.BattleData;
import com.pixelcite.turnbattle.BattleManager;
import com.pixelcite.turnbattle.BattleState;
import com.pixelcite.turnbattle.CharStats;
import com.pixelcite.turnbattle.MobLevelManager;
import com.pixelcite.turnbattle.RecruitData;
import com.orbiral.soulboundqol.QoLHelper;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BattleManager.class, remap = false)
public class BattleManagerMixin {

    @Inject(method = "getEnemyDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetEnemyDamage(class_1309 enemy, int playerLevel, CallbackInfoReturnable<Float> cir) {
        if (enemy == null) return;
        int mobLevel = MobLevelManager.getMobLevel(enemy);
        if (mobLevel > 0) {
            cir.setReturnValue(QoLHelper.getFixedEnemyDamage(enemy, playerLevel));
        }
    }

    @Inject(method = "getRecruitEffectiveStats", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetRecruitEffectiveStats(RecruitData.RecruitState rs, CallbackInfoReturnable<CharStats> cir) {
        cir.setReturnValue(QoLHelper.getRecruitEffectiveStats(rs));
    }

    @Inject(method = "endBattle(Lcom/pixelcite/turnbattle/BattleState;Lnet/minecraft/server/MinecraftServer;Ljava/lang/String;)V", at = @At("HEAD"), remap = false)
    private static void onEndBattle(BattleState state, MinecraftServer server, String outcome, CallbackInfo ci) {
        if (state == null || server == null) return;
        boolean restoreParty = !"win".equals(outcome) && !"mercy".equals(outcome);
        QoLHelper.savePartyMemberHpEndBattle(server, state, state.playerUuid, restoreParty);
    }

    @Inject(method = "dropBattleLoot", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onDropBattleLoot(class_1309 entity, class_3218 world, boolean killed, class_3222 killer, CallbackInfo ci) {
        QoLHelper.dropBattleLoot(entity, world, killed, killer);
        ci.cancel();
    }

    @Redirect(
        method = "applySoulHitDamage",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
        remap = false
    )
    private static Object onApplySoulHitHpPut(Map map, Object key, Object value, class_3222 player, float damage) {
        if (key instanceof UUID && value instanceof Float) {
            UUID slot = (UUID) key;
            if (slot.equals(BattleState.PARTY_MEMBER_UUID) || slot.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                BattleState state = BattleManager.activeBattles.get(player.method_5667());
                if (state != null && map == state.participantHp) {
                    float oldHp = state.participantHp.getOrDefault(slot, 0.0f);
                    float newHpVal = ((Float) value).floatValue();
                    float rawDmg = oldHp - newHpVal;
                    if (rawDmg > 0.0f) {
                        BattleData bd = BattleData.get(player.method_5682());
                        BattleData.PlayerProgress progD = bd != null ? bd.getOrCreate(state.playerUuid) : null;
                        float armorFactor = QoLHelper.getPartyArmorFactor(player, state, slot, progD);
                        float mitigatedDmg = rawDmg * armorFactor;
                        float finalHp = oldHp - mitigatedDmg;
                        if (finalHp <= 0.0f) {
                            float maxHp = state.participantMaxHp.getOrDefault(slot, 20.0f);
                            finalHp = -Math.round(maxHp / 2.0f);
                        }
                        value = Float.valueOf(finalHp);
                        state.targetParticipantHp.put(slot, Float.valueOf(finalHp));
                    }
                }
            }
        }
        return map.put(key, value);
    }

    @Inject(method = "promoteNextEnemy", at = @At("HEAD"), remap = false)
    private static void onPromoteNextEnemy(BattleState state, class_3218 world, CallbackInfo ci) {
        QoLHelper.onBeforePromote(state, world);
    }

    @Redirect(
        method = "promoteNextEnemy",
        at = @At(value = "INVOKE", target = "Lcom/pixelcite/turnbattle/BattleManager;discardEnemy(Lnet/minecraft/class_1297;)V"),
        remap = false
    )
    private static void onDiscardEnemyInPromote(class_1297 entity, BattleState state, class_3218 world) {
        QoLHelper.onDeadExtraDiscarded(state, world, entity);
        if (entity != null && !(entity instanceof net.minecraft.class_1657)) {
            entity.method_31472();
        }
    }

    @Redirect(
        method = "runDirectDamageTurn",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
        remap = false
    )
    private static Object onRunDirectHpPut(Map map, Object key, Object value, class_3222 triggeringPlayer, class_1309 enemy, BattleState state) {
        if (key instanceof UUID && value instanceof Float) {
            UUID slot = (UUID) key;
            if (slot.equals(BattleState.PARTY_MEMBER_UUID) || slot.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                if (state != null && (map == state.participantHp || map == state.targetParticipantHp)) {
                    float oldHp = state.participantHp.getOrDefault(slot, 0.0f);
                    float newHpVal = ((Float) value).floatValue();
                    float rawDmg = oldHp - newHpVal;
                    if (rawDmg > 0.0f) {
                        float armorFactor = QoLHelper.getPartyArmorFactor(triggeringPlayer, state, slot, null);
                        float mitigatedDmg = rawDmg * armorFactor;
                        float finalHp = oldHp - mitigatedDmg;
                        if (finalHp <= 0.0f) {
                            float maxHp = state.participantMaxHp.getOrDefault(slot, 20.0f);
                            finalHp = -Math.round(maxHp / 2.0f);
                        }
                        value = Float.valueOf(finalHp);
                    }
                }
            }
        }
        return map.put(key, value);
    }
}
