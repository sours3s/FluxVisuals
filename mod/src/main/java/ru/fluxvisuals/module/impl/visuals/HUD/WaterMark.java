package ru.fluxvisuals.module.impl.visuals.HUD;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Логотип в стиле исходного FluxVisuals: компактная градиентная пилюля 16 px. */
@Environment(EnvType.CLIENT)
public final class WaterMark {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Identifier LOGO_TEXTURE = Identifier.of("fluxvisuals", "textures/icon/logo.png");
    private static final String NAME = "FluxVisuals";
    private static final String VERSION = " 1.21.11";
    private static final int PADDING = 4;
    private static final int HEIGHT = 16;
    private static final int ICON = 12;

    private WaterMark() {
    }

    public static void waterMark(DrawContext context) {
        TextRenderer font = mc.textRenderer;
        float width = font.getWidth(NAME + VERSION) + 26.0F;
        int x = PADDING;
        int y = PADDING;
        int right = (int) (x + width);
        int bottom = y + HEIGHT;

        // Тень — как в исходнике: прямоугольник со сдвигом, без blur.
        context.fill(x + 1, y + 1, right + 1, bottom + 1, 0x88000000);

        // Вертикальный градиент в исходнике задаётся как bgcolor→accentSecond.
        context.fillGradient(x, y, right, bottom, HudTheme.ACCENT, HudTheme.ACCENT_SECOND);

        // Тонкая светлая обводка.
        int outline = HudTheme.withAlpha(HudTheme.TEXT, 30);
        context.fill(x, y, right, y + 1, outline);
        context.fill(x, bottom - 1, right, bottom, outline);
        context.fill(x, y, x + 1, bottom, outline);
        context.fill(right - 1, y, right, bottom, outline);

        context.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO_TEXTURE,
              x + 5, y + 2, 0.0F, 0.0F, ICON, ICON, 1024, 1024);

        int textX = x + 21;
        int textY = y + 3;
        context.drawTextWithShadow(font, NAME, textX, textY, HudTheme.TEXT);
        context.drawTextWithShadow(font, VERSION, textX + font.getWidth(NAME), textY,
              HudTheme.withAlpha(HudTheme.TEXT, 190));
    }
}
