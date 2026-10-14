package com.orbiral.soulboundqol.mixin;

import com.orbiral.soulboundqol.QoLHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_4050;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(value = class_1297.class, remap = false)
public abstract class EntityPoseMixin {

    @Inject(method = "method_18276", at = @At("HEAD"), cancellable = true, remap = false)
    private void qol$noSneakingPoseDuringSoulRip(CallbackInfoReturnable<Boolean> cir) {
        class_1297 self = (class_1297)(Object)this;
        if (QoLHelper.isSoulRipping(self.method_5667())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "method_18376", at = @At("RETURN"), cancellable = true, remap = false)
    private void qol$standingPoseDuringSoulRip(CallbackInfoReturnable<class_4050> cir) {
        if (cir.getReturnValue() == class_4050.field_18081) {
            class_1297 self = (class_1297)(Object)this;
            if (QoLHelper.isSoulRipping(self.method_5667())) {
                cir.setReturnValue(class_4050.field_18076);
            }
        }
    }
}
