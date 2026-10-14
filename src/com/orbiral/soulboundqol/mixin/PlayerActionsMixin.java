package com.orbiral.soulboundqol.mixin;

import com.pixelcite.turnbattle.PlayerActions;
import com.pixelcite.turnbattle.SoulType;
import com.orbiral.soulboundqol.QoLHelper;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = PlayerActions.class, remap = false)
public abstract class PlayerActionsMixin {

    @Inject(method = "startLocalSoulRip", at = @At("HEAD"), remap = false)
    private static void turnbattle$onStartLocalSoulRip(SoulType soul, CallbackInfo ci) {
        class_310 mc = class_310.method_1551();
        if (mc != null && mc.field_1724 != null) {
            QoLHelper.SOUL_RIPPING_PLAYERS.add(mc.field_1724.method_5667());
        }
    }

    @Inject(method = "cancelLocalSoulRip", at = @At("HEAD"), remap = false)
    private static void turnbattle$onCancelLocalSoulRip(CallbackInfo ci) {
        class_310 mc = class_310.method_1551();
        if (mc != null && mc.field_1724 != null) {
            QoLHelper.SOUL_RIPPING_PLAYERS.remove(mc.field_1724.method_5667());
        }
    }

    @Inject(method = "clear", at = @At("HEAD"), remap = false)
    private static void turnbattle$onClear(UUID who, CallbackInfo ci) {
        if (who != null) {
            QoLHelper.SOUL_RIPPING_PLAYERS.remove(who);
        }
    }

    @Inject(method = "onPacket", at = @At("HEAD"), remap = false)
    private static void turnbattle$onPacket(int kind, UUID who, float yaw, double bx, double by, double bz, int soulOrd, CallbackInfo ci) {
        if (who == null) return;
        if (kind == 3) {
            QoLHelper.SOUL_RIPPING_PLAYERS.add(who);
        } else if (kind == 4) {
            QoLHelper.SOUL_RIPPING_PLAYERS.remove(who);
        }
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private static void turnbattle$onTick(class_310 mc, CallbackInfo ci) {
        if (mc.field_1687 == null) {
            QoLHelper.SOUL_RIPPING_PLAYERS.clear();
        } else if (mc.field_1724 != null) {
            UUID localId = mc.field_1724.method_5667();
            if (QoLHelper.SOUL_RIPPING_PLAYERS.contains(localId)) {
                if (!PlayerActions.cameraActive() && !PlayerActions.localLocked()) {
                    QoLHelper.SOUL_RIPPING_PLAYERS.remove(localId);
                }
            }
        }
    }
}
