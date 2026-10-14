package com.orbiral.soulboundqol;

import com.pixelcite.turnbattle.BattleData;
import com.pixelcite.turnbattle.CharStats;
import com.pixelcite.turnbattle.MobLevelManager;
import com.pixelcite.turnbattle.Net121;
import com.pixelcite.turnbattle.PartyMember;
import com.pixelcite.turnbattle.Recruit;
import com.pixelcite.turnbattle.RecruitData;
import com.pixelcite.turnbattle.TurnBattleMod;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_1263;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoulboundQoL implements ModInitializer {
    public static final String MOD_ID = "soulbound-qol";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[Soulbound QoL] Initializing Soulbound QoL addon...");

        ServerLifecycleEvents.SERVER_STARTING.register(serverInstance -> {
            try {
                // Unregister upstream default STAT_RESET handler and register the fixed handler
                ServerPlayNetworking.unregisterGlobalReceiver(TurnBattleMod.STAT_RESET);
                Net121.registerServerReceiver(TurnBattleMod.STAT_RESET, (server, player, handler, buf, sender) -> {
                int rawTargetType = buf.readInt();
                server.execute(() -> {
                    if (rawTargetType == 0 && BattleData.get((MinecraftServer)server).getOrCreate((UUID)player.method_5667()).rogueRun) {
                        player.method_7353((class_2561)class_2561.method_43470((String)"[Stats] Your stats come from your cards during a Roguelite run - they can't be reset."), false);
                        return;
                    }
                    CharStats targetStats = null;
                    Runnable afterReset = null;
                    if (rawTargetType >= 1000) {
                        int ci = QoLHelper.resolveRecruitTargetIndex(rawTargetType);
                        RecruitData rd = RecruitData.get(server);
                        RecruitData.PlayerRecruits pr = rd.getOrCreate(player.method_5667());
                        if (ci >= 0 && ci < pr.customRecruits.size()) {
                            targetStats = pr.customRecruits.get(ci).state.stats;
                            afterReset = () -> {
                                rd.method_80();
                                TurnBattleMod.sendRecruitInfo(player);
                            };
                        }
                    } else if (rawTargetType >= 1) {
                        RecruitData rd = RecruitData.get(server);
                        RecruitData.PlayerRecruits pr = rd.getOrCreate(player.method_5667());
                        int seen = 0;
                        Recruit matched = null;
                        for (Recruit r : Recruit.values()) {
                            if (!pr.recruits[r.ordinal()].unlocked || ++seen != rawTargetType) continue;
                            matched = r;
                            break;
                        }
                        if (matched != null) {
                            targetStats = pr.recruits[matched.ordinal()].stats;
                            afterReset = () -> {
                                rd.method_80();
                                TurnBattleMod.sendRecruitInfo(player);
                            };
                        }
                    } else {
                        BattleData data = BattleData.get(server);
                        BattleData.PlayerProgress prog = data.getOrCreate(player.method_5667());
                        CharStats stats = rawTargetType == 0 ? prog.playerStats : (rawTargetType < PartyMember.values().length ? prog.partyStats[rawTargetType] : null);
                        if (stats != null) {
                            targetStats = stats;
                            afterReset = () -> {
                                data.method_80();
                                if (rawTargetType == 0) {
                                    MobLevelManager.applyPlayerStats(player, prog.level, prog.playerStats);
                                }
                                TurnBattleMod.sendPartyInfo(player);
                            };
                        }
                    }

                    if (targetStats == null) {
                        player.method_7353((class_2561)class_2561.method_43470((String)"[Stats] Couldn't find that target to reset (no netherite consumed)."), false);
                        return;
                    }
                    boolean hasIngot = player.method_31548().method_55753(stack -> stack.method_31574(class_1802.field_22020));
                    if (!hasIngot) {
                        player.method_7353((class_2561)class_2561.method_43470((String)"[Stats] You need 1 Netherite Ingot to reset stats."), false);
                        return;
                    }
                    player.method_31548().method_29280(stack -> stack.method_31574(class_1802.field_22020), 1, (class_1263)player.field_7498.method_29281());
                    targetStats.reset();
                    afterReset.run();
                    player.method_7353((class_2561)class_2561.method_43470((String)"[Stats] Stats reset! Points refunded."), false);
                });
            });
            LOGGER.info("[Soulbound QoL] Registered enhanced STAT_RESET receiver (custom recruits support active).");
            } catch (Throwable t) {
                LOGGER.error("[Soulbound QoL] Failed to register STAT_RESET override: ", t);
            }
        });

        LOGGER.info("[Soulbound QoL] All QoL fixes initialized successfully!");
    }
}
