package ru.fluxvisuals.module.impl.visuals.HUD;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import ru.fluxvisuals.util.render.math.animation.AnimationMath;

/**
 * TargetHUD в стиле исходного FluxVisuals: панель 150×34 с именем, дистанцией,
 * полосой здоровья и полосой брони. Появляется только при цели под прицелом;
 * с открытым чатом показывает самого игрока.
 */
@Environment(EnvType.CLIENT)
public final class TargetHUD {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final float WIDTH = 150.0F;
    private static final float HEIGHT = 34.0F;
    private static final float BAR_WIDTH = 134.0F;
    private static final float BAR_HEIGHT = 5.0F;
    private static final int PADDING = 4;

    private static float visibility = 0.0F;

    private TargetHUD() {
    }

    public static void renderTargetHUD(DrawContext context) {
        if (mc.player == null || mc.world == null) {
            visibility = AnimationMath.animation(visibility, 0.0F, 0.05F);
            return;
        }

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        LivingEntity target = chatOpen ? mc.player : getCrosshairTarget();
        boolean shown = target != null && target.isAlive();

        visibility = AnimationMath.animation(visibility, shown ? 1.0F : 0.0F, 0.05F);
        if (visibility <= 0.01F || target == null) {
            return;
        }

        float alpha = Math.min(1.0F, visibility);
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        float x = PADDING;
        // Ватермарка занимает верхние 16 px — панель ставим сразу под ней.
        float y = PADDING + 20.0F;
        if (x + WIDTH > sw) {
            x = Math.max(1.0F, sw - WIDTH - PADDING);
        }
        if (y + HEIGHT > sh) {
            y = Math.max(1.0F, sh - HEIGHT - PADDING);
        }

        int left = (int) x;
        int top = (int) y;
        int right = (int) (x + WIDTH);
        int bottom = (int) (y + HEIGHT);

        context.fill(left, top, right, bottom,
              HudTheme.withAlpha(HudTheme.BG, (int) (HudTheme.BG_ALPHA * alpha)));
        int border = HudTheme.withAlpha(HudTheme.ACCENT, (int) (90 * alpha));
        context.fill(left, top, right, top + 1, border);
        context.fill(left, bottom - 1, right, bottom, border);
        context.fill(left, top, left + 1, bottom, border);
        context.fill(right - 1, top, right, bottom, border);

        String name = target.getDisplayName().getString();
        String distance = String.format("%.1fm", mc.player.distanceTo(target));
        context.drawTextWithShadow(mc.textRenderer, name, left + 8, top + 6,
              HudTheme.mulAlpha(HudTheme.TEXT, alpha));
        context.drawTextWithShadow(mc.textRenderer, distance,
              right - 8 - mc.textRenderer.getWidth(distance), top + 6,
              HudTheme.mulAlpha(HudTheme.TEXT_DIM, alpha));

        float maxHealth = Math.max(1.0F, target.getMaxHealth());
        float healthPercent = Math.max(0.0F, Math.min(1.0F, target.getHealth() / maxHealth));
        int hpColor = HudTheme.healthColor(healthPercent);
        int barX = left + 8;
        int barY = top + 20;
        int barRight = (int) (barX + BAR_WIDTH);
        int barBottom = (int) (barY + BAR_HEIGHT);
        int barBg = HudTheme.withAlpha(0xFF000000, (int) (0.35F * 255.0F * alpha));
        context.fill(barX, barY, barRight, barBottom, barBg);
        context.fill(barX, barY, (int) (barX + BAR_WIDTH * healthPercent), barBottom,
              HudTheme.withAlpha(hpColor, (int) (255 * alpha)));

        float armorPercent = Math.max(0.0F, Math.min(1.0F, target.getArmor() / 20.0F));
        int armorY = barBottom + 2;
        context.fill(barX, armorY, barRight, armorY + 2, barBg);
        context.fill(barX, armorY, (int) (barX + BAR_WIDTH * armorPercent), armorY + 2,
              HudTheme.withAlpha(HudTheme.ARMOR, (int) (255 * alpha)));
    }

    private static LivingEntity getCrosshairTarget() {
        HitResult hit = mc.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}
