package ru.fluxvisuals.hud;

import net.minecraft.client.gui.DrawContext;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.RenderUtils;
import ru.fluxvisuals.render.Theme;

/** Отрисовка типовых HUD-строк: фоновая панель + градиентная акцентная полоса + текст. */
public final class HudElements {
    public static final float ROW_HEIGHT = 12f;

    private HudElements() {}

    public static float rowWidth(String text) {
        return RenderUtils.textWidth(text) + 12f;
    }

    /** Рисует строку с фоном, акцентной полосой слева и текстом. */
    public static void drawRow(DrawContext g, String text, float x, float y, float width, int accent, boolean background) {
        if (background) {
            RenderUtils.drawRoundedRect(g, x, y, width, ROW_HEIGHT, 3f, Theme.bg());
        }
        RenderUtils.fillGradientV(g, x, y + 1f, 2f, ROW_HEIGHT - 2f, accent, Theme.accentSecond());
        RenderUtils.textShadow(g, text, x + 6f, y + 1.5f, Theme.text());
    }
}
