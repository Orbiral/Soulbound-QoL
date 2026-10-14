package com.orbiral.soulboundqol.mixin;

import com.orbiral.soulboundqol.QoLHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(value = class_746.class, remap = false)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "method_18276", at = @At("HEAD"), cancellable = true, remap = false)
    private void qol$noClientSneakingPoseDuringSoulRip(CallbackInfoReturnable<Boolean> cir) {
        class_746 self = (class_746)(Object)this;
        if (QoLHelper.isSoulRipping(self.method_5667())) {
            cir.setReturnValue(false);
        }
    }
}
