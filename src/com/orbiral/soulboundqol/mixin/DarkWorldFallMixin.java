package com.orbiral.soulboundqol.mixin;

import com.pixelcite.turnbattle.DarkWorldFall;
import com.pixelcite.turnbattle.QoLFallHelper;
import net.minecraft.class_310;
import net.minecraft.class_332;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DarkWorldFall.class, remap = false)
public class DarkWorldFallMixin {

    @Inject(method = "drawFaller", at = @At("TAIL"), remap = false)
    private static void onDrawFaller(class_332 ctx, class_310 mc, float x, float y, int sh, float t, CallbackInfo ci) {
        QoLFallHelper.drawCompanions(ctx, mc, x, y, sh, t);
    }
}
