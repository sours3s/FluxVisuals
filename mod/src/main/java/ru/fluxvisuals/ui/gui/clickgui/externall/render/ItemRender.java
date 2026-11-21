package ru.fluxvisuals.ui.gui.clickgui.externall.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix3x2fStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemRender {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Map<String, CachedSprite> SPRITE_CACHE = new ConcurrentHashMap<>();
    private static final int FORCED_GUI_SCALE = 2;
    private static final Random RANDOM = Random.create();

    public static void flushGui() {
        if (mc != null && mc.gameRenderer instanceof IGameRenderer gr) {
            gr.flux$flushGui();
        }
    }

    private static int getCurrentGuiScale() {
        int scale = mc.options.getGuiScale().getValue();
        if (scale == 0) {
            scale = mc.getWindow().calculateScaleFactor(0, mc.forcesUnicodeFont());
        }
        return scale;
    }

    private static float getScaleCompensation() {
        return (float) FORCED_GUI_SCALE / (float) getCurrentGuiScale();
    }

    public static boolean isBlockItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    public static boolean isPotionItem(ItemStack stack) {
        return stack.getItem() == Items.POTION ||
                stack.getItem() == Items.SPLASH_POTION ||
                stack.getItem() == Items.LINGERING_POTION ||
                stack.getItem() == Items.TIPPED_ARROW;
    }

    public static boolean hasGlint(ItemStack stack) {
        return stack.hasGlint();
    }

    public static boolean needsContextRender(ItemStack stack) {
        return isBlockItem(stack) || isPotionItem(stack) || hasGlint(stack);
    }

    public static void drawItem(ItemStack stack, float x, float y, float scale, float alpha) {
        if (stack.isEmpty() || alpha <= 0.01f) return;

        int tintColor = 0xFFFFFFFF;
        if (isPotionItem(stack)) {
            net.minecraft.component.type.PotionContentsComponent contents = stack.get(net.minecraft.component.DataComponentTypes.POTION_CONTENTS);
            if (contents != null && contents.getColor() != -1) {
                tintColor = 0xFF000000 | contents.getColor();
            }
        }
        drawItem(stack, x, y, scale, alpha, tintColor);
    }

    public static void drawItem(ItemStack stack, float x, float y, float scale, float alpha, int tintColor) {
        if (stack.isEmpty() || alpha <= 0.01f) return;

        Sprite sprite = getSpriteForStack(stack);
        if (sprite != null) {
            int color = applyAlpha(tintColor, alpha);
            float size = 16 * scale;
            Render2D.drawSprite(sprite, x, y, size, size, color, true);
        }
    }

    public static void drawBlockItem(DrawContext context, ItemStack stack, float x, float y, float scale, float alpha) {
        drawItemWithContext(context, stack, x, y, scale, alpha);
    }

    public static void drawItemWithContext(DrawContext context, ItemStack stack, float x, float y, float scale, float alpha) {
        float size = 16 * scale;
        float centerX = x + size / 2f;
        float centerY = y + size / 2f;
        drawItemCenteredWithContext(context, stack, centerX, centerY, size, alpha);
    }

    public static void drawItemCentered(ItemStack stack, float centerX, float centerY, float scale, float alpha) {
        float size = 16 * scale;
        float x = centerX - size / 2f;
        float y = centerY - size / 2f;
        drawItem(stack, x, y, scale, alpha);
    }

    public static void drawItemCenteredWithContext(DrawContext context, ItemStack stack, float centerX, float centerY, float size, float alpha) {
        if (stack.isEmpty() || alpha <= 0.01f || context == null) return;

        float targetScale = 2F;
        float currentScale = (float) mc.getWindow().getScaleFactor();
        float scaleFix = targetScale / currentScale;

        float finalScale = (size / 16F) * scaleFix;

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX * scaleFix, centerY * scaleFix);
        matrices.scale(finalScale, finalScale);
        context.drawItem(stack, -8, -8);
        matrices.popMatrix();
    }

    public static void drawItemWithOverlay(DrawContext context, ItemStack stack, float centerX, float centerY, float size, float alpha) {
        if (stack.isEmpty() || alpha <= 0.01f || context == null) return;

        float targetScale = 2F;
        float currentScale = (float) mc.getWindow().getScaleFactor();
        float scaleFix = targetScale / currentScale;

        float finalScale = (size / 16F) * scaleFix;

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX * scaleFix, centerY * scaleFix);
        matrices.scale(finalScale, finalScale);
        context.drawItem(stack, -8, -8);
        context.drawStackOverlay(mc.textRenderer, stack, -8, -8);
        matrices.popMatrix();
    }

    public static void drawItemCenteredNoOverlay(DrawContext context, ItemStack stack, float centerX, float centerY, float size, float alpha) {
        if (stack.isEmpty() || alpha <= 0.01f || context == null) return;

        float targetScale = 2F;
        float currentScale = (float) mc.getWindow().getScaleFactor();
        float scaleFix = targetScale / currentScale;

        float finalScale = (size / 16F) * scaleFix;

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX * scaleFix, centerY * scaleFix);
        matrices.scale(finalScale, finalScale);
        context.drawItem(stack, -8, -8);
        matrices.popMatrix();
    }

    public static void drawItemWithCount(DrawContext context, ItemStack stack, float centerX, float centerY, float size, float alpha, int count) {
        if (stack.isEmpty() || alpha <= 0.01f || context == null) return;

        float targetScale = 2F;
        float currentScale = (float) mc.getWindow().getScaleFactor();
        float scaleFix = targetScale / currentScale;

        float finalScale = (size / 16F) * scaleFix;

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX * scaleFix, centerY * scaleFix);
        matrices.scale(finalScale, finalScale);
        context.drawItem(stack, -8, -8);
        if (count > 1) {
            context.drawStackOverlay(mc.textRenderer, stack, -8, -8, String.valueOf(count));
        }
        matrices.popMatrix();
    }

    public static Sprite getSpriteForStack(ItemStack stack) {
        String cacheKey = getCacheKey(stack);

        CachedSprite cached = SPRITE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached.sprite;
        }

        try {
            ItemRenderState state = new ItemRenderState();
            mc.getItemModelManager().clearAndUpdate(state, stack, ItemDisplayContext.GUI, mc.world, null, 0);

            Sprite sprite = state.getParticleSprite(RANDOM);
            if (sprite != null) {
                SPRITE_CACHE.put(cacheKey, new CachedSprite(sprite));
                return sprite;
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static String getCacheKey(ItemStack stack) {
        return stack.getItem().toString() + "_" + stack.getComponents().hashCode();
    }

    private static int applyAlpha(int color, float alpha) {
        int baseA = (color >> 24) & 0xFF;
        if (baseA == 0) baseA = 255;
        int a = (int) (baseA * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static void clearCache() {
        SPRITE_CACHE.clear();
    }

    private record CachedSprite(Sprite sprite) {}
}
