package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1309;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_490;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class QoLFallHelper {

    public static float blockPx(int sh) {
        return (float)sh * 0.3f / 1.8f;
    }

    public static String getSlotCompanionName(int slotCode) {
        if (slotCode >= 1000) {
            int ci = slotCode - 1000;
            String[] customs = TurnBattleClientMod.customRecruitNames;
            if (customs != null && ci >= 0 && ci < customs.length) {
                return customs[ci];
            }
        } else if (slotCode >= 0 && slotCode < Recruit.values().length) {
            return Recruit.values()[slotCode].name();
        }
        return null;
    }

    public static List<String> getTransformingCompanions() {
        ArrayList<String> result = new ArrayList<>();

        // Slot 1 (clientActiveRecruit)
        String name1 = getSlotCompanionName(TurnBattleClientMod.clientActiveRecruit);
        if (name1 != null && !name1.isBlank() && QoLSkinHelper.hasDarkSkin(name1)) {
            result.add(name1);
        }

        // Slot 2 (clientSecondRecruit)
        String name2 = getSlotCompanionName(TurnBattleClientMod.clientSecondRecruit);
        if (name2 != null && !name2.isBlank() && QoLSkinHelper.hasDarkSkin(name2)) {
            if (!result.contains(name2)) {
                result.add(name2);
            }
        }

        return result;
    }

    public static float getFallerSpacing(int sw, int sh, int count) {
        float b = blockPx(sh);
        float ideal = b * 1.25f;
        if (count <= 0) return ideal;
        float maxSpan = (float)sw * 0.75f;
        float factor = (float)((count + 1) / 2) * 2.3f;
        if (factor * ideal > maxSpan) {
            return maxSpan / factor;
        }
        return ideal;
    }

    public static float getFallerXOffset(int index, float spacing) {
        int side = index % 2 == 0 ? -1 : 1;
        int pairIndex = index / 2;
        return (float)side * spacing * (float)(pairIndex + 1);
    }

    public static float getFallerYOffset(int index, float b) {
        int pairIndex = index / 2;
        return (float)pairIndex * b * 0.18f;
    }

    public static void drawCompanions(class_332 ctx, class_310 mc, float cx, float y, int sh, float t) {
        if (mc.field_1687 == null) return;
        List<String> comps = getTransformingCompanions();
        int n = comps.size();
        if (n == 0) return;
        int sw = ctx.method_51421();
        float spacing = getFallerSpacing(sw, sh, n);
        float b = blockPx(sh);

        for (int i = 0; i < n; ++i) {
            float xOff = getFallerXOffset(i, spacing);
            float yOff = getFallerYOffset(i, b);
            drawCompanionFaller(ctx, mc, comps.get(i), cx + xOff, y + yOff, sh, t, i);
        }
    }

    public static void drawCompanionFaller(class_332 ctx, class_310 mc, String name, float x, float y, int sh, float t, int index) {
        BattleEntityRenderer.RecruitFakePlayer e;
        try {
            e = BattleEntityRenderer.recruitFake(name);
        } catch (Throwable ex) {
            return;
        }
        if (e == null) return;

        e.field_6220 = 180.0f;
        e.field_6283 = 180.0f;
        e.field_6259 = 180.0f;
        e.field_6241 = 180.0f;
        e.method_36456(180.0f);
        e.field_5982 = 180.0f;
        e.method_36457(0.0f);
        e.field_6004 = 0.0f;
        e.field_6229 = 0.0f;
        e.field_6251 = 0.0f;
        e.field_6012 = (int)(t * 20.0f);

        CloneAnimator.Pose p = new CloneAnimator.Pose();
        p.armW = 1.0f;
        p.headW = 1.0f;
        p.legW = 1.0f;
        float phase = (float)(index + 1) * 2.1f;
        float flap = 0.07f * (float)Math.sin((t + phase) * 15.0f);
        float flap2 = 0.07f * (float)Math.sin((t + phase) * 13.0f + 1.3f);
        p.rArmP = -2.85f + flap;
        p.rArmR = 0.32f + flap2;
        p.lArmP = -2.85f + flap2;
        p.lArmR = -0.32f - flap;
        p.rLegP = 0.28f + 0.06f * (float)Math.sin((t + phase) * 6.0f);
        p.lLegP = -0.2f + 0.06f * (float)Math.sin((t + phase) * 6.0f + 2.0f);
        p.headP = 0.3f;
        float sway = (float)Math.sin((t + phase) * 1.9f);
        float size = (float)sh * 0.3f / 1.8f;
        float bob = (float)Math.sin((t + phase) * 2.6f) * (float)sh * 0.008f;
        Quaternionf q = new Quaternionf().rotateZ((float)Math.PI).rotateY((float)Math.toRadians(sway * 7.0f)).rotateX((float)Math.toRadians(-3.0f * sway));
        Vector3f offset = new Vector3f(0.0f, 0.9f, 0.0f);

        CloneAnimator.beginModelPose((class_1309)e, p);
        try {
            class_490.method_48472(ctx, x, y + bob, size, offset, q, null, (class_1309)e);
        } catch (Exception ignored) {
        } finally {
            CloneAnimator.endModelPose();
            ctx.method_51452();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}
