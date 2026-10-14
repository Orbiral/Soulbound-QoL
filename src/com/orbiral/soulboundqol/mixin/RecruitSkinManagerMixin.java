package com.orbiral.soulboundqol.mixin;

import com.pixelcite.turnbattle.QoLSkinHelper;
import com.pixelcite.turnbattle.RecruitSkinManager;
import net.minecraft.class_2960;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RecruitSkinManager.class, remap = false)
public class RecruitSkinManagerMixin {

    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetSkin(String recruitName, CallbackInfoReturnable<class_2960> cir) {
        class_2960 skin = QoLSkinHelper.getCustomSkin(recruitName);
        if (skin != null) {
            cir.setReturnValue(skin);
        }
    }
}
