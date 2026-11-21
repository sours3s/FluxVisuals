package ru.fluxvisuals.ui.fluxgui.client.gui.font.icon;

import ru.fluxvisuals.ui.fluxgui.module.Category;
import ru.fluxvisuals.ui.fluxgui.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class IconTtfBridge {
    public static final int TTF_ICON_DISPLAY_SCALE = 3;
    static final float CATEGORY_ICON_SCALE = 0.95F;
    static final float CATEGORY_ICON_REF_BOX = 22.0F;
    static final float HUD_ICON_REF_HEIGHT = 22.0F;
    static final float COLOR_SWATCH_TTF_SCALE = 0.656F;
    static final float HEADER_TOOL_ICON_REF_BOX = 18.0F;
    static final float GENERIC_ICON_REF_BOX = 12.0F;

    static IconTtfBridge$CategoryIconSpec categoryIconSpec(Category cat) {
        return switch (IconTtfBridge$1.$SwitchMap$com$example$FluxVisuals$module$Category[cat.ordinal()]) {
            case 1 -> new IconTtfBridge$CategoryIconSpec('D', 23, 0.0F, 0.0F);
            case 2 -> new IconTtfBridge$CategoryIconSpec('\'', 21, 0.0F, 0.0F);
            case 3 -> new IconTtfBridge$CategoryIconSpec('d', 21, 0.0F, 0.1F);
            case 4 -> new IconTtfBridge$CategoryIconSpec('f', 21, 0.0F, 0.0F);
            case 5 -> new IconTtfBridge$CategoryIconSpec('_', 21, 0.0F, 0.0F);
            case 6 -> new IconTtfBridge$CategoryIconSpec('*', 21, 0.0F, 0.0F);
            case 7 -> new IconTtfBridge$CategoryIconSpec('@', 21, 0.0F, 0.0F);
            case 8 -> new IconTtfBridge$CategoryIconSpec('&', 21, 0.0F, 0.0F);
            case 9 -> new IconTtfBridge$CategoryIconSpec('f', 21, 0.0F, 0.0F);
            case 10 -> new IconTtfBridge$CategoryIconSpec('c', 21, 0.0F, 0.0F);
            case 11 -> new IconTtfBridge$CategoryIconSpec('j', 21, 0.0F, 0.0F);
            case 12 -> new IconTtfBridge$CategoryIconSpec('8', 21, 0.0F, 0.0F);
            case 13 -> new IconTtfBridge$CategoryIconSpec('f', 21, 0.0F, 0.0F);
            default -> throw new MatchException(null, null);
        };
    }

    IconTtfBridge() {
    }

    static int shaderColorToArgb() {
        float[] sc = RenderUtil.shaderColor();
        int r = Math.min(255, Math.max(0, Math.round(sc[0] * 255.0F)));
        int g = Math.min(255, Math.max(0, Math.round(sc[1] * 255.0F)));
        int b = Math.min(255, Math.max(0, Math.round(sc[2] * 255.0F)));
        int a = Math.min(255, Math.max(0, Math.round(sc[3] * 255.0F)));
        return a << 24 | r << 16 | g << 8 | b;
    }

    static int hudScaledSize(int helsingRefPx, float boxHeight) {
        int scaled = Math.round(helsingRefPx * boxHeight / 22.0F * 3.0F);
        return Math.max(10, Math.min(120, scaled));
    }

    public static boolean tryDrawHudHeader(DrawContext context, String texturePath, float x, float y, float w, float h) {
        TtfIconFonts.lazyInit();
        int argb = shaderColorToArgb();
        float cx = x + w / 2.0F;
        float cy = y + h / 2.0F;
        RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        return switch (texturePath) {
            case "textures/hotkeys.png" -> drawHudIconsGlyph(context, 'B', 23, cx, cy, argb, h);
            case "textures/potions.png" -> drawHudIconsGlyph(context, 'C', 17, cx, cy, argb, h);
            case "textures/cooldowns.png" -> drawHudIconsGlyph(context, 'D', 21, cx, cy, argb, h);
            case "textures/fps.png" -> drawHudIconsGlyph(context, 'h', 17, cx, cy, argb, h);
            case "textures/ping.png" -> drawHudIconsGlyph(context, 'P', 18, cx, cy, argb, h);
            case "textures/player.png" -> drawHudIconsGlyph(context, 'A', 21, cx, cy, argb, h);
            case "textures/server.png" -> drawHudIconsGlyph(context, 'S', 17, cx, cy, argb, h);
            case "textures/time.png" -> drawHudIconsGlyph(context, 'T', 17, cx, cy, argb, h);
            case "textures/bps.png" -> drawHudIconsGlyph(context, 'Y', 19, cx, cy, argb, h);
            case "textures/del.png" -> drawHudIconsGlyph(context, 'p', 17, cx, cy, argb, h);
            case "textures/download.png" -> drawHudIconsGlyph(context, 's', 17, cx, cy, argb, h);
            default -> false;
        };
    }

    public static boolean tryDrawHudHeader(DrawContext context, Identifier id, float x, float y, float w, float h) {
        return tryDrawHudHeader(context, id.getPath(), x, y, w, h);
    }

    static boolean drawHudIconsGlyph(DrawContext context, char ch, int helsingRefPx, double cx, double cy, int argb, float boxH) {
        if (!TtfIconFonts.hasIcons()) {
            return false;
        }

        int sz = hudScaledSize(helsingRefPx, boxH);
        return drawCenteredChar(context, TtfIconFonts.icons(sz), ch, cx, cy, argb);
    }

    public static boolean tryDrawCategoryIcon(DrawContext context, Category cat, float boxX, float boxY, float boxSize, int argb) {
        TtfIconFonts.lazyInit();
        IconTtfBridge$CategoryIconSpec spec = categoryIconSpec(cat);
        float boxScale = Math.max(0.2F, Math.min(4.0F, boxSize / 22.0F));
        int logicalSize = Math.min(120, Math.max(8, Math.round(spec.fontSize() * 3 * 0.95F * boxScale)));
        HelsingStyleTtfFont font;
        if (cat != Category.PLAYER) {
            if (!TtfIconFonts.hasIcons()) {
                return false;
            }

            font = TtfIconFonts.icons(logicalSize);
        } else {
            if (!TtfIconFonts.hasCategory()) {
                return false;
            }

            font = TtfIconFonts.category(logicalSize);
        }

        if (font == null) {
            return false;
        }

        float boxScalePx = boxSize / 22.0F;
        float cx = boxX + boxSize / 2.0F + spec.offsetX() * boxScalePx;
        float cy = boxY + boxSize / 2.0F + spec.offsetY() * boxScalePx;
        RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        font.drawVerticallyCenteredString(context, String.valueOf(spec.ch()), cx, cy, argb);
        return true;
    }

    public static boolean tryDrawHeaderToolIcon(DrawContext context, int toolIndex, float iconX, float iconY, float iconBoxSize, int argb) {
        TtfIconFonts.lazyInit();
        if (!TtfIconFonts.hasIcons()) {
            return false;
        }

        char ch = (char)(switch (toolIndex) {
            case 0 -> 101;
            case 1 -> 43;
            case 2 -> 98;
            default -> 102;
        });

        float refMul = switch (toolIndex) {
            case 1 -> 0.74F;
            case 3 -> 0.84F;
            default -> 0.7F;
        };
        float boxScale = Math.max(0.25F, Math.min(4.0F, iconBoxSize / 18.0F));
        int logicalSize = Math.min(120, Math.max(8, Math.round(51.0F * refMul * boxScale)));
        float cx = iconX + iconBoxSize / 2.0F;
        float f = iconY + iconBoxSize / 2.0F;

        float cy = f + switch (toolIndex) {
            case 2, 3 -> 0.35F;
            default -> 0.0F;
        };
        RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return drawCenteredChar(context, TtfIconFonts.icons(logicalSize), ch, cx, cy, argb);
    }

    static boolean drawCenteredChar(DrawContext context, HelsingStyleTtfFont font, char ch, double cx, double cy, int argb) {
        return font == null ? false : font.drawVerticallyCenteredChar(context, ch, cx, cy, argb);
    }

    public static boolean tryDrawIconsFontChar(DrawContext context, char ch, float iconX, float iconY, float iconBoxSize, int argb) {
        TtfIconFonts.lazyInit();
        if (!TtfIconFonts.hasIcons()) {
            return false;
        }

        float boxScale = Math.max(0.25F, Math.min(4.0F, iconBoxSize / 12.0F));
        int logicalSize = Math.min(120, Math.max(8, Math.round(42.0F * boxScale * 0.7F)));
        HelsingStyleTtfFont font = TtfIconFonts.icons(logicalSize);
        if (font == null) {
            return false;
        }

        float cx = iconX + iconBoxSize / 2.0F;
        float cy = iconY + iconBoxSize / 2.0F;
        RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return drawCenteredChar(context, font, ch, cx, cy, argb);
    }

    public static boolean tryDrawColorPickerSwatch(DrawContext context, double centerX, double centerY, float boxSizePx, int argb) {
        TtfIconFonts.lazyInit();
        int sz = Math.max(10, Math.min(120, Math.round(18.0F * boxSizePx / 22.0F * 3.0F * 0.656F)));
        RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        HelsingStyleTtfFont font;
        if (TtfIconFonts.hasIcons() && (font = TtfIconFonts.icons(sz)) != null && drawCenteredChar(context, font, 'u', centerX, centerY, argb)) {
            return true;
        } else if (TtfIconFonts.hasGui() && (font = TtfIconFonts.gui(sz)) != null) {
            font.drawVerticallyCenteredString(context, "u", centerX, centerY, argb);
            return true;
        } else {
            return false;
        }
    }

    public static int colorSwatchLayoutCellPx() {
        float refBox = 14.0F;
        int sz = Math.max(10, Math.min(120, Math.round(18.0F * refBox / 22.0F * 3.0F * 0.656F)));
        if (!TtfIconFonts.hasIcons()) {
            return Math.max(9, (int)Math.ceil(refBox * 0.656F));
        }

        HelsingStyleTtfFont font = TtfIconFonts.icons(sz);
        if (font == null) {
            return Math.max(9, (int)Math.ceil(refBox * 0.656F));
        }

        int w = Math.round(font.getStringWidth("u"));
        return Math.max(8, Math.min(16, w + 2));
    }
}
