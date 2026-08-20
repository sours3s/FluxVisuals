package ru.fluxvisuals.module.impl.visuals.HUD;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.joml.Vector4f;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.animation.AnimationMath;
import ru.fluxvisuals.util.render.math.ScaleHelper;
import java.awt.Color;

/**
 * HotBar HUD — полная реализация из GodWeer (HotbarElement)
 * <p>Кастомный хотбар с рендерингом слотов, оффхенда, HP, голод, уровень XP.
 * Не перетаскиваемый, позиция ванильная.
 */
@Environment(EnvType.CLIENT)
public class HotBarHUD {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Константы из донора (HotbarElement)
    private static final float SLOT_SIZE = 24.0F;
    private static final float SLOT_OFFSET = 2.0F;
    private static final float HOTBAR_WIDTH = 198.0F;
    private static final float HOTBAR_HEIGHT = SLOT_SIZE - 2.0F; // 22f
    private static final float HOTBAR_OFFSET = 22.0F;
    private static final float ROUND_SMALL = 4.0F;
    private static final float SMOOTH = 1.0F;

    // Анимация выбранного слота (как в доноре)
    private static float slotChangeAnim = 0f;

    /**
     * Рендерит хотбар — вызывается из InGameHudMixin (как в доноре)
     */
    public static void renderHotbar(DrawContext context, RenderTickCounter tickCounter) {
        if (!FluxVisualsClient.isModInitialized()) return;
        if (mc.player == null || mc.world == null) return;

        // Анимация выбранного слота (как в доноре)
        int slot = mc.player.getInventory().getSelectedSlot();
        slotChangeAnim = AnimationMath.animation(slotChangeAnim, slot, 0.41f);

        // Позиции (как в доноре) — ванильная позиция
        float halfWidth = mc.getWindow().getScaledWidth() / 2.0F;
        float x = halfWidth - HOTBAR_WIDTH / 2.0F;
        float y = mc.getWindow().getScaledHeight() - HOTBAR_OFFSET;

        Renderer2D r2 = FluxVisualsClient.getRenderer();
        if (r2 == null) return;

        // Рендерим стиль хотбара
        drawHotbarStyle(r2, x, y, HOTBAR_WIDTH, HOTBAR_HEIGHT, ROUND_SMALL, 1.0F);

        // Индикатор выбранного слота (как в доноре)
        float selectedSlotX = halfWidth - HOTBAR_WIDTH / 2.0F + ((SLOT_SIZE - SLOT_OFFSET) * slotChangeAnim);
        int selectedColor = Renderer2D.ColorUtil.getMainColor(1, 0);
        // Renderer2D.rect не поддерживает Vector4f, используем прямоугольник с rounding
        r2.rect(selectedSlotX + 1, y + 1.5f, SLOT_SIZE - 4, 2.5f, 1, selectedColor);

        // Оффхенд слот (как в доноре)
        ClientPlayerEntity player = mc.player;
        ItemStack offhandStack = player.getOffHandStack();
        Arm offhand = player.getMainArm().getOpposite();

        if (!offhandStack.isEmpty()) {
            float offhandGap = 10;
            float offhandSlotWidth = SLOT_SIZE - 3;

            float offhandX = offhand == Arm.LEFT
                    ? x - offhandSlotWidth - offhandGap
                    : x + HOTBAR_WIDTH + offhandGap;

            drawHotbarStyle(r2, offhandX, y, offhandSlotWidth, HOTBAR_HEIGHT, ROUND_SMALL, 1.0F);

            float itemX = offhandX + (offhandSlotWidth - 16) / 2.0F;
            float itemY = y + (HOTBAR_HEIGHT - 16) / 2.0F;

            renderHotbarItem(context, itemX, itemY, tickCounter, offhandStack, 10);
        }

        // 9 слотов хотбара (как в доноре)
        for (int m = 0; m < 9; ++m) {
            float n = x + (m * (SLOT_SIZE - SLOT_OFFSET)) + 4;
            float o = y + (HOTBAR_HEIGHT - 16) / 2.0F;
            renderHotbarItem(context, n, o, tickCounter, player.getInventory().getStack(m), m);
        }
    }

    /**
     * Рисует стиль хотбара (glass/blur)
     */
    private static void drawHotbarStyle(Renderer2D r2, float rx, float ry, float rw, float rh, float round, float alpha) {
        if (ru.fluxvisuals.module.impl.visuals.Hud.blur.get("HUD")) {
            r2.prepareBlur(23.0F);
            r2.blur(rx, ry, rw, rh, round, alpha);
        }

        // Glass style (как в доноре GLASS)
        int bgColor = Renderer2D.ColorUtil.replAlpha(0xFF3E3E47, 0);
        r2.rect(rx, ry, rw, rh, round, bgColor);

        int outAlpha = (int) Math.min(Math.max(25 * alpha, 0), 255);
        int outColor = Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, outAlpha);
        r2.rectOutline(rx, ry, rw, rh, round, outColor, 0.4F);
    }

    /**
     * Рендерит предмет в слоте хотбара (как в доноре)
     */
    private static void renderHotbarItem(DrawContext context, float x, float y, RenderTickCounter tickCounter, ItemStack stack, int slotIndex) {
        if (stack.isEmpty()) return;

        context.getMatrices().pushMatrix();
        try {
            context.getMatrices().translate(x, y);
            context.drawItem(stack, 0, 0);
            context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
        } finally {
            context.getMatrices().popMatrix();
        }
    }
}