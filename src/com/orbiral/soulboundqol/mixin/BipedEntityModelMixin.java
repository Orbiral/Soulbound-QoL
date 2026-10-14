package com.orbiral.soulboundqol.mixin;

import com.orbiral.soulboundqol.QoLHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1309;
import net.minecraft.class_572;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = class_572.class, remap = false)
public abstract class BipedEntityModelMixin {

    @Shadow public boolean field_3400; // sneaking

    @Inject(method = "method_17087(Lnet/minecraft/class_1309;FFFFF)V", at = @At("HEAD"), remap = false)
    private void qol$preventSneakPoseDuringSoulRip(class_1309 entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (entity != null && QoLHelper.isSoulRipping(entity.method_5667())) {
            this.field_3400 = false;
        }
    }
}
