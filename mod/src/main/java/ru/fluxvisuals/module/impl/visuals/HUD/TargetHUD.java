package ru.fluxvisuals.module.impl.visuals.HUD;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import org.joml.Vector4f;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.animation.AnimationMath;
import ru.fluxvisuals.util.render.math.ScaleHelper;
import ru.fluxvisuals.util.render.text.FontObject;
import ru.fluxvisuals.util.render.text.FontRegistry;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * TargetHUD — полная реализация из GodWeer (TargetHudElement)
 * <p>Показывает здоровье, никнейм, предметы цели под прицелом.
 * Видимость: в чате — последняя зафиксированная цель, в игре — цель под прицелом/наведением.
 */
@Environment(EnvType.CLIENT)
public class TargetHUD {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Константы из донора
    private static final float WIDTH = 110f;
    private static final float MAIN_HEIGHT = 28f;
    private static final float HEAD_SIZE = 20f;
    private static final float ITEM_BOX = 14.5f;
    private static final float SPACING = 1.5f;

    // Анимация появления/скрытия
    private static float animation = 0f;
    private static float scrollAnim = 0f;
    private static long scrollDelay = 0;
    private static int scrollDir = 1;

    // Состояние цели
    private static LivingEntity lastHoveredEntity = null;
    private static long lastHoverTime = 0;
    private static LivingEntity prevTarget = null;

    // Здоровье с анимацией (как в доноре)
    private static float healthAnim = 0f;
    private static float absorptionAnim = 0f;
    private static float healthFactor = 0f;

    // Частицы (упрощенно, без движка частиц)
    private static int particlesToSpawn = 0;

    // Шрифт
    private static FontObject font = FontRegistry.INTER_MEDIUM;

