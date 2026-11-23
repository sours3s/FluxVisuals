package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import ru.fluxvisuals.ui.fluxgui.client.gui.render.IconRender;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.RoundedRenderer;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.ShadowRender;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.TextRender;
import ru.fluxvisuals.ui.fluxgui.client.gui.screen.ClickGui;
import ru.fluxvisuals.ui.fluxgui.compat.Gui;
import ru.fluxvisuals.ui.fluxgui.compat.Gui$Color;
import ru.fluxvisuals.ui.fluxgui.util.ClientProfile;
import ru.fluxvisuals.ui.fluxgui.util.render.FrameEspGlow;
import ru.fluxvisuals.ui.fluxgui.util.render.MirageGlassPipeline;
import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class Hud {
    public static final Hud INSTANCE = new Hud();
    static final float DESIGN_WIDTH = 1920.0F;
    static final float DESIGN_HEIGHT = 1080.0F;
    private static final float BACKDROP_BLUR_SIGMA = 16.0F;
    private static final float BLUR_DISK_PER_SIGMA = 2.462F;
    private static final float SHADOW_SIGMA = 16.0F;
    private static final int SHADOW_COLOR = -352321536;
    public static final int PANEL_COLOR = -872415232;
    public static final int TEXT_COLOR = -2434342;
    public static final int TEXT_ROW = -2829100;
    public static final int TEXT_UNIT = -1076176166;
    public static final int TEXT_BADGE = -2133140774;
    public static final int WHITE = -1;
    public static final int SUBTLE = 452984831;
    public static final float TITLE_SIZE = 16.274F;
    public static final float NAME_SIZE = 14.241F;
    public static final float ROW_SIZE = 13.224F;
    public static final float BADGE_SIZE = 12.206F;
    public static final float PANEL_RADIUS = 10.0F;
    public static final float SEPARATOR_RADIUS = 1.0F;
    public static final float BADGE_RADIUS = 3.0F;
    public static final float SEPARATOR_WIDTH = 2.0F;
    public static final float LIST_WIDTH = 193.0F;
    public static final float LIST_HEIGHT = 82.0F;
    public static final float LIST_TITLE_X = 9.5F;
    public static final float LIST_TITLE_BASELINE = 27.08F;
    public static final float LIST_ROW_BASELINE = 65.583F;
    public static final float LIST_ROW_STEP = 27.0F;
    public static final float LIST_BADGE_Y = 51.0F;
    public static final float LIST_BADGE_HEIGHT = 19.0F;
    public static final float LIST_BADGE_BASELINE = 65.076F;
    public static final float LIST_ITEM_X = 10.0F;
    public static final float LIST_ITEM_Y = 54.0F;
    public static final float LIST_ITEM_SIZE = 13.0F;
    private static final float TIMER_CENTER_X = 9.5F;
    private static final float TIMER_CENTER_Y = 9.5F;
    private static final float TIMER_INNER_RADIUS = 3.78F;
    private static final float TIMER_OUTER_RADIUS = 6.5F;
    private static final float TIMER_START = 270.0F;
    private static final float TIMER_FULL_SWEEP = 360.0F;
    private static final float VANILLA_ITEM_SIZE = 16.0F;
    private static final float EFFECT_SPRITE_SIZE = 18.0F;
    private static final HudElement[] ELEMENTS = new HudElement[]{
        Watermark.INSTANCE,
        Notifications.INSTANCE,
        Target.INSTANCE,
        Hotkeys.INSTANCE,
        Potions.INSTANCE,
        Cooldowns.INSTANCE,
        MediaPlayer.INSTANCE,
        Inventory.INSTANCE,
        ArmorHud.INSTANCE,
        CustomHotbar.INSTANCE
    };
    private static final Map<String, Float> ELEMENT_ANIMS = new HashMap<>();
    private static final Map<String, Float> DRAG_ANIMS = new HashMap<>();
    private static final Map<String, Float> HOVER_ANIMS = new HashMap<>();
    float scale = 1.0F;
    float originX;
    float originY;
    private float currentDragAnimation;
    private float currentElementOpacity = 1.0F;
    private String currentElementId;
    private boolean editing;

    public void render(DrawContext context) {
        FrameEspGlow.update();
        HudItemFilter.clearFrame();
        ru.fluxvisuals.ui.fluxgui.module.render.Animations.INSTANCE.updateAnimations();
        this.updateLayout(context);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(this.originX, this.originY);
        context.getMatrices().scale(this.scale, this.scale);
        for (HudElement element : ELEMENTS) {
            if (element != Notifications.INSTANCE) {
                this.renderElement(context, element, this.editing);
            }
        }

        this.currentDragAnimation = 0.0F;
        renderEditorGuides(context);
        renderEditorHint(context, this.editing);
        context.getMatrices().popMatrix();
    }

    public void renderNotifications(DrawContext context) {
        this.updateLayout(context);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(this.originX, this.originY);
        context.getMatrices().scale(this.scale, this.scale);
        this.renderElement(context, Notifications.INSTANCE, this.editing);
        this.currentDragAnimation = 0.0F;
        context.getMatrices().popMatrix();
    }

    private void updateLayout(DrawContext context) {
        this.scale = Math.min(context.getScaledWindowWidth() / 1920.0F, context.getScaledWindowHeight() / 1080.0F);
        this.originX = Math.round((context.getScaledWindowWidth() - 1920.0F * this.scale) / 2.0F);
        this.originY = Math.round((context.getScaledWindowHeight() - 1080.0F * this.scale) / 2.0F);
        HudEditor.install(this);
        this.editing = MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
    }

    private void renderElement(DrawContext context, HudElement element, boolean editing) {
        boolean enabled = elementEnabled(element);
        if (editing && !enabled) {
            ELEMENT_ANIMS.put(element.id(), 0.0F);
            DRAG_ANIMS.put(element.id(), 0.0F);
        } else if (!this.hiddenBehindClickGui(element)) {
            boolean visible = enabled && (editing || element.hasContent());
            float target = visible ? 1.0F : 0.0F;
            float current = ELEMENT_ANIMS.getOrDefault(element.id(), 0.0F);
            current = MathHelper.lerp(visible ? 0.055F : 0.11F, current, target);
            ELEMENT_ANIMS.put(element.id(), current);
            if (!(current < 0.035F)) {
                float ease = current * current * (3.0F - 2.0F * current);
                boolean fadeOnly = element == Notifications.INSTANCE && target <= 0.0F;
                float slideY = fadeOnly ? 0.0F : (1.0F - ease) * 14.0F;
                float editorScale = HudEditor.scale(element.id());
                float dragTarget = HudEditor.isDragging(element.id()) ? 1.0F : 0.0F;
                float dragAnimation = MathHelper.lerp(0.16F, DRAG_ANIMS.getOrDefault(element.id(), 0.0F), dragTarget);
                DRAG_ANIMS.put(element.id(), dragAnimation);
                this.currentDragAnimation = dragAnimation;
                float[] offset = HudEditor.offset(element.id());
                float automaticY = automaticYOffset(element);
                float elemX = element.x();
                float elemY = element.y();
                float elemW = Math.max(1.0F, element.width());
                float elemH = Math.max(1.0F, element.height());
                float centerX = elemX + elemW / 2.0F;
                float centerY = elemY + elemH / 2.0F;
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(offset[0], offset[1] + automaticY + slideY);
                if (element == CustomHotbar.INSTANCE) {
                    float fixedScale = 182.0F / Math.max(1.0F, elemW * this.scale);
                    context.getMatrices().translate(centerX, elemY + elemH);
                    context.getMatrices().scale(fixedScale, fixedScale);
                    context.getMatrices().translate(-centerX, -(elemY + elemH));
                }

                if (Math.abs(editorScale - 1.0F) > 1.0E-4F) {
                    context.getMatrices().translate(centerX, centerY);
                    context.getMatrices().scale(editorScale, editorScale);
                    context.getMatrices().translate(-centerX, -centerY);
                }

                this.currentElementOpacity = ease;
                this.currentElementId = element.id();
                boolean brightText = ru.fluxvisuals.ui.fluxgui.module.render.Hud.isLiquidGlassStyle();
                if (brightText) {
                    TextRender.beginBrightText();
                }

                try {
                    element.render(context, this);
                } finally {
                    if (brightText) {
                        TextRender.endBrightText();
                    }

                    this.currentElementOpacity = 1.0F;
                    this.currentElementId = null;
                }

                if (editing) {
                    String title = elementTitle(element);
                    float size = 11.0F;
                    float textWidth = TextRender.width(title, size);
                    float labelX = elemX;
                    float labelY = elemY - 15.0F;
                    if (labelY < 4.0F) {
                        labelY = elemY + elemH + 4.0F;
                    }
                    int labelBg = fade(0xC8000000, ease);
                    int labelText = fade(-1, ease);
                    RoundedRenderer.drawRoundedRect(context, labelX - 4.0F, labelY - 2.0F, textWidth + 8.0F, 15.0F, 4.0F, labelBg);
                    TextRender.draw(context, title, labelX, labelY + 8.0F, size, labelText);
                }

                context.getMatrices().popMatrix();
            }
        }
    }

    private boolean hiddenBehindClickGui(HudElement element) {
        float left;
        float top;
        float right;
        float bottom;
        if (element == CustomHotbar.INSTANCE) {
            float screenWidth = 182.0F;
            float screenHeight = element.height() * screenWidth / Math.max(1.0F, element.width());
            float centerX = this.originX + (element.x() + element.width() / 2.0F) * this.scale;
            bottom = this.originY + (element.y() + element.height()) * this.scale;
            left = centerX - screenWidth / 2.0F;
            right = centerX + screenWidth / 2.0F;
            top = bottom - screenHeight;
        } else {
            float[] bounds = HudEditor.boundsFor(element);
            left = this.originX + bounds[0] * this.scale;
            top = this.originY + bounds[1] * this.scale;
            right = left + bounds[2] * this.scale;
            bottom = top + bounds[3] * this.scale;
        }

        return ClickGui.overlapsMainPanel(left, top, right, bottom);
    }

    public static HudElement[] elements() {
        return ELEMENTS;
    }

    static float automaticYOffset(HudElement element) {
        return element == Watermark.INSTANCE ? Watermark.automaticYOffset() : 0.0F;
    }

    static boolean elementEnabled(HudElement element) {
        ru.fluxvisuals.client.FluxVisualsClient client = ru.fluxvisuals.client.FluxVisualsClient.get;
        ru.fluxvisuals.module.impl.visuals.Hud legacy = client == null || client.manager == null
              ? null
              : (ru.fluxvisuals.module.impl.visuals.Hud) client.manager.getModule(ru.fluxvisuals.module.impl.visuals.Hud.class);
        if (legacy == null || legacy.element == null) {
            return true;
        }
        String key;
        if (element == Watermark.INSTANCE) {
            key = "Watermark";
        } else if (element == Notifications.INSTANCE) {
            key = "Notifications";
        } else if (element == Target.INSTANCE) {
            key = "TargetHUD";
        } else if (element == Hotkeys.INSTANCE) {
            key = "Hotkeys";
        } else if (element == Potions.INSTANCE) {
            key = "Potion List";
        } else if (element == Cooldowns.INSTANCE) {
            key = "Cooldowns";
        } else if (element == Inventory.INSTANCE) {
            key = "Inventory HUD";
        } else if (element == MediaPlayer.INSTANCE) {
            key = "Media Player";
        } else if (element == ArmorHud.INSTANCE) {
            key = "Armor HUD";
        } else if (element == CustomHotbar.INSTANCE) {
            key = "Custom Hotbar";
        } else {
            return true;
        }
        return legacy.element.get(key);
    }

    public static String playerName() {
        return ClientProfile.getUsername();
    }

    public void panel(DrawContext context, float x, float y, float width, float height, float radius) {
        this.panel(context, x, y, width, height, radius, 1.0F);
    }

    public void panel(DrawContext context, float x, float y, float width, float height, float radius, float opacity) {
        opacity *= this.currentElementOpacity;
        HudElementStyleManager$Style style = this.currentStyle();
        String material = style.material(ru.fluxvisuals.ui.fluxgui.module.render.Hud.hudStyle.getValue());
        boolean liquidGlass = material.equalsIgnoreCase("Liquid Glass") || material.equalsIgnoreCase("Жидкое стекло");
        radius = style.radius(radius);
        boolean shadowEnabled = style.shadow(ru.fluxvisuals.ui.fluxgui.module.render.Hud.shadow.getValue());
        if (shadowEnabled) {
            float shadowSigma = MathHelper.lerp(this.currentDragAnimation, 16.0F, 20.0F) * style.shadowStrength(1.0F);
            int defaultShadow = liquidGlass ? 1979711488 : -352321536;
            int shadowColor = withAlpha(style.shadowColor(defaultShadow & 16777215), defaultShadow >>> 24 & 0xFF);
            ShadowRender.draw(context, x, y, width, height, radius, shadowSigma, fade(shadowColor, opacity));
        }

        boolean defaultGlow = liquidGlass && ru.fluxvisuals.ui.fluxgui.module.render.Hud.hudGlassGlow.getValue();
        if (style.glow(defaultGlow)) {
            int defaultGlowRgb = ru.fluxvisuals.ui.fluxgui.module.render.Hud.hudGlassColor.getValue() & 16777215;
            int glowRgb = style.glowColor(style.accentColor(defaultGlowRgb));
            float glowStrength = style.glowStrength(ru.fluxvisuals.ui.fluxgui.module.render.Hud.hudGlowStrength.getFloatValue());
            int glowAlpha = Math.round(14.0F + glowStrength * 22.0F);
            float glowSigma = 12.0F + glowStrength * 9.0F;
            ShadowRender.draw(context, x, y, width, height, radius, glowSigma, fade(withAlpha(glowRgb, glowAlpha), opacity));
        }

        if (liquidGlass) {
            Color selected = ru.fluxvisuals.ui.fluxgui.module.render.Hud.hudGlassColor.getColor();
            int glassRgb = style.panelColor(selected.getRGB() & 16777215);
            int glassAlpha = style.panelOpacity(selected.getAlpha());
            int materialAlpha = MathHelper.clamp(Math.round(12.0F + glassAlpha * 0.12F), 12, 42);
            Color tint = new Color(materialAlpha << 24 | glassRgb, true);
            if (style.blur(true)) {
                MirageGlassPipeline.drawLiquidAt(context, x, y, width, height, radius, 10.5F * style.blurStrength(1.0F), 2.0F, 0.6F, opacity, tint);
            }

            int tintOverlay = MathHelper.clamp(Math.round(7.0F + glassAlpha * 0.07F), 7, 25);
            int secondRgb = style.panelColor2() == null ? glassRgb : style.panelColor2();
            RoundedRenderer.drawFourCornerGradientRoundedRect(
                context,
                x,
                y,
                width,
                height,
                radius,
                fade(withAlpha(glassRgb, tintOverlay), opacity),
                fade(553648127, opacity),
                fade(withAlpha(secondRgb, Math.min(88, tintOverlay + 28)), opacity),
                fade(withAlpha(secondRgb, Math.min(76, tintOverlay + 20)), opacity)
            );
            RoundedRenderer.drawRoundedOutline(
                context,
                x + 1.0F,
                y + 1.0F,
                Math.max(0.0F, width - 2.0F),
                Math.max(0.0F, height - 2.0F),
                Math.max(1.0F, radius - 1.0F),
                0.65F,
                fade(989855743, opacity)
            );
        } else if (style.blur(ru.fluxvisuals.ui.fluxgui.module.render.Hud.blur.getValue())) {
            MirageGlassPipeline.drawBlurAt(context, x, y, width, height, radius, 39.392F * this.scale * style.blurStrength(1.0F), opacity);
            this.drawStyledPanel(context, style, x, y, width, height, radius, opacity);
        } else {
            this.drawStyledPanel(context, style, x, y, width, height, radius, opacity);
        }

        if (style.outline()) {
            RoundedRenderer.drawRoundedOutline(
                context, x, y, width, height, radius, style.outlineWidth(), fade(withAlpha(style.outlineColor(accent()), 190), opacity)
            );
        }

        if (style.stripe()) {
            this.drawStripe(context, style, x, y, width, height, radius, opacity);
        }

        if (!style.label().isEmpty()) {
            float labelSize = 9.5F;
            float labelWidth = TextRender.width(style.label(), labelSize);
            float labelInset = 7.0F;
            if (style.hasIcon() && !hasBuiltInPrimaryIcon(this.currentElementId)) {
                labelInset += IconRender.getDesignSize(style.icon("star")) * style.iconScale() + 4.0F;
            }

            TextRender.draw(
                context,
                style.label(),
                x + width - labelWidth - labelInset,
                y + 12.0F,
                labelSize,
                fade(withAlpha(style.labelColor(styledColor(-1076176166)), 220), opacity)
            );
        }

        if (style.hasIcon() && !hasBuiltInPrimaryIcon(this.currentElementId)) {
            String icon = style.icon("star");
            float iconSize = IconRender.getDesignSize(icon) * style.iconScale();
            IconRender.draw(context, icon, x + width - iconSize - 7.0F, y + 6.0F, iconSize, fade(accent(), opacity));
        }
    }

    private static boolean hasBuiltInPrimaryIcon(String id) {
        return id != null && (id.equals("watermark") || id.equals("notifications") || id.equals("hotkeys") || id.equals("potions") || id.equals("cooldowns"));
    }

    private void drawStyledPanel(
        DrawContext context, HudElementStyleManager$Style style, float x, float y, float width, float height, float radius, float opacity
    ) {
        int rgb = style.panelColor(0);
        int alpha = style.panelOpacity(204);
        Integer second = style.panelColor2();
        if (second == null) {
            RoundedRenderer.drawRoundedRect(context, x, y, width, height, radius, fade(withAlpha(rgb, alpha), opacity));
        } else {
            RoundedRenderer.drawFourCornerGradientRoundedRect(
                context,
                x,
                y,
                width,
                height,
                radius,
                fade(withAlpha(rgb, alpha), opacity),
                fade(withAlpha(second, alpha), opacity),
                fade(withAlpha(second, alpha), opacity),
                fade(withAlpha(rgb, alpha), opacity)
            );
        }
    }

    private void drawStripe(DrawContext context, HudElementStyleManager$Style style, float x, float y, float width, float height, float radius, float opacity) {
        float stripe = style.stripeWidth();
        int color = fade(withAlpha(style.stripeColor(accent()), 220), opacity);
        String var11 = style.stripeSide();
        switch (var11) {
            case "right":
                RoundedRenderer.drawRoundedRect(context, x + width - stripe, y, stripe, height, Math.min(radius, stripe / 2.0F), color);
                break;
            case "top":
                RoundedRenderer.drawRoundedRect(context, x, y, width, stripe, Math.min(radius, stripe / 2.0F), color);
                break;
            case "bottom":
                RoundedRenderer.drawRoundedRect(context, x, y + height - stripe, width, stripe, Math.min(radius, stripe / 2.0F), color);
                break;
            default:
                RoundedRenderer.drawRoundedRect(context, x, y, stripe, height, Math.min(radius, stripe / 2.0F), color);
        }
    }

    public boolean isEditing() {
        return this.editing;
    }

    public void listPanel(DrawContext context, float x, float y, float height, String title, String mark, float markX, float markY) {
        this.listPanel(context, x, y, 193.0F, height, title, mark, markX, markY);
    }

    public void listPanel(DrawContext context, float x, float y, float width, float height, String title, String mark, float markX, float markY) {
        this.panel(context, x, y, width, height, 10.0F);
        String icon = primaryIcon(mark);
        float iconSize = IconRender.getDesignSize(icon) * this.currentStyle().iconScale();
        IconRender.draw(context, icon, x + markX, y + markY, iconSize, animatedColor(accent()));
        TextRender.draw(context, title, x + 9.5F, y + 27.08F, 16.274F, animatedColor(-2434342));
    }

    public static void item(DrawContext context, ItemStack stack, float x, float y, float size) {
        if (!stack.isEmpty()) {
            float factor = size / 16.0F;
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(factor, factor);
            HudItemFilter.beginCapture();

            try {
                context.drawItem(stack, 0, 0);
            } finally {
                HudItemFilter.endCapture();
            }

            context.getMatrices().popMatrix();
        }
    }

    /** Иконки кулдаунов рисуются ванильным цветом, без фильтрации HUD-предметов. */
    public static void itemUnfiltered(DrawContext context, ItemStack stack, float x, float y, float size) {
        if (stack.isEmpty()) {
            return;
        }
        float factor = size / 16.0F;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(factor, factor);
        context.drawItem(stack, 0, 0);
        context.getMatrices().popMatrix();
    }

    public static void effect(DrawContext context, RegistryEntry<StatusEffect> effect, float x, float y) {
        effect(context, effect, x, y, 18.0F);
    }

    public static void effect(DrawContext context, RegistryEntry<StatusEffect> effect, float x, float y, float size) {
        Identifier id = effect.getKey().map(RegistryKey::getValue).orElse(null);
        if (id != null) {
            Identifier texture = Identifier.of(id.getNamespace(), "textures/mob_effect/" + id.getPath() + ".png");
            Gui.tex(context, texture)
                .vertex(x, y + size, 0.0F, 1.0F, animatedColor(-1))
                .vertex(x + size, y + size, 1.0F, 1.0F, animatedColor(-1))
                .vertex(x + size, y, 1.0F, 0.0F, animatedColor(-1))
                .vertex(x, y, 0.0F, 0.0F, animatedColor(-1))
                .submit(context);
        }
    }

    public static void timer(DrawContext context, float x, float y, float progress) {
        float sweep = 360.0F * MathHelper.clamp(progress, 0.0F, 1.0F);
        RoundedRenderer.drawArc(context, x + 9.5F, y + 9.5F, 3.78F, 6.5F, 270.0F, sweep, animatedColor(accent()));
    }

    public static void separator(DrawContext context, float x, float y, float height, int color) {
        int animated = animatedColor(color);
        Gui$Color batch = Gui.color(context);
        batch.quad(x, y, x + 2.0F, y, x + 2.0F, y + height, x, y + height, animated);
        batch.submit(context);
    }

    public static void badge(DrawContext context, String text, float x, float y, float width, float baseline) {
        RoundedRenderer.drawRoundedRect(context, x, y, width, 19.0F, 3.0F, animatedColor(452984831));
        TextRender.draw(context, text, x + (width - TextRender.width(text, 12.206F)) / 2.0F, y + baseline, 12.206F, animatedColor(-2133140774));
    }

    public static float listHeight(int count) {
        return 82.0F + Math.max(0, count - 1) * 27.0F;
    }

    public static String formatSeconds(float seconds) {
        int sec = Math.max(0, Math.round(seconds));
        return String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
    }

    public static int fade(int color, float opacity) {
        int a = Math.round((color >>> 24 & 0xFF) * opacity);
        return a << 24 | color & 16777215;
    }

    private static int withAlpha(int rgb, int alpha) {
        return MathHelper.clamp(alpha, 0, 255) << 24 | rgb & 16777215;
    }

    public static int animatedColor(int color) {
        return fade(styledColor(color), INSTANCE.currentElementOpacity);
    }

    public static int accent() {
        int fallback = ru.fluxvisuals.ui.fluxgui.module.render.Hud.accentColor.getValue();
        return 0xFF000000 | INSTANCE.currentStyle().accentColor(fallback & 16777215);
    }

    public static int styledColor(int color) {
        Integer styled = INSTANCE.currentStyle().textColor();
        int alpha = color >>> 24 & 0xFF;
        int resolved = styled == null ? color : withAlpha(styled, alpha);
        return liquidGlassReadableText(resolved);
    }

    private static int liquidGlassReadableText(int color) {
        HudElementStyleManager$Style style = INSTANCE.currentStyle();
        String material = style.material(ru.fluxvisuals.ui.fluxgui.module.render.Hud.hudStyle.getValue());
        if (!material.equalsIgnoreCase("Liquid Glass") && !material.equalsIgnoreCase("Жидкое стекло")) {
            return color;
        }

        int glass = style.panelColor(ru.fluxvisuals.ui.fluxgui.module.render.Hud.hudGlassColor.getValue());
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;
        int channelMin = Math.min(red, Math.min(green, blue));
        int channelMax = Math.max(red, Math.max(green, blue));
        if (channelMax - channelMin > 36) {
            return color;
        }

        int glassRed = glass >>> 16 & 0xFF;
        int glassGreen = glass >>> 8 & 0xFF;
        int glassBlue = glass & 0xFF;
        int redDelta = red - glassRed;
        int greenDelta = green - glassGreen;
        int blueDelta = blue - glassBlue;
        if (redDelta * redDelta + greenDelta * greenDelta + blueDelta * blueDelta >= 16384) {
            return color;
        }

        float grayMix = 0.42F;
        int grayRed = Math.round(MathHelper.lerp(grayMix, red, 174.0F));
        int grayGreen = Math.round(MathHelper.lerp(grayMix, green, 180.0F));
        int grayBlue = Math.round(MathHelper.lerp(grayMix, blue, 186.0F));
        return color >>> 24 << 24 | grayRed << 16 | grayGreen << 8 | grayBlue;
    }

    public static String primaryIcon(String fallback) {
        return INSTANCE.currentStyle().icon(fallback);
    }

    public static float primaryIconScale() {
        return INSTANCE.currentStyle().iconScale();
    }

    private HudElementStyleManager$Style currentStyle() {
        return HudElementStyleManager.style(this.currentElementId);
    }

    public static float animatedOpacity() {
        return INSTANCE.currentElementOpacity;
    }

    public static void timerBadge(DrawContext context, float x, float y, float width, float textX, float baselineY, String text, float progress) {
        RoundedRenderer.drawRoundedRect(context, x, y, width, 19.0F, 3.0F, animatedColor(452984831));
        TextRender.draw(context, text, textX, baselineY, 12.206F, animatedColor(-2133140774));
        timer(context, x + width - 18.0F, y, progress);
    }

    public static void resetAnimations() {
        ELEMENT_ANIMS.clear();
        DRAG_ANIMS.clear();
        HOVER_ANIMS.clear();
    }

    private static String elementTitle(HudElement element) {
        String id = element.id();
        if (element == Watermark.INSTANCE) {
            return "Watermark";
        } else if (element == Notifications.INSTANCE) {
            return "Notifications";
        } else if (element == Target.INSTANCE) {
            return "Target HUD";
        } else if (element == Hotkeys.INSTANCE) {
            return "Hotkeys";
        } else if (element == Potions.INSTANCE) {
            return "Potion Effects";
        } else if (element == Cooldowns.INSTANCE) {
            return "Cooldowns";
        } else if (element == MediaPlayer.INSTANCE) {
            return "Media Player";
        } else if (element == Inventory.INSTANCE) {
            return "Inventory HUD";
        } else if (element == ArmorHud.INSTANCE) {
            return "Armor HUD";
        } else if (element == CustomHotbar.INSTANCE) {
            return "Hotbar";
        }
        return id.isEmpty() ? "HUD" : id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1);
    }

    private static void renderEditorGuides(DrawContext context) {
        if (HudEditor.isDragging()) {
            int centerColor = 949656218;
            int snapColor = 1622192304;
            RoundedRenderer.drawRoundedRect(context, 959.5F, 0.0F, 1.0F, 1080.0F, 0.5F, centerColor);
            RoundedRenderer.drawRoundedRect(context, 0.0F, 539.5F, 1920.0F, 1.0F, 0.5F, centerColor);
            Float guideX = HudEditor.guideX();
            if (guideX != null && Math.abs(guideX - 960.0F) > 0.1F) {
                RoundedRenderer.drawRoundedRect(context, guideX - 0.5F, 0.0F, 1.0F, 1080.0F, 0.5F, snapColor);
            }

            Float guideY = HudEditor.guideY();
            if (guideY != null && Math.abs(guideY - 540.0F) > 0.1F) {
                RoundedRenderer.drawRoundedRect(context, 0.0F, guideY - 0.5F, 1920.0F, 1.0F, 0.5F, snapColor);
            }
        }
    }

    private static void renderEditorHint(DrawContext context, boolean editing) {
        HudElement hovered = editing && !HudEditor.isDragging() ? HudEditor.hovered() : null;

        for (HudElement element : ELEMENTS) {
            if (!elementEnabled(element)) {
                HOVER_ANIMS.put(element.id(), 0.0F);
            } else {
                float target = element == hovered ? 1.0F : 0.0F;
                float opacity = MathHelper.lerp(0.12F, HOVER_ANIMS.getOrDefault(element.id(), 0.0F), target);
                HOVER_ANIMS.put(element.id(), opacity);
                if (!(opacity < 0.01F)) {
                    float eased = opacity * opacity * (3.0F - 2.0F * opacity);
                    float[] bounds = HudEditor.boundsFor(element);
                    String first = "Ctrl для свободного перемещения";
                    String second = "Колёсико для смены размера";
                    float size = 11.5F;
                    float centerX = bounds[0] + bounds[2] / 2.0F;
                    float firstX = centerX - TextRender.width(first, size) / 2.0F;
                    float secondX = centerX - TextRender.width(second, size) / 2.0F;
                    float firstY = bounds[1] + bounds[3] + 15.0F - (1.0F - eased) * 4.0F;
                    int color = fade(-1717789540, eased);
                    TextRender.draw(context, first, firstX, firstY, size, color);
                    TextRender.draw(context, second, secondX, firstY + 13.0F, size, color);
                }
            }
        }
    }
}
