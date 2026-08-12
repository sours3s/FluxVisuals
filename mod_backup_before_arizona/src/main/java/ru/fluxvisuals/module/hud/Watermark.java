package ru.fluxvisuals.module.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import ru.fluxvisuals.config.ConfigManager;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.hud.HudLayout;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.RenderUtils;
import ru.fluxvisuals.render.Theme;

/** Логотип клиента: иконка + название в градиентной пилюле. */
public class Watermark extends Module {
    private static final Identifier LOGO_TEXTURE = Identifier.of("fluxvisuals", "textures/icon/logo.png");
    private static final float ICON_SIZE = 12f;

    private final Setting position = Setting.mode("Position", "Расположение", "Top-Left", "Top-Right");

    public Watermark() {
        super("Watermark", "Логотип FluxVisuals", Category.HUD);
        addSetting(position);
        setEnabled(true);
    }

    @Override
    public boolean shouldList() { return false; }

    @Override
    public void onRender2D(DrawContext g, float tickDelta) {
        String name = ConfigManager.INSTANCE.clientName;
        String version = " 1.21.11";
        float textW = RenderUtils.textWidth(name + version);
        float w = textW + ICON_SIZE + 14f; // icon + gap + text + padding
        float h = 16f;
        HudLayout.Anchor anchor = position.getModeIndex() == 0 ? HudLayout.Anchor.TOP_LEFT : HudLayout.Anchor.TOP_RIGHT;
        HudLayout.INSTANCE.push(g, anchor, w, h, p -> {
            float px = p[0], py = p[1];
            RenderUtils.drawShadow(g, px, py, w, h, 4f, 70);
            RenderUtils.drawRoundedRectGradient(g, px, py, w, h, 8f, Theme.accent(), Theme.accentSecond());
            RenderUtils.drawRoundedOutline(g, px, py, w, h, 8f, 1f, ColorUtils.withAlpha(Theme.text(), 30));

            // logo icon (centred vertically in the pill)
            float iconY = py + (h - ICON_SIZE) / 2f;
            try {
                RenderUtils.drawTexture(g, LOGO_TEXTURE, px + 5f, iconY, ICON_SIZE, ICON_SIZE);
            } catch (Exception ignored) { /* texture not loaded yet */ }

            // text after icon
            float textX = px + 5f + ICON_SIZE + 4f;
            float textY = py + (h - RenderUtils.textHeight()) / 2f;
            RenderUtils.textShadow(g, name, textX, textY, Theme.text());
            float nameEnd = textX + RenderUtils.textWidth(name);
            RenderUtils.text(g, version, nameEnd + 2f, textY,
                    ColorUtils.withAlpha(Theme.text(), 190));
        });
    }
}