    /**
     * Рендерит TargetHUD — вызывается из Hud.onRender (как в доноре)
     */
    public static void renderTargetHUD(DrawContext context, int mouseX, int mouseY) {
        if (mc.player == null || mc.world == null) return;
        if (font == null) return;

        // Получаем цель под прицелом (как в доноре: TargetsUtility.getTarget() или mc.targetedEntity)
        LivingEntity target = getHoveredEntity();
        LivingEntity hoveredEntity = (mc.targetedEntity instanceof LivingEntity living) ? living : null;

        long now = System.currentTimeMillis();
        if (hoveredEntity != null) {
            lastHoverTime = now;
            lastHoveredEntity = hoveredEntity;
        }

        boolean isInChat = mc.currentScreen instanceof ChatScreen;
        boolean showOnHover = true; // настройка из донора
        boolean isHovering = showOnHover && lastHoveredEntity != null && (now - lastHoverTime < 2000);

        // Логика показа:
        // - В чате: показываем себя (mc.player)
        // - В игре: показываем цель под прицелом или при наведении
        boolean shown = isInChat || target != null || isHovering;

        // Обновляем prevTarget
        if (target != null) {
            prevTarget = target;
        } else if (isHovering) {
            prevTarget = lastHoveredEntity;
        } else if (isInChat) {
            // В чате показываем себя
            prevTarget = mc.player;
        } else {
            // Нет цели, не наведение, не чат - сбрасываем цель
            prevTarget = null;
        }

        // Анимация появления/скрытия (как в доноре: BACKWARDS = показывать)
        animation = AnimationMath.animation(animation, shown ? 1.0f : 0.0f, 0.4f);
        float anim = 1.0f - animation;
        anim = Math.max(0.0f, Math.min(1.0f, anim));
        if (anim < 0.01f) return;

        if (prevTarget == null) return;

        // Предметы цели
        List<ItemStack> items = new ArrayList<>();
        boolean showItems = true; // настройка из донора
        if (showItems) {
            items.add(prevTarget.getMainHandStack());
            items.add(prevTarget.getEquippedStack(EquipmentSlot.HEAD));
            items.add(prevTarget.getEquippedStack(EquipmentSlot.CHEST));
            items.add(prevTarget.getEquippedStack(EquipmentSlot.LEGS));
            items.add(prevTarget.getEquippedStack(EquipmentSlot.FEET));
            items.add(prevTarget.getOffHandStack());
            items.removeIf(ItemStack::isEmpty);
        }

        float totalItemsW = items.size() * ITEM_BOX + Math.max(0, items.size() - 1) * SPACING;
        float itemStartY = 0.0f;
        float mainBoxY = showItems && !items.isEmpty() ? ITEM_BOX + 2.0f : 0.0f;
        float panelH = mainBoxY + MAIN_HEIGHT + 6.0f;

        // Позиция (ванильная: центр снизу)
        float defX = Math.max(0.0f, (mc.getWindow().getFramebufferWidth() - WIDTH) / 2.0f);
        float defY = Math.max(0.0f, mc.getWindow().getFramebufferHeight() * 0.55f);

        // В FluxVisuals нет Drag системы, используем фиксированную позицию
        float x = defX;
        float y = defY;
        float hudScale = 1.0f;

        float width = WIDTH * hudScale;
        float mainHeight = MAIN_HEIGHT * hudScale;
        float headSize = HEAD_SIZE * hudScale;
        float boxY = y + mainBoxY * hudScale;
        float itemStartYScaled = y + itemStartY * hudScale;

        Renderer2D r2 = FluxVisualsClient.getRenderer();
        if (r2 == null) return;

        try {
            // Рендерим предметы
            if (showItems && !items.isEmpty()) {
                float itemStartX = x + (width / 2f) - (totalItemsW * hudScale / 2f);
                for (int i = 0; i < items.size(); i++) {
                    float ix = itemStartX + i * (ITEM_BOX + SPACING) * hudScale;
                    float itemBoxScaled = ITEM_BOX * hudScale;
                    drawStyle(r2, ix, itemStartYScaled, itemBoxScaled, itemBoxScaled, anim);
                    if (context != null) {
                        renderItem(context, items.get(i), ix + 2.0f * hudScale, itemStartYScaled + 2.0f * hudScale, itemBoxScaled - 4.0f * hudScale);
                    }
                }
            }

            // Основная панель
            drawStyle(r2, x, boxY, width, mainHeight, anim);

            // Голова цели
            float headX = x + 4.0f * hudScale;
            float headY = boxY + 4.0f * hudScale;
            drawHead(r2, context, prevTarget, headX, headY, headSize, anim);

            // Имя с скроллом (как в доноре)
            String name = prevTarget.getName().getString();
            float nameX = headX + headSize + 5.0f * hudScale;
            float nameW = width - (headSize + 15.0f * hudScale);
            float nameSize = 9.0f * hudScale; // УВЕЛИЧЕНО от 7.5 до 9.0 как в доноре

            // Текст метрика
            var metrics = r2.measureText(font, name, nameSize);
            float tWidth = metrics != null ? metrics.width : 0;

            // Скролл анимация (как в доноре)
            if (scrollDelay == 0L) scrollDelay = System.currentTimeMillis();
            if (System.currentTimeMillis() - scrollDelay > 1000L) {
                scrollDelay = System.currentTimeMillis();
                scrollDir = -scrollDir;
            }
            float targetOffset = scrollDir > 0 ? Math.max(0.0f, tWidth - nameW) : 0.0f;
            scrollAnim = AnimationMath.animation(scrollAnim, targetOffset, 0.05f);

            // Имя (без scissor - просто рисуем)
            r2.text(font, nameX - scrollAnim, boxY + 4.5f * hudScale, nameSize, name, Color.WHITE.getRGB());

            // HP-бар
            float barX = nameX;
            float barY = boxY + mainHeight - 8.5f * hudScale;
            float barW = nameW;
            float barH = 3.5f * hudScale;

            // Фон HP-бара
            r2.rect(barX, barY, barW, barH, 1.5f, new Color(0, 0, 0, (int) (60 * anim)).getRGB());

            // Анимация здоровья (как в доноре)
            float maxHp = prevTarget.getMaxHealth();
            healthAnim = AnimationMath.animation(healthAnim, prevTarget.getHealth(), 0.2f);
            absorptionAnim = AnimationMath.animation(absorptionAnim, prevTarget.getAbsorptionAmount(), 0.2f);

            float hpFactor = Math.max(0, Math.min(1, healthAnim / maxHp));

            // Цвета здоровья (как в доноре)
            Color c1 = new Color(0xFF, 0x44, 0x44); // красный
            Color c2 = new Color(0xCC, 0x33, 0x33);
            r2.rect(barX, barY, barW * hpFactor, barH, 1.5f, c1.getRGB());

            // Поглощение (absorption)
            float absWidth = 0;
            if (absorptionAnim > 0.1f) {
                float absFactor = Math.max(0, Math.min(1, absorptionAnim / maxHp));
                absWidth = barW * absFactor;
                Color gold1 = new Color(255, 215, 0, 200);
                r2.rect(barX + barW - absWidth, barY, absWidth, barH, 1.5f, gold1.getRGB());
            }

            // HP текст — используем РЕАЛЬНОЕ здоровье (как в доноре), не анимированное
            float currentHealth = prevTarget.getHealth();
            float currentAbsorption = prevTarget.getAbsorptionAmount();
            float hValue = (float) (Math.round(currentHealth * 10.0f) / 10.0f);
            float aValue = (float) (Math.round(currentAbsorption * 10.0f) / 10.0f);
            String hStr = hValue + "";
            String aStr = aValue > 0 ? " + " + aValue : "";
            String suffix = " HP";
            float hpTextSize = 8.0f * hudScale; // УВЕЛИЧЕНО от 6.5 до 8.0

            // Измеряем ширину текста через measureText
            var hMetrics = r2.measureText(font, hStr, hpTextSize);
            var aMetrics = r2.measureText(font, aStr, hpTextSize);
            var sMetrics = r2.measureText(font, suffix, hpTextSize);
            float hWidth = hMetrics != null ? hMetrics.width : 0;
            float aWidth = aMetrics != null ? aMetrics.width : 0;
            float sWidth = sMetrics != null ? sMetrics.width : 0;
            float fullTextW = hWidth + aWidth + sWidth;

            float drawX = barX + barW - fullTextW;
            float drawY = barY - 7.0f * hudScale;
            r2.text(font, drawX, drawY, hpTextSize, hStr, new Color(0xE0, 0xE0, 0xE0).getRGB());
            float off = hWidth;
            if (aValue > 0) {
                r2.text(font, drawX + off, drawY, hpTextSize, aStr, new Color(0xFF, 0xD7, 0x00).getRGB());
                off += aWidth;
            }
            r2.text(font, drawX + off, drawY, hpTextSize, suffix, new Color(255, 255, 255, (int) (150 * anim)).getRGB());

        } catch (Exception e) {
            // Ignore render errors
        }
    }

