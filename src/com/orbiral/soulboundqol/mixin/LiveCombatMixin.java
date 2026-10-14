package com.orbiral.soulboundqol.mixin;

import com.pixelcite.turnbattle.LiveCombat;
import com.orbiral.soulboundqol.QoLHelper;
import net.minecraft.class_1309;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LiveCombat.class, remap = false)
public class LiveCombatMixin {

    @Inject(method = "rewards", at = @At("HEAD"), remap = false)
    private static void onRewards(MinecraftServer server, class_1309 t, class_3222 by, float maxHp, String outcome, CallbackInfo ci) {
        QoLHelper.onLiveCombatOutcome(t, by, outcome);
    }
}
