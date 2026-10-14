package com.orbiral.soulboundqol.mixin;

import com.orbiral.soulboundqol.QoLHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1007;
import net.minecraft.class_243;
import net.minecraft.class_742;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(value = class_1007.class, remap = false)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "method_23206(Lnet/minecraft/class_742;F)Lnet/minecraft/class_243;", at = @At("HEAD"), cancellable = true, remap = false)
    private void qol$noSneakOffsetDuringSoulRip(class_742 player, float tickDelta, CallbackInfoReturnable<class_243> cir) {
        if (player != null && QoLHelper.isSoulRipping(player.method_5667())) {
            cir.setReturnValue(class_243.field_1353);
        }
    }
}