    /**
     * Получает сущность под прицелом (как TargetsUtility.getTarget в доноре)
     */
    private static LivingEntity getHoveredEntity() {
        HitResult hit = mc.crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            net.minecraft.util.hit.EntityHitResult entityHit = (net.minecraft.util.hit.EntityHitResult) hit;
            if (entityHit.getEntity() instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    /**
     * Рисует стиль панели (glass/blur)
     */
    private static void drawStyle(Renderer2D r2, float x, float y, float w, float h, float anim) {
        if (ru.fluxvisuals.module.impl.visuals.Hud.blur.get("HUD")) {
            r2.prepareBlur(23.0f);
            r2.blur(x, y, w, h, 4f, anim);
        }

        // Glass style
        int bgColor = Renderer2D.ColorUtil.replAlpha(0xFF141419, (int) (180 * anim));
        r2.rect(x, y, w, h, 4f, bgColor);

        // Outline
        int outAlpha = (int) (30 * anim);
        int outColor = Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, outAlpha);
        r2.rectOutline(x, y, w, h, 4f, outColor, 0.5f);
    }

    /**
     * Рисует голову цели (как в доноре)
     */
    private static void drawHead(Renderer2D r2, DrawContext context, LivingEntity target, float headX, float headY, float headSize, float anim) {
        // Для игроков и мобов используем дефолтную голову
        drawDefaultHead(r2, headX, headY, headSize);
    }

    /**
     * Дефолтная голова для мобов
     */
    private static void drawDefaultHead(Renderer2D r2, float headX, float headY, float headSize) {
        // Рисуем простой прямоугольник как placeholder для головы
        r2.rect(headX, headY, headSize, headSize, 2f, new Color(0x3E, 0x3E, 0x47, 255).getRGB());
        r2.rectOutline(headX, headY, headSize, headSize, 2f, new Color(0xFF, 0xFF, 0xFF, 80).getRGB(), 0.5f);
    }

    /**
     * Рендерит предмет (упрощенно)
     */
    private static void renderItem(DrawContext context, ItemStack stack, float x, float y, float size) {
        if (stack.isEmpty()) return;

        context.getMatrices().pushMatrix();
        try {
            context.getMatrices().translate(x, y);
            float scale = size / 16f;
            context.getMatrices().scale(scale, scale);
            context.drawItem(stack, 0, 0);
            context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
        } finally {
            context.getMatrices().popMatrix();
        }
    }
}