package com.orbiral.soulboundqol.mixin;

import com.pixelcite.turnbattle.SoulItem;
import com.orbiral.soulboundqol.QoLHelper;
import net.minecraft.class_1268;
import net.minecraft.class_1271;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoulItem.class)
public abstract class SoulItemMixin {

    @Inject(method = "method_7836", at = @At("HEAD"), cancellable = true)
    private void turnbattle$onUseSoul(class_1937 world, class_1657 user, class_1268 hand, CallbackInfoReturnable<class_1271<class_1799>> cir) {
        if (!world.method_8608() && user instanceof class_3222 sp) {
            class_1799 stack = sp.method_5998(hand);
            if (QoLHelper.handleSoulItemUse(sp, (SoulItem)(Object)this, stack)) {
                cir.setReturnValue(class_1271.method_29237(class_1799.field_8037, true));
            }
        }
    }
}
