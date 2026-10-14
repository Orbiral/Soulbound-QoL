package com.orbiral.soulboundqol.mixin;

import com.pixelcite.turnbattle.BattleData;
import com.pixelcite.turnbattle.Net121;
import com.pixelcite.turnbattle.SoulWalk;
import com.pixelcite.turnbattle.TurnBattleMod;
import com.orbiral.soulboundqol.QoLHelper;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoulWalk.class)
public abstract class SoulWalkMixin {

    /**
     * Dual-Mode Soul Extraction:
     * When sneaking (holding Shift while pressing Remove Soul), extracts active soul into an inventory item.
     * When not sneaking, standard Soul Walk flight mode runs.
     */
    @Inject(method = "tryLeave", at = @At("HEAD"), cancellable = true)
    private static void turnbattle$onTryLeave(class_3222 p, CallbackInfoReturnable<Boolean> cir) {
        if (p != null && (p.method_5715() || p.method_5869())) {
            cir.setReturnValue(QoLHelper.extractSoulToItem(p));
        }
    }

    /**
     * Prevents upstream from wiping soul items from player inventory upon joining.
     */
    @Redirect(
        method = "onJoin",
        at = @At(value = "FIELD", target = "Lcom/pixelcite/turnbattle/BattleData$PlayerProgress;soulRemoved:Z", opcode = 180)
    )
    private static boolean turnbattle$preventSoulItemDeletionOnJoin(BattleData.PlayerProgress prog) {
        // Return false so upstream skips the destructive inventory wipe loop
        return false;
    }

    @Redirect(
        method = "onJoin",
        at = @At(value = "FIELD", target = "Lcom/pixelcite/turnbattle/BattleData$PlayerProgress;soulRemoved:Z", opcode = 181),
        require = 0
    )
    private static void turnbattle$preventSoulRemovedReset(BattleData.PlayerProgress prog, boolean value) {
        // No-op: keep prog.soulRemoved as true
    }

    @Redirect(
        method = "onJoin",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1661;method_5447(ILnet/minecraft/class_1799;)V"),
        require = 0
    )
    private static void turnbattle$preventSetStackEmpty(net.minecraft.class_1661 inv, int slot, net.minecraft.class_1799 stack) {
        // No-op: never delete soul items
    }

    /**
     * If the player's soul is currently extracted, synchronizes that state to the client upon join.
     */
    @Inject(method = "onJoin", at = @At("RETURN"))
    private static void turnbattle$onJoinSyncSoulRemoved(class_3222 p, CallbackInfo ci) {
        if (p == null) {
            return;
        }
        MinecraftServer server = p.method_5682();
        if (server == null) {
            return;
        }
        BattleData data = BattleData.get(server);
        BattleData.PlayerProgress prog = data.getOrCreate(p.method_5667());
        if (prog != null && prog.soulRemoved) {
            Net121.sendToClient(p, TurnBattleMod.SOUL_REMOVED, PacketByteBufs.empty());
        }
    }
}
